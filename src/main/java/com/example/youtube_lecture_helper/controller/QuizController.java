package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.dto.QuizAttemptDto;
import com.example.youtube_lecture_helper.dto.QuizAttemptWithAnswerDto;
import com.example.youtube_lecture_helper.dto.UserQuizAnswerDto;
import com.example.youtube_lecture_helper.repository.QuizAttemptProjection;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;

    @GetMapping("/api/quizzes")
    public ResponseEntity<ApiResponse<QuizService.CreatedQuizSetDTO>> getQuizzesWithDifficultyAndCount(
            @RequestParam String videoId,
            @RequestParam int difficulty,
            @RequestParam int count,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        Long userId = ((CustomUserDetails) userDetails).getId();
        QuizService.CreatedQuizSetDTO quizzes = quizService.createQuizSetForUser(
                userId,difficulty,videoId,count
        );
        return ApiResponse.buildResponse(HttpStatus.OK,"success",quizzes);
    }

    @PostMapping("/api/quizzes/submit")
    public Mono<ResponseEntity<ApiResponse<List<QuizAttemptDto>>>> submitQuizAnswers(
            @RequestParam Long quizSetId,
            @RequestBody List<UserQuizAnswerDto> answers,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return quizAttemptService.processQuizAnswers(quizSetId, answers)
                .map(result -> ApiResponse.buildResponse(
                        HttpStatus.OK, 
                        "퀴즈 채점이 완료되었습니다.", 
                        result))
                .onErrorResume(e -> {
                    if (e instanceof QuizNotFoundException) {
                        return Mono.just(ApiResponse.buildResponse(
                                HttpStatus.NOT_FOUND,
                                "퀴즈를 찾을 수 없습니다: " + e.getMessage(),
                                null));
                    } else {
                        return Mono.just(ApiResponse.buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "처리 중 오류가 발생했습니다: " + e.getMessage(),
                                null));
                    }
                });
    }
    //해당 QuizSet에 대한 모든 quiz 정보(내 답, 정답, 설명 포함)
    @GetMapping("/api/quizzes/attempts/sets/{quizSetId}")
    public ResponseEntity<List<QuizAttemptWithAnswerDto>> getQuizAttempts(
            @PathVariable Long quizSetId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        return ResponseEntity.ok(quizAttemptService.getQuizAttemptDetails(quizSetId,userId));
    }
    
    //userId로 해당 유저의 전체 퀴즈 기록 조회
    @GetMapping("/api/quizzes/attempts/summaries")
    public ResponseEntity<List<QuizAttemptProjection>> getQuizAttempts(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        return ResponseEntity.ok(quizAttemptService.getQuizHistorySummaries(userId));
    }

    //youtubeId, videoId로 해당 유저가 해당 비디오에 푼 기록들 조회
    @GetMapping("/api/quizzes/attempts/videos/{youtubeId}")
    public ResponseEntity<List<QuizAttemptProjection>> getQuizAttemptsByVideoId(
            @PathVariable String youtubeId,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        Long userId = ((CustomUserDetails) userDetails).getId();
        return ResponseEntity.ok(quizAttemptService.getVideoQuizHistorySummaries(userId,youtubeId));
    }
}
