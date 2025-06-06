package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.QuizSet;
import com.example.youtube_lecture_helper.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizSetRepository extends JpaRepository<QuizSet,Long> {
    @Query("SELECT qs " +
            "FROM QuizSet qs " +
            "LEFT JOIN FETCH qs.attempts a " +
            "LEFT JOIN FETCH a.quiz " +
            "WHERE qs.id = :quizSetId")
    Optional<QuizSet> findByIdWithAttempts(@Param("quizSetId") Long quizSetId);

    @Query("""
    SELECT DISTINCT qa.quizSet
    FROM UserVideoCategory uvc
    JOIN uvc.video v
    JOIN Quiz q ON q.youtubeId = v.youtubeId
    JOIN QuizAttempt qa ON qa.quiz = q
    WHERE uvc.category.id = :categoryId
    AND qa.quizSet.user = uvc.category.user
    """)
    List<QuizSet> findValidQuizSetsByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
        SELECT DISTINCT qs
        FROM QuizSet qs
        JOIN qs.attempts qa
        WHERE qa.quiz.youtubeId = :youtubeId
          AND qs.user= :user
    """)
    List<QuizSet> findAllByVideoAndUser(
            @Param("youtubeId") String youtubeId,
            @Param("user") User user
    );
    List<QuizSet> findByUserId(Long userId);
    List<QuizSet> findByIsMultiVideo(Boolean isMultiVideo);
    @Query("""
        select qs 
        from QuizSet qs 
        join QuizSetMulti qsm on qs = qsm.quizSet
        where qsm.quiz.id = :quizId
    """)
    List<QuizSet> findAllByQuizId(@Param("quizId") Long quizId);
    
    boolean existsByIdAndUserId(Long id, Long userId);

    // 특정 유저가 제작한 멀티 퀴즈 세트 가져오기
    @Query("SELECT DISTINCT qs FROM QuizSet qs " +
       "JOIN QuizSetMulti qsm ON qsm.quizSet.id = qs.id " +
       "WHERE qs.user.id = :userId")
    List<QuizSet> findMultiQuizSetsByUserId(@Param("userId") Long userId);
}
