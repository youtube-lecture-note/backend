package com.example.youtube_lecture_helper.repository;

import java.time.LocalDateTime;

public interface QuizAttemptProjection {
    Long getQuizSetId();
    LocalDateTime getAttemptTime();
    Long getTotalAttempts();   // 해당 QuizSet의 전체 문항 수
    Long getIncorrectAttempts(); // 해당 QuizSet의 오답 수
}
