package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.SummaryStatus;
import com.example.youtube_lecture_helper.dto.VideoSummaryResponseDto;
import com.example.youtube_lecture_helper.openai_api.SummaryResult;
import com.example.youtube_lecture_helper.service.CreateSummaryAndQuizService;
import com.example.youtube_lecture_helper.service.VideoService;
import com.example.youtube_lecture_helper.dto.VideoProcessingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    //private final SummaryService summaryService;
    private final VideoService videoService;
    private final CreateSummaryAndQuizService createSummaryAndQuizService;

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
    public Mono<ResponseEntity<ApiResponse<String>>> processVideo(@RequestParam String videoId) {

        log.info("Received async request to process video: {}", videoId);

        // 서비스 호출하여 요약 결과 Mono 받기
        return createSummaryAndQuizService.initiateVideoProcessing(videoId, "ko")
                .map(summaryResult -> {
                    // SummaryResult 상태에 따라 ResponseEntity<ApiResponse<String>> 생성
                    return switch (summaryResult.getStatus()) {
                        case SUCCESS -> {
                            log.info("Successfully generated summary for videoId: {}. Responding OK.", videoId);
                            // 성공 시 ApiResponse 생성 (static helper 사용)
                            yield ApiResponse.<String>buildResponse(HttpStatus.OK, "성공", summaryResult.getSummary());
                        }
                        case NO_SUBTITLE -> {
                            log.warn("Summary generation failed for videoId: {}. Reason: No Subtitles.", videoId);
                            // 자막 없음 에러 처리
                            yield ApiResponse.<String>buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "자막 없음", null); // 에러 시 data는 null
                        }
                        case NOT_LECTURE -> {
                            log.warn("Summary generation failed for videoId: {}. Reason: Not a lecture video.", videoId);
                            // 강의 영상 아님 에러 처리
                            yield ApiResponse.<String>buildResponse(HttpStatus.BAD_REQUEST, "강의 영상 아님", null); // 에러 시 data는 null
                        }
                        case FAILED, PROCESSING -> { // 기타 실패 또는 처리 중 상태
                            log.warn("Summary generation failed or is still processing for videoId: {}. Status: {}", videoId, summaryResult.getStatus());
                            // SummaryResult에 에러 메시지가 있으면 사용, 없으면 기본 메시지 사용
                            String errorMessage = (summaryResult.getSummary() != null && !summaryResult.getSummary().isBlank())
                                    ? summaryResult.getSummary() // 서비스에서 실패 시 여기에 메시지를 넣어준다고 가정
                                    : "요약 생성 실패 또는 진행 중";
                            yield ApiResponse.<String>buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, null);
                        }
                    };
                })
                .onErrorResume(e -> {
                    // 리액티브 스트림 처리 중 예상치 못한 예외 발생 시 처리
                    log.error("Unhandled exception during reactive processing initiation for videoId: {}", videoId, e);
                    String errorMessage = "서버 내부 오류 발생: " + e.getMessage();

                    // --- 중요: 제네릭 타입 추론 오류 해결 ---
                    // buildResponse 호출 시 명시적으로 타입 파라미터 <String>을 지정하여
                    // null 데이터가 있어도 T가 String으로 추론되도록 함
                    Mono<ResponseEntity<ApiResponse<String>>> errorResponse = Mono.just(
                            ApiResponse.<String>buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, null)
                    );
                    return errorResponse;
                });
        // Optional: 초기 응답 생성에 대한 타임아웃 설정
        // .timeout(Duration.ofSeconds(30), Mono.defer(() -> {
        //     log.error("Timeout waiting for initial summary response for videoId: {}", videoId);
        //     // 타임아웃 발생 시에도 ApiResponse 형태로 반환
        //     return Mono.just(ApiResponse.<String>buildResponse(HttpStatus.GATEWAY_TIMEOUT, "요청 처리 시간 초과", null));
        // }));
    }

    // --- ApiResponse 클래스 ---
    // 별도 파일에 있거나 여기에 내부 클래스로 정의되어 있다고 가정
    // 예시:
    /*
    @Getter
    public static class ApiResponse<T> {
        private int status;
        private String message;
        private T data;

        public ApiResponse(int status, String message, T data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static <T> ResponseEntity<ApiResponse<T>> buildResponse(HttpStatus httpStatus, String message, T data) {
            ApiResponse<T> body = new ApiResponse<>(httpStatus.value(), message, data);
            return ResponseEntity.status(httpStatus).body(body);
        }
    }
    */
}
