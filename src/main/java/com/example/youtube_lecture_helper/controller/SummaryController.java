package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.security.CustomUserDetails;
import com.example.youtube_lecture_helper.service.CreateSummaryAndQuizService;
import com.example.youtube_lecture_helper.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SummaryController {
    //        //SzrVaYDLiHs : 빅뱅
//        //yHzJikY3LBo : 수소폭탄
//        //nJ-R9G1pepU : 사물궁이
//        //Bh6WtpsStpM : 중학교 2학년 과학 (17분)
//        //veTpPfu1-o8 : 뱀(58분)
//        //vLaFAKnaRJU : 영어강의
    private final CreateSummaryAndQuizService createSummaryAndQuizService;
    private final VideoService videoService;

    // @GetMapping(value = "/api/summary", produces = "application/json")
    // //ApiResponse<String>
    // public ResponseEntity<ApiResponse<String>> getSummary(@RequestParam String videoId) {
    //     SummaryResult summaryResult = videoService.getSummary(videoId);
    //     if(summaryResult.getStatus()== SummaryStatus.NO_SUBTITLE){
    //         return ApiResponse.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,"자막 없음","");
    //     }
    //     if(summaryResult.getStatus()== SummaryStatus.NOT_LECTURE){
    //         return ApiResponse.buildResponse(HttpStatus.BAD_REQUEST, "강의 영상 아님", "");
    //     }
    //     System.out.println("summary: " + summaryResult.getSummary());
    //     return ApiResponse.buildResponse(HttpStatus.OK, "성공", summaryResult.getSummary());
    // }

    @GetMapping(value="/api/summary")
    public Mono<ResponseEntity<ApiResponse<String>>> processVideo(
            @RequestParam String videoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Received async request to process video: {}", videoId);
        Long userId = null;
        try{
            userId = ((CustomUserDetails) userDetails).getId();
        }catch(NullPointerException e){
            log.error("UserDetails is null or does not contain user ID", e);
            return Mono.just(ApiResponse.<String>buildResponse(
                    HttpStatus.UNAUTHORIZED, "사용자 인증 정보가 없습니다.", "로그인이 필요합니다"));
        }

        // 서비스 호출하여 요약 결과 Mono 받기
        return createSummaryAndQuizService.initiateVideoProcessing(userId, videoId, "ko")
                .flatMap(summaryResult -> {
                    // SummaryResult 상태에 따라 처리
                    return switch (summaryResult.getStatus()) {
                        case SUCCESS -> {
                            log.info("Successfully generated summary for videoId: {}. Responding OK.", videoId);
                            yield Mono.just(ApiResponse.<String>buildResponse(
                                    HttpStatus.OK, "성공", summaryResult.getSummary()));
                        }
                        case NO_SUBTITLE -> {
                            log.warn("Summary generation failed for videoId: {}. Reason: No Subtitles.", videoId);
                            yield Mono.just(ApiResponse.<String>buildResponse(
                                    HttpStatus.INTERNAL_SERVER_ERROR, "자막 없음", null));
                        }
                        case NOT_LECTURE -> {
                            log.warn("Summary generation failed for videoId: {}. Reason: Not a lecture video.", videoId);
                            yield Mono.just(ApiResponse.<String>buildResponse(
                                    HttpStatus.BAD_REQUEST, "강의 영상 아님", null));
                        }
                        case BANNED -> {
                            log.warn("Summary generation failed for videoId: {}. Reason: BANNED video: ", videoId);
                            yield Mono.just(ApiResponse.<String>buildResponse(
                                    HttpStatus.BAD_REQUEST, "저작권 처리 요청 들어온 영상", null));
                        }
                        case FAILED, PROCESSING -> {
                            log.warn("Summary generation failed or is still processing for videoId: {}. Status: {}",
                                    videoId, summaryResult.getStatus());
                            String errorMessage = (summaryResult.getSummary() != null && !summaryResult.getSummary().isBlank())
                                    ? summaryResult.getSummary()
                                    : "요약 생성 실패 또는 진행 중";
                            yield Mono.just(ApiResponse.<String>buildResponse(
                                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, null));
                        }
                    };
                })
                .onErrorResume(e -> {
                    log.error("Unhandled exception during reactive processing initiation for videoId: {}", videoId, e);
                    String errorMessage = "서버 내부 오류 발생: " + e.getMessage();
                    return Mono.just(ApiResponse.<String>buildResponse(
                            HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, null));
                });
    }
    @GetMapping(value="/api/summary/{id}")
    public ResponseEntity<ApiResponse<String>> getSummaryById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        return ApiResponse.<String>buildResponse(HttpStatus.OK, "성공", videoService.getVideoSummaryById(id));
    }

//    @GetMapping(value="/api/summary/test")
//    public Mono<ResponseEntity<ApiResponse<String>>> processVideoTest(
//            @RequestParam String videoId) {
//        log.info("Received async request to process video: {}", videoId);
//        Long userId = 1L;
//
//        // 서비스 호출하여 요약 결과 Mono 받기
//        return createSummaryAndQuizService.initiateVideoProcessing(videoId, "ko")
//                .flatMap(summaryResult -> {
//                    // SummaryResult 상태에 따라 처리
//                    return switch (summaryResult.getStatus()) {
//                        case SUCCESS -> {
//                            log.info("Successfully generated summary for videoId: {}. Responding OK.", videoId);
//                            yield Mono.just(ApiResponse.<String>buildResponse(
//                                    HttpStatus.OK, "성공", summaryResult.getSummary()));
//                        }
//                        case NO_SUBTITLE -> {
//                            log.warn("Summary generation failed for videoId: {}. Reason: No Subtitles.", videoId);
//                            yield Mono.just(ApiResponse.<String>buildResponse(
//                                    HttpStatus.INTERNAL_SERVER_ERROR, "자막 없음", null));
//                        }
//                        case NOT_LECTURE -> {
//                            log.warn("Summary generation failed for videoId: {}. Reason: Not a lecture video.", videoId);
//                            yield Mono.just(ApiResponse.<String>buildResponse(
//                                    HttpStatus.BAD_REQUEST, "강의 영상 아님", null));
//                        }
//                        case FAILED, PROCESSING -> {
//                            log.warn("Summary generation failed or is still processing for videoId: {}. Status: {}",
//                                    videoId, summaryResult.getStatus());
//                            String errorMessage = (summaryResult.getSummary() != null && !summaryResult.getSummary().isBlank())
//                                    ? summaryResult.getSummary()
//                                    : "요약 생성 실패 또는 진행 중";
//                            yield Mono.just(ApiResponse.<String>buildResponse(
//                                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, null));
//                        }
//                    };
//                })
//                .onErrorResume(e -> {
//                    log.error("Unhandled exception during reactive processing initiation for videoId: {}", videoId, e);
//                    String errorMessage = "서버 내부 오류 발생: " + e.getMessage();
//                    return Mono.just(ApiResponse.<String>buildResponse(
//                            HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, null));
//                });
//    }

}
