package com.example.youtube_lecture_helper.exception;

public class QuizSetNotFoundException extends RuntimeException{
    public QuizSetNotFoundException(String message) {
        super(message);
    }
}