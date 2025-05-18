package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.QuizSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizSetRepository extends JpaRepository<QuizSet,Long> {
    @Query("SELECT qs " +
            "FROM QuizSet qs " +
            "LEFT JOIN FETCH qs.attempts a " +
            "LEFT JOIN FETCH a.quiz " +
            "WHERE qs.id = :quizSetId")
    Optional<QuizSet> findByIdWithAttempts(@Param("quizSetId") Long quizSetId);
}
