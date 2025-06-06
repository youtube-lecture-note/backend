package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.dto.QuizAttemptDto;
import com.example.youtube_lecture_helper.dto.QuizHistorySummaryDto;
import com.example.youtube_lecture_helper.dto.QuizAttemptWithAnswerDto;
import com.example.youtube_lecture_helper.dto.ParticipantResultDto;
import com.example.youtube_lecture_helper.dto.QuizStatisticsDto;
import com.example.youtube_lecture_helper.entity.QuizAttempt;
import com.example.youtube_lecture_helper.entity.QuizSet;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAttemptRepository  extends JpaRepository<QuizAttempt,Long> {
    @Query("SELECT " +
        "  q.youtubeId AS youtubeId, " +
        "  COALESCE(uvc.userVideoName, q.youtubeId) AS userVideoName, " +
        "  v.id AS videoId, " +
        "  qs.attemptTime AS date, " +
        "  qs.id AS quizSetId, " +
        "  COUNT(qa.id) AS totalQuizzes, " +
        "  SUM(CASE WHEN qa.isCorrect = false THEN 1 ELSE 0 END) AS wrongCount " +
        "FROM QuizAttempt qa " +
        "JOIN qa.quizSet qs " +
        "JOIN qa.quiz q " +
        "JOIN Video v ON q.youtubeId = v.youtubeId " +
        "LEFT JOIN UserVideoCategory uvc ON uvc.video.id = v.id AND uvc.user.id = :userId " +
        "WHERE qa.user.id = :userId " +
        "GROUP BY q.youtubeId, uvc.userVideoName, v.id, qs.attemptTime, qs.id " +
        "ORDER BY qs.attemptTime DESC")
    List<QuizHistorySummaryDto> findQuizSetSummariesByUserId(@Param("userId") Long userId);



    /**
     * 특정 비디오(youtubeId)에 대한 사용자의 퀴즈 풀이 기록 요약을 조회 (최근 순)
     * userId와 youtubeId를 사용하는 경우 (비디오 선택 후)
     * QuizSet 내에 해당 youtubeId를 가진 Quiz가 하나라도 포함된 경우 해당 QuizSet의 전체 통계를 보여줍니다.
     */
    @Query("""
        SELECT
          qs.id AS quizSetId,
          qs.attemptTime AS date,
          COUNT(qa.id) AS totalQuizzes,
          SUM(CASE WHEN qa.isCorrect = false THEN 1 ELSE 0 END) AS wrongCount 
        FROM QuizAttempt qa
        JOIN qa.quizSet qs
        JOIN qa.quiz q
        WHERE qs.user.id = :userId
          AND q.youtubeId = :youtubeId
        GROUP BY qs.id, qs.attemptTime
        ORDER BY qs.attemptTime DESC
    """)
    List<QuizHistorySummaryDto> findQuizSetSummariesByUserIdAndYoutubeId(
            @Param("userId") Long userId,
            @Param("youtubeId") String youtubeId);


    /**
     * 특정 QuizSet에 포함된 모든 QuizAttempt 상세 정보 조회 (N+1 방지)
     * 사용자가 목록에서 특정 기록(QuizSet)을 선택했을 때 호출
     */
    @Query("SELECT new com.example.youtube_lecture_helper.dto.QuizAttemptDto(" + // DTO의 정규화된(Full) 클래스 이름 사용
            "  qa.id, " +                 // attemptId
            "  qa.userAnswer, " +         // userAnswer
            "  qa.isCorrect, " +          // isCorrect
            "  q.id, " +                 // quizId
            "  q.question, " +           // questionT (Quiz 필드)
            "  q.youtubeId, " +          // youtubeId (Quiz 필드, Quiz 엔티티에 존재)
            "  qa.quizSet.id " +         // quizSetId
            ") " +
            "FROM QuizAttempt qa " +
            "JOIN qa.quiz q " +           // 일반 JOIN으로 변경 (FETCH 불필요)
            "WHERE qa.quizSet.id = :quizSetId " +
            "ORDER BY qa.id ASC")
    List<QuizAttemptDto> findDetailedAttemptDTOsByQuizSetId(@Param("quizSetId") Long quizSetId);
    @Query("SELECT new com.example.youtube_lecture_helper.dto.QuizAttemptWithAnswerDto(" +
            "  qa.id, " +                 // attemptId
            "  qa.userAnswer, " +         // userAnswer
            "  qa.isCorrect, " +          // isCorrect
            "  q.id, " +                  // quizId
            "  q.question, " +            // question
            "  q.youtubeId, " +           // youtubeId
            "  qa.quizSet.id, " +         // quizSetId
            "  q.option1, " +             // 추가 필드
            "  q.option2, " +
            "  q.option3, " +
            "  q.option4, " +
            "  q.correctAnswer, " +
            "  q.comment, " +
            "  q.selective, " +
            "  q.difficulty, " +
            "  q.timestamp " +
            ") " +
            "FROM QuizAttempt qa " +
            "JOIN qa.quiz q " +
            "WHERE qa.quizSet.id = :quizSetId " +
            "ORDER BY qa.id ASC")
    List<QuizAttemptWithAnswerDto> findDetailedAttemptsWithAnswersByQuizSetId(@Param("quizSetId") Long quizSetId);

    List<QuizAttempt> findByQuizSetId(Long quizSetId);
    List<QuizAttempt> findByUserId(Long userId);
    boolean existsByQuizSetIdAndUserId(Long quizSetId, Long userId);
    @Query("""
        select case when count(qa) > 0 then true else false end
        from QuizAttempt qa
        join qa.quiz q
        where qa.user.id = :userId
        and q.youtubeId = :youtubeId
        """)
    boolean existsByUserIdAndQuizYoutubeId(@Param("userId") Long userId, @Param("youtubeId") String youtubeId);
    
    void deleteByQuizSetId(Long quizSetId);
    void deleteByQuizSet(QuizSet quizSet);

    @Modifying
    @Query("DELETE FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND qa.quizSet.id = :quizSetId")
    void deleteByQuizIdAndQuizSetId(@Param("quizId") Long quizId, @Param("quizSetId") Long quizSetId);

    @Modifying
    @Query("DELETE FROM QuizAttempt qa WHERE qa.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);


    @Query("""
        select case when count(qa) > 0 then true else false end
        from QuizAttempt qa
        where qa.user.id = :userId
        and qa.quizSet.id = :quizSetId
        """)
    boolean existsByUserIdAndQuizSetId(@Param("userId") Long userId, @Param("quizSetId") Long quizSetId);

    @Query("SELECT new com.example.youtube_lecture_helper.dto.ParticipantResultDto(" +
       "u.name, u.email, " +
       "SUM(CASE WHEN qa.isCorrect = true THEN 1L ELSE 0L END)) " +
       "FROM QuizAttempt qa " +
       "JOIN qa.user u " +
       "WHERE qa.quizSet.id = :quizSetId " +
       "GROUP BY u.id, u.name, u.email " +
       "ORDER BY u.name ASC")
    List<ParticipantResultDto> findParticipantResultsByQuizSetId(@Param("quizSetId") Long quizSetId);


    @Query("SELECT new com.example.youtube_lecture_helper.dto.QuizStatisticsDto(" +
       "qa.quiz.id, " +
       "COUNT(qa), " +
       "SUM(CASE WHEN qa.isCorrect = true THEN 1L ELSE 0L END), " +
       "ROUND((SUM(CASE WHEN qa.isCorrect = true THEN 1.0 ELSE 0.0 END) * 100.0 / COUNT(qa)), 2)) " +
       "FROM QuizAttempt qa " +
       "WHERE qa.quizSet.id = :quizSetId " +
       "GROUP BY qa.quiz.id " +
       "ORDER BY qa.quiz.id")
    List<QuizStatisticsDto> findQuizStatisticsByQuizSetId(@Param("quizSetId") Long quizSetId);
}
