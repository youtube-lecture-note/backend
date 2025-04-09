package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.openai_api.Quiz;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository {
    List<Quiz> findByVideoId(String videoId);
    List<Quiz> save(String videoId, List<Quiz> quizzes);
    List<Quiz> findAll();
}
