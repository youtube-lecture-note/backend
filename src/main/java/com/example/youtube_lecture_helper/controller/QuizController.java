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
public class QuizController {

    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;
    private final CreateSummaryAndQuizService createSummaryAndQuizService;

    //난이도별 퀴즈 개수 반환
    @GetMapping("/api/quizzes/count")
    public ResponseEntity<QuizCountDto> getQuizCount(
            @RequestParam String videoId, @AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(quizService.getQuizCountByYoutubeId(videoId));
    }
    //퀴즈 생성 (단일 난이도)
    @GetMapping("/api/quizzes")
    public ResponseEntity<ApiResponse<QuizService.CreatedQuizSetDTO>> getQuizzesWithDifficultyAndCount(
            @RequestParam String videoId,
            @RequestParam int difficulty,
            @RequestParam int count,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        Long userId = ((CustomUserDetails) userDetails).getId();
        
        if(createSummaryAndQuizService.isQuizProcessing(videoId)) {
            return ApiResponse.buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "퀴즈를 생성 중입니다.",
                    null
            );
        }
        QuizService.CreatedQuizSetDTO quizzes = quizService.createQuizSetForUser(
                userId,difficulty,videoId,count,false
        );
        return ApiResponse.buildResponse(HttpStatus.OK,"success",quizzes);
    }

    //퀴즈 생성(여러 난이도, 여러 유저용)
    @GetMapping("/api/quizzes/multi-create")
    public ResponseEntity<ApiResponse<QuizService.CreatedQuizSetDTO>> createQuizSetByCounts(
            @RequestParam String videoId,
            @RequestParam int level1Count,
            @RequestParam int level2Count,
            @RequestParam int level3Count,
            @RequestParam String quizSetName,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if(createSummaryAndQuizService.isQuizProcessing(videoId)) {
            return ApiResponse.buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "퀴즈를 생성 중입니다.",
                    null
            );
        }
        Long userId = ((CustomUserDetails) userDetails).getId();
        QuizService.CreatedQuizSetDTO quizzes = quizService.createQuizSetForUserByCounts(
                userId, videoId, level1Count, level2Count, level3Count, true, quizSetName
        );
        return ApiResponse.buildResponse(HttpStatus.OK, "success", quizzes);
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
    public ResponseEntity<List<QuizHistorySummaryDto>> getQuizAttempts(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        return ResponseEntity.ok(quizAttemptService.getQuizHistorySummaries(userId));
    }

    //youtubeId, videoId로 해당 유저가 해당 비디오에 푼 기록들 조회
    @GetMapping("/api/quizzes/attempts/videos/{youtubeId}")
    public ResponseEntity<List<QuizHistorySummaryDto>> getQuizAttemptsByVideoId(
            @PathVariable String youtubeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        return ResponseEntity.ok(quizAttemptService.getVideoQuizHistorySummaries(userId, youtubeId));
    }
    //한명이 quizSet 생성 요청하면 QuizSetMulti 생성
    @GetMapping("/api/quizzes/multi/create")
    public ResponseEntity<?> createQuizSetMulti(
            @RequestParam String videoId,
            @RequestParam Integer difficulty,
            @RequestParam Integer count,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        Long userId = ((CustomUserDetails)userDetails).getId();
        if(createSummaryAndQuizService.isQuizProcessing(videoId)) {
            return ApiResponse.buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "퀴즈를 생성 중입니다.",
                    null
            );
        }
        return ResponseEntity.ok(quizService.createQuizSetForUser(userId,difficulty,videoId,count,true));
    }

    //redisKey로 만든 quizSet 가져오기
    @GetMapping("/api/quizzes/multi")
    public ResponseEntity<?> getMultiQuizzes(
            @RequestParam String redisKey,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        Long userId = ((CustomUserDetails)userDetails).getId();
        return ResponseEntity.ok(quizService.getQuizSetQuizzesByRedisQuizSetKey(userId,redisKey));  //내부적으로는 quizAttempt에 시도 생성
    }

    //QuizSetMulti 생성자가 해당 Quiz를 푼 사람들과 각 정답 개수 조회
    @GetMapping("/api/quizzes/multi/{quizSetId}/results")
    public ResponseEntity<?> getQuizSetMultiResults(
            @PathVariable Long quizSetId,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        Long userId = ((CustomUserDetails)userDetails).getId();
        if (!quizService.isQuizSetCreator(quizSetId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("해당 퀴즈셋의 결과를 조회할 권한이 없습니다.");
        }   
        return ResponseEntity.ok(quizService.getQuizSetResultsMulti(quizSetId));
    }
}
