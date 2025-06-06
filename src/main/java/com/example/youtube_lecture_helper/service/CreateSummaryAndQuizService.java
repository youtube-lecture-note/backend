package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.SummaryStatus;
import com.example.youtube_lecture_helper.entity.Video;
import com.example.youtube_lecture_helper.openai_api.*;
import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.entity.Ban;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.repository.VideoRepository;
import com.example.youtube_lecture_helper.repository.BanRepository;
import com.example.youtube_lecture_helper.event.VideoProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;



import java.time.Duration;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import reactor.core.scheduler.Schedulers;
import org.springframework.context.ApplicationEventPublisher;
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateSummaryAndQuizService {
    private final ReactiveGptClient reactiveGptClient;
    private final YoutubeSubtitleExtractor youtubeSubtitleExtractor;
    private final VideoRepository videoRepository;
    private final QuizRepository quizRepository;
    private final BanRepository banRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, Mono<SummaryResult>> processingCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Mono<Void>> quizProcessingCache = new ConcurrentHashMap<>();
    
    //video 밴 리스트에 없음 + videoRepo에 이미 존재하는지 검색 후 호출
    public Mono<SummaryResult> initiateVideoProcessing(Long userId, String videoId, String language) {
        log.info("Request received to process videoId: {}", videoId);

        // Step 0: Ban 체크
        return checkIfBanned(videoId)
            .flatMap(banOpt -> {
                if (banOpt.isPresent()) {
                    log.warn("VideoId {} is banned. Skipping summary.", videoId);
                    // 금지된 영상이면 바로 결과 반환 (원하는 메시지/상태로)
                    return Mono.just(new SummaryResult(SummaryStatus.BANNED, "This video is banned."));
                }
                // Step 1: Summary DB 체크
                return checkDatabaseForExistingSummary(videoId)
                    .switchIfEmpty(Mono.defer(() -> {
                        // Step 2: 캐시/요약 생성
                        log.debug("No summary in DB for videoId: {}. Checking active processing cache.", videoId);
                        return processingCache.computeIfAbsent(videoId, key -> {
                            log.info("No active processing found for videoId: {}. Starting summary generation.", key);
                            return generateSummaryAndTriggerQuizProcessing(userId, key, language)
                                    .doFinally(signal -> {
                                        log.info("Summary processing signal {} for videoId: {}. Removing from active cache.", signal, key);
                                        processingCache.remove(key);
                                    })
                                    .cache();
                        });
                    }));
            });
    }

    /**
     * Checks the database ONLY for an existing video summary.
     *
     * @param videoId YouTube Video ID
     * @return Mono emitting SummaryResult if found, otherwise Mono.empty().
     */
    private Mono<SummaryResult> checkDatabaseForExistingSummary(String videoId) {
        return Mono.fromCallable(() -> {
                    log.debug("Checking database for summary of videoId: {}", videoId);
                    // Blocking call
                    return videoRepository.findByYoutubeId(videoId)
                            .filter(video -> video.getSummary() != null && !video.getSummary().isBlank()) // Check if summary exists and is not blank
                            .map(video -> {
                                log.info("Found existing summary in DB for videoId: {}", videoId);
                                return new SummaryResult(SummaryStatus.SUCCESS, video.getSummary());
                            }); // Returns Optional<SummaryResult>
                })
                .flatMap(Mono::justOrEmpty) // Convert Optional<SummaryResult> to Mono<SummaryResult> or Mono.empty()
                .subscribeOn(Schedulers.boundedElastic()); // Schedule blocking call
    }


    /**
     * Generates the summary, saves it to DB, returns the result,
     * and then triggers quiz generation in the background.
     */
    private Mono<SummaryResult> generateSummaryAndTriggerQuizProcessing(Long userId, String videoId, String language) {
        log.debug("Executing summary generation pipeline for videoId: {}", videoId);
        return reactiveGptClient.getVideoSummaryReactive(videoId, language)
                .flatMap(summaryResult -> {
                    if (summaryResult.isSuccess()) {
                        log.info("Summary successful for videoId: {}. Saving to DB first.", videoId);
                        // *** 먼저 summary를 DB에 저장 ***
                        return saveSummaryToDatabase(videoId, summaryResult)
                            .then(Mono.fromCallable(() -> {
                                // 퀴즈 생성 캐시 확인
                                quizProcessingCache.computeIfAbsent(videoId, key -> {
                                    log.info("Starting quiz generation for videoId: {}", key);
                                    return generateQuizzesInBackground(userId, key, language, summaryResult)
                                            .doFinally(signal -> {
                                                log.info("Quiz processing completed for videoId: {}. Removing from cache.", key);
                                                quizProcessingCache.remove(key);
                                            });
                                }).subscribeOn(Schedulers.boundedElastic())
                                .subscribe(
                                        vd -> log.info("Background quiz generation completed for videoId: {}", videoId),
                                        err -> log.error("Background quiz generation failed for videoId: {}", videoId, err)
                                );
                                return summaryResult;
                            }));
                    } else {
                        log.warn("Summary generation failed for videoId: {} with status: {}.", videoId, summaryResult.getStatus());
                        return Mono.just(summaryResult);
                    }
                })
                .onErrorResume(exception -> {
                    log.error("Critical error during summary phase for videoId: {}.", videoId, exception);
                    SummaryResult errorSummary = new SummaryResult(SummaryStatus.FAILED, "Error during summary: " + exception.getMessage());
                    return Mono.just(errorSummary);
                })
                .timeout(Duration.ofMinutes(5), Mono.defer(() -> {
                    log.error("Summary generation timed out for videoId: {}", videoId);
                    SummaryResult timeoutSummary = new SummaryResult(SummaryStatus.FAILED, "Summary generation timed out after 5 minutes");
                    return Mono.just(timeoutSummary);
                }));
    }

    /*
    * Summary만 DB에 저장하는 메서드 (동기적 처리)
     */
    private Mono<Void> saveSummaryToDatabase(String videoId, SummaryResult summaryResult) {
        if (summaryResult.getStatus() != SummaryStatus.SUCCESS) {
            log.warn("Attempted to save summary for videoId {} but status was {}. Skipping save.", videoId, summaryResult.getStatus());
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> {
                    log.info("Saving summary to DB for videoId: {}", videoId);
                    
                    Video videoToSave = videoRepository.findByYoutubeId(videoId)
                            .orElseGet(() -> {
                                log.info("Video with id {} not found in DB. Creating new entry.", videoId);
                                return new Video(videoId, summaryResult.getSummary());
                            });

                    // Summary 설정
                    if (videoToSave.getSummary() == null || videoToSave.getSummary().isBlank()) {
                        videoToSave.setSummary(summaryResult.getSummary());
                    }

                    // Video 저장
                    videoRepository.save(videoToSave);
                    log.info("Summary saved to DB for videoId: {}", videoId);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * 퀴즈만 백그라운드에서 생성하고 저장하는 메서드
     */
    private Mono<Void> generateQuizzesInBackground(Long userId, String videoId, String language, SummaryResult summaryResult) {
        log.info("Starting background quiz generation for videoId: {}", videoId);
        String summaryContent = summaryResult.getSummary();

        if (summaryContent == null || summaryContent.isBlank()) {
            log.warn("Summary content is blank for videoId: {}. Skipping quiz generation.", videoId);
            return Mono.empty();
        }

        // 퀴즈 생성
        Mono<List<Quiz>> multiChoiceMono = reactiveGptClient.sendSummariesAndGetQuizzesReactive(videoId, summaryContent, QuizType.MULTIPLE_CHOICE)
                .doOnError(e -> log.error("Error generating Multiple Choice quizzes for {}", videoId, e))
                .onErrorReturn(Collections.emptyList());

        Mono<List<Quiz>> shortAnswerMono = reactiveGptClient.sendSummariesAndGetQuizzesReactive(videoId, summaryContent, QuizType.SHORT_ANSWER)
                .doOnError(e -> log.error("Error generating Short Answer quizzes for {}", videoId, e))
                .onErrorReturn(Collections.emptyList());

        return Mono.zip(multiChoiceMono, shortAnswerMono)
                .flatMap(tuple -> {
                    List<Quiz> allQuizzes = new ArrayList<>();
                    allQuizzes.addAll(tuple.getT1());
                    allQuizzes.addAll(tuple.getT2());
                    log.info("Generated {} total quizzes for videoId: {}", allQuizzes.size(), videoId);

                    // 퀴즈만 저장
                    return saveQuizzesToDatabase(videoId, allQuizzes);
                })
                .timeout(Duration.ofMinutes(10), Mono.defer(() -> {
                    log.error("Background quiz generation timed out for videoId: {}", videoId);
                    return Mono.error(new RuntimeException("Quiz generation timed out for videoId: " + videoId));
                }))
                .then();
    }

    /**
     * 퀴즈만 DB에 저장하는 메서드
     */
    private Mono<Void> saveQuizzesToDatabase(String videoId, List<Quiz> quizzes) {
        return Mono.fromRunnable(() -> {
                    log.info("Saving {} quizzes to DB for videoId: {}", quizzes.size(), videoId);
                    saveQuizzes(videoId, quizzes);
                    log.info("Quizzes saved to DB for videoId: {}", videoId);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
     // saveQuizzes remains largely the same, just ensure logging indicates it's part of the background task
    private void saveQuizzes(String videoId, List<Quiz> quizzesToSave) {
        if (quizzesToSave != null && !quizzesToSave.isEmpty()) {
            // Optional: Delete existing quizzes first if you always want a fresh set.
            // log.debug("Background task: Deleting existing quizzes for videoId: {}", videoId);
            // int deletedCount = quizRepository.deleteByYoutubeId(videoId); // Assuming this method exists and returns count
            // log.debug("Background task: Deleted {} existing quizzes.", deletedCount);


            log.info("Background task: Saving {} quizzes to DB for videoId: {}", quizzesToSave.size(), videoId);
            quizzesToSave.forEach(q -> q.setYoutubeId(videoId)); // Ensure youtubeId is set
            quizRepository.saveAll(quizzesToSave); // Blocking saveAll
        } else {
            log.info("Background task: No quizzes generated or found to save for videoId: {}", videoId);
        }
    }

    //요약 요청하기 전 밴 된 영상인지 먼저 확인하기
    private Mono<Optional<Ban>> checkIfBanned(String videoId) {
        return Mono.fromCallable(() -> {
                log.debug("Checking ban status for videoId: {}", videoId);
                // blocking call
                return banRepository.findByYoutubeId(videoId); // Optional<Ban>
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    private void publishVideoSavedEvent(String youtubeId, Long userId, String title) {
        eventPublisher.publishEvent(new VideoProcessedEvent(youtubeId, userId, title));
        log.info("Published video processed event for youtubeId: {}", youtubeId);
    }
    
    //퀴즈 생성 중인지 캐시로 확인하고 boolean 반환
    public boolean isQuizProcessing(String videoId) {
        synchronized (this) {
            return quizProcessingCache.containsKey(videoId) || processingCache.containsKey(videoId);
        }
    }
}
