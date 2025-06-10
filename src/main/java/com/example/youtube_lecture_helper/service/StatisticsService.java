package com.example.youtube_lecture_helper.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;

import com.example.youtube_lecture_helper.dto.QuizStatisticsDto;
import com.example.youtube_lecture_helper.dto.UserStatisticsDto;
import com.example.youtube_lecture_helper.dto.UserRankingDto;
import com.example.youtube_lecture_helper.repository.UserVideoCategoryRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {
    //quiz_statistics 테이블은 quiz_id cascade on delete
    //user_statistics 테이블은 user_id cascade on delete
    private final UserVideoCategoryRepository userVideoCategoryRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeStatistics() {
        log.info("Initializing statistics on server startup");
        updateAllStatistics();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 600000) // 10분마다
    public void updateAllStatistics() {
        updateQuizStatistics();
        updateUserStatistics();
    }
    
    public void updateQuizStatistics() {
        String sql = """
            INSERT INTO quiz_statistics (quiz_id, total_attempts, correct_attempts, accuracy_rate)
            SELECT 
                quiz_id,
                COUNT(*) as total_count,
                SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as correct_count,
                ROUND(
                    (SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)), 
                    2
                ) as accuracy
            FROM quiz_attempt 
            GROUP BY quiz_id
            ON DUPLICATE KEY UPDATE
                total_attempts = VALUES(total_attempts),
                correct_attempts = VALUES(correct_attempts),
                accuracy_rate = VALUES(accuracy_rate),
                last_updated = NOW()
            """;
        
        int updatedRows = jdbcTemplate.update(sql);
    }

    private void updateUserStatistics() {
    String sql = """
        INSERT INTO user_statistics (user_id, total_attempts, correct_attempts, accuracy_rate, studied_video_count)
        SELECT 
            qa.user_id,
            COUNT(*) as total_count,
            SUM(CASE WHEN qa.is_correct = 1 THEN 1 ELSE 0 END) as correct_count,
            ROUND(
                (SUM(CASE WHEN qa.is_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)), 
                2
            ) as accuracy,
            COALESCE(uvc.video_count, 0) as video_count
        FROM quiz_attempt qa
        LEFT JOIN (
            SELECT 
                user_id,
                COUNT(DISTINCT video_id) as video_count
            FROM user_video_category 
            WHERE video_id IS NOT NULL
            GROUP BY user_id
        ) uvc ON qa.user_id = uvc.user_id
        WHERE qa.user_id IS NOT NULL 
        GROUP BY qa.user_id
        ON DUPLICATE KEY UPDATE
            total_attempts = VALUES(total_attempts),
            correct_attempts = VALUES(correct_attempts),
            accuracy_rate = VALUES(accuracy_rate),
            studied_video_count = VALUES(studied_video_count),
            last_updated = NOW()
        """;
        
        int updatedUsers = jdbcTemplate.update(sql);
        log.info("Updated statistics for {} users", updatedUsers);
    }


    public QuizStatisticsDto getQuizStatistics(Long quizId) {
        String sql = """
            SELECT 
                q.id,
                q.question,
                q.difficulty,
                COALESCE(qs.total_attempts, 0) as total_attempts,
                COALESCE(qs.correct_attempts, 0) as correct_attempts,
                COALESCE(qs.accuracy_rate, 0) as accuracy_rate
            FROM quiz q
            LEFT JOIN quiz_statistics qs ON q.id = qs.quiz_id
            WHERE q.id = ?
            """;
        
        List<QuizStatisticsDto> results = jdbcTemplate.query(sql, 
            BeanPropertyRowMapper.newInstance(QuizStatisticsDto.class), quizId);
        return results.isEmpty() ? null : results.get(0);
    }

    
    // 유저 정답률 조회
    public UserStatisticsDto getUserStatistics(Long userId) {
        // CTE를 사용하여 전체 사용자의 순위를 먼저 계산한 후, 특정 사용자를 필터링합니다.
        // 이렇게 해야 PERCENT_RANK()가 전체 데이터를 기준으로 올바르게 계산됩니다.
        String sql = """
            WITH ranked_statistics AS (
                SELECT
                    us.user_id,
                    u.name,
                    us.total_attempts,
                    us.correct_attempts,
                    us.accuracy_rate,
                    us.studied_video_count,
                    (1 - PERCENT_RANK() OVER (ORDER BY us.accuracy_rate ASC)) * 100 as accuracy_percentile_rank,
                    (1 - PERCENT_RANK() OVER (ORDER BY us.studied_video_count ASC)) * 100 as videos_percentile_rank
                FROM user_statistics us
                JOIN user u ON us.user_id = u.id
            )
            SELECT
                name,
                total_attempts,
                correct_attempts,
                accuracy_rate,
                studied_video_count,
                accuracy_percentile_rank,
                videos_percentile_rank
            FROM ranked_statistics
            WHERE user_id = ?
            """;

        // BeanPropertyRowMapper가 DB의 snake_case 컬럼명(e.g., total_attempts)을
        // DTO의 camelCase 필드명(e.g., totalAttempts)으로 자동 매핑해줍니다.
        List<UserStatisticsDto> results = jdbcTemplate.query(
            sql,
            BeanPropertyRowMapper.newInstance(UserStatisticsDto.class),
            userId
        );

        // 결과가 비어있으면 null을, 그렇지 않으면 첫 번째 결과를 반환합니다.
        return results.isEmpty() ? null : results.get(0);
    }


    //정답률 상위 유저 랭킹 조회
    public List<UserRankingDto> getTopPerformingUsers(int limit) {
        String sql = """
            SELECT 
                u.username,
                us.total_attempts,
                us.accuracy_rate,
                RANK() OVER (ORDER BY us.accuracy_rate DESC) as ranking
            FROM user u
            INNER JOIN user_statistics us ON u.id = us.user_id
            WHERE us.total_attempts >= 20
            ORDER BY us.accuracy_rate DESC
            LIMIT ?
            """;
        
        return jdbcTemplate.query(sql, new RowMapper<UserRankingDto>() {
            @Override
            public UserRankingDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new UserRankingDto(
                    rs.getString("username"),
                    rs.getInt("total_attempts"),
                    rs.getBigDecimal("accuracy_rate"),
                    rs.getInt("ranking")
                );
            }
        }, limit);
    }
}
