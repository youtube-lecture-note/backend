package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.dto.QuizAttemptDto;
import com.example.youtube_lecture_helper.dto.QuizHistorySummaryDto;
import com.example.youtube_lecture_helper.dto.QuizAttemptWithAnswerDto;
import com.example.youtube_lecture_helper.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAttemptRepository  extends JpaRepository<QuizAttempt,Long> {
    @Query("SELECT " +
            "  v.youtubeId AS youtubeId, " +
            "  COALESCE(uvc.userVideoName, v.youtubeId) AS userVideoName, " +
            "  v.id AS videoId, " +
            "  qs.attemptTime AS date, " +
            "  qs.id AS quizSetId, " +
            "  COUNT(qa.id) AS totalQuizzes, " +
            "  SUM(CASE WHEN qa.isCorrect = false THEN 1 ELSE 0 END) AS wrongCount " +
            "FROM QuizAttempt qa " +
            "JOIN qa.quizSet qs " +
            "JOIN qa.quiz q " +
            "JOIN Video v ON q.youtubeId = v.youtubeId " +
            "LEFT JOIN UserVideoCategory uvc ON uvc.video.id = v.id AND uvc.user.id = qs.user.id " +
            "WHERE qs.user.id = :userId " +
            "GROUP BY v.youtubeId, uvc.userVideoName, v.id, qs.attemptTime, qs.id " +
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

}
