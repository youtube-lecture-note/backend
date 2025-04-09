package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.openai_api.Quiz;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class DummyQuizRepository implements QuizRepository{
    private final Map<String, List<Quiz>> quizMap = new HashMap<>();

    @Override
    public List<Quiz> findByVideoId(String videoId) {
        return quizMap.get(videoId);
    }

    @Override
    public List<Quiz> save(String videoId, List<Quiz> quizzes) {
        quizMap.put(videoId, quizzes);
        return quizzes;
    }

    @Override
    public List<Quiz> findAll() {
        return quizMap.values().stream()
                .flatMap(List::stream)
                .toList();
    }
}
