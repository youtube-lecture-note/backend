package com.example.youtube_lecture_helper.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.youtube_lecture_helper.controller.ApiResponse;
import com.example.youtube_lecture_helper.openai_api.ReactiveGptClient.OpenAiServerException;

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
    @ExceptionHandler(OpenAiServerException.class)
    public ResponseEntity<ApiResponse<Void>> handleOpenAiServerException(OpenAiServerException e) {
        e.printStackTrace(); // 로그에 에러 출력
        // OpenAI 서버 오류 메시지 파싱
        String userFriendlyMessage = "AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
        
        // 500 에러의 경우 SERVICE_UNAVAILABLE로 처리
        if (e.getMessage().contains("500") || e.getMessage().contains("INTERNAL_SERVER_ERROR")) {
            return ApiResponse.buildResponse(HttpStatus.SERVICE_UNAVAILABLE, userFriendlyMessage, null);
        }
        
        // 기타 OpenAI 에러의 경우
        return ApiResponse.buildResponse(HttpStatus.BAD_GATEWAY, userFriendlyMessage, null);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        e.printStackTrace(); // 로그에 에러 출력
        return ApiResponse.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", null);
    }
}