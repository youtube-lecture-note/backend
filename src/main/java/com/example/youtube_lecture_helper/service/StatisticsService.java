package com.example.youtube_lecture_helper.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.example.youtube_lecture_helper.dto.QuizStatisticsDto;
import com.example.youtube_lecture_helper.dto.UserStatisticsDto;
import com.example.youtube_lecture_helper.dto.UserRankingDto;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

@Slf4j
@Service
public class StatisticsService {
    //quiz_statistics 테이블은 quiz_id cascade on delete
    //user_statistics 테이블은 user_id cascade on delete
    
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
            INSERT INTO user_statistics (user_id, total_attempts, correct_attempts, accuracy_rate)
            SELECT 
                user_id,
                COUNT(*) as total_count,
                SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as correct_count,
                ROUND(
                    (SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)), 
                    2
                ) as accuracy
            FROM quiz_attempt 
            GROUP BY user_id
            ON DUPLICATE KEY UPDATE
                total_attempts = VALUES(total_attempts),
                correct_attempts = VALUES(correct_attempts),
                accuracy_rate = VALUES(accuracy_rate),
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
        String sql = """
            SELECT 
                u.name,
                COALESCE(us.total_attempts, 0) as total_attempts,
                COALESCE(us.correct_attempts, 0) as correct_attempts,
                COALESCE(us.accuracy_rate, 0) as accuracy_rate
            FROM `user` u
            LEFT JOIN `user_statistics` us ON u.id = us.user_id
            WHERE u.id = ?
            """;
        
        List<UserStatisticsDto> results = jdbcTemplate.query(sql, 
        BeanPropertyRowMapper.newInstance(UserStatisticsDto.class), userId);
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
