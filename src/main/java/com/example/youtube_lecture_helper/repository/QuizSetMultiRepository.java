package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.QuizSetMulti;
import com.example.youtube_lecture_helper.entity.Quiz;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizSetMultiRepository extends JpaRepository<QuizSetMulti,Long> {
    List<QuizSetMulti> findByQuizSetId(Long quizSetId);
    boolean existsByQuizSetId(Long quizSetId);
    void deleteByQuizSetId(Long quizSetId);

    @Modifying
    @Query("DELETE FROM QuizSetMulti qsm WHERE qsm.quiz.id = :quizId AND qsm.quizSet.id = :quizSetId")
    void deleteByQuizIdAndQuizSetId(@Param("quizId") Long quizId, @Param("quizSetId") Long quizSetId);

    int countByQuizSetId(Long quizSetId);

    @Query("SELECT COUNT(qsm) FROM QuizSetMulti qsm WHERE qsm.quizSet.id = :quizSetId")
    Long countQuizzesByQuizSetId(@Param("quizSetId") Long quizSetId);

    //quizSetId로 퀴즈 가져오기
    @Query("SELECT qsm.quiz FROM QuizSetMulti qsm WHERE qsm.quizSet.id = :quizSetId")
    List<Quiz> findQuizzesByQuizSetId(@Param("quizSetId") Long quizSetId);
}
