package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.youtube_lecture_helper.dto.QuizStatisticsDto;
import com.example.youtube_lecture_helper.dto.UserRankingDto;
import com.example.youtube_lecture_helper.dto.UserStatisticsDto;
import com.example.youtube_lecture_helper.service.StatisticsService;
import java.util.List;


@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/statistics")
public class StatisticsController{
    private final StatisticsService statisticsService;

    @GetMapping("/quiz")
    public ResponseEntity<QuizStatisticsDto> getQuizStatistics(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(required = true) Long quizId
    ) {
        QuizStatisticsDto statistics = statisticsService.getQuizStatistics(quizId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/user")
    public ResponseEntity<UserStatisticsDto> getUserStatistics(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        UserStatisticsDto statistics = statisticsService.getUserStatistics(userId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/ranking/high")
    public ResponseEntity<List<UserRankingDto>> getTopPerformingUsers(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<UserRankingDto> ranking = statisticsService.getTopPerformingUsers(10);
        return ResponseEntity.ok(ranking);
    }
}