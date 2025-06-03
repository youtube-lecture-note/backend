package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.QuizSetMulti;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSetMultiRepository extends JpaRepository<QuizSetMulti,Long> {
    List<QuizSetMulti> findByQuizSetId(Long quizSetId);
    boolean existsByQuizSetId(Long quizSetId);
}
