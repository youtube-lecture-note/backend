package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.dto.*;
import com.example.youtube_lecture_helper.dto.QuizHistorySummaryDto;
import com.example.youtube_lecture_helper.security.CustomUserDetails;
import com.example.youtube_lecture_helper.service.QuizAttemptService;
import com.example.youtube_lecture_helper.service.QuizService;
import com.example.youtube_lecture_helper.exception.QuizNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import com.example.youtube_lecture_helper.service.CreateSummaryAndQuizService;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quizsets")
public class QuizSetController {
    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;

    //제작한 멀티 퀴즈셋 보기
    @GetMapping("/multi")
    public ResponseEntity<List<QuizSetSummaryDto>> getMultiQuizSets(    
            @AuthenticationPrincipal UserDetails userDetails) 
    {
        Long userId = ((CustomUserDetails) userDetails).getId();
        List<QuizSetSummaryDto> multiQuizSets = quizService.getMultiQuizSetsByUser(userId);
        return ResponseEntity.ok(multiQuizSets);
    }

    //퀴즈셋에 포함된 퀴즈 가져오기
    @GetMapping("/{quizSetId}/quizzes")
    public ResponseEntity<List<QuizWithAnswerDto>> getAllQuizzesInSet(
            @PathVariable Long quizSetId,
            @AuthenticationPrincipal UserDetails userDetails) 
    {
        Long userId = ((CustomUserDetails) userDetails).getId();
        List<QuizWithAnswerDto> quizzes = quizService.getAllQuizzesInSet(quizSetId, userId);
        return ResponseEntity.ok(quizzes);
    }
}