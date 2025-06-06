package com.example.youtube_lecture_helper.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.youtube_lecture_helper.controller.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(EntityNotFoundException e) {
        return ApiResponse.buildResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiResponse.buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }
    
    @ExceptionHandler(QuizAlreadyAttemptedException.class)
    public ResponseEntity<ApiResponse<Void>> handleQuizAlreadyAttemptedException(QuizAlreadyAttemptedException e) {
        return ApiResponse.buildResponse(HttpStatus.CONFLICT, e.getMessage(), null);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        return ApiResponse.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", null);
    }
}