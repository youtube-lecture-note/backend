package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.QuizSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizSetRepository extends JpaRepository<QuizSet,Long> {
}
