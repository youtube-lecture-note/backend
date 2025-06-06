package com.example.youtube_lecture_helper.exception;
public class QuizAlreadyAttemptedException extends RuntimeException {
    public QuizAlreadyAttemptedException(String message) {
        super(message);
    }
}