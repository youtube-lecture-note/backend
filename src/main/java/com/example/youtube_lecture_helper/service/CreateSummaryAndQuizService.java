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
     * Generates the summary and, upon success, triggers the quiz generation
     * and saving process in the background without waiting for it.
     *
     * @param videoId  YouTube Video ID
     * @param language Language code
     * @return Mono emitting the SummaryResult once generated.
     */
    private Mono<SummaryResult> generateSummaryAndTriggerQuizProcessing(Long userId, String videoId, String language) {
        log.debug("Executing summary generation pipeline for videoId: {}", videoId);
        return reactiveGptClient.getVideoSummaryReactive(videoId, language)
                .flatMap(summaryResult -> {
                    if (summaryResult.isSuccess()) {
                        log.info("Summary successful for videoId: {}. Triggering background quiz generation and saving.", videoId);
                        // *** Trigger background processing ***
                        // We don't wait for this Mono to complete.
                        // subscribe() kicks off the execution on a background thread.
                        generateAndSaveQuizzesInBackground(userId, videoId, language, summaryResult)
                                .subscribeOn(Schedulers.boundedElastic()) // Run background task on appropriate scheduler
                                .subscribe(
                                        vd -> log.info("Background quiz/save task completed successfully for videoId: {}", videoId),
                                        err -> log.error("Background quiz/save task failed for videoId: {}", videoId, err)
                                );
                        // Return the successful summary result immediately
                        return Mono.just(summaryResult);
                    } else {
                        // Summary failed, return the failure result
                        log.warn("Summary generation failed for videoId: {} with status: {}. No background task triggered.", videoId, summaryResult.getStatus());
                        return Mono.just(summaryResult);
                    }
                })
                .onErrorResume(exception -> {
                    log.error("Critical error during summary phase for videoId: {}. Cannot proceed.", videoId, exception);
                    SummaryResult errorSummary = new SummaryResult(SummaryStatus.FAILED, "Error during summary: " + exception.getMessage());
                    return Mono.just(errorSummary);
                })
                .timeout(Duration.ofMinutes(5), Mono.defer(() -> { // Timeout specifically for summary generation
                    log.error("Summary generation timed out for videoId: {}", videoId);
                    SummaryResult timeoutSummary = new SummaryResult(SummaryStatus.FAILED, "Summary generation timed out after 5 minutes");
                    return Mono.just(timeoutSummary);
                }))
                .log("SummaryGeneration." + videoId, Level.FINE, SignalType.ON_NEXT, SignalType.ON_ERROR);
    }


    /**
     * Runs in the background. Generates quizzes based on the summary and saves
     * both the summary (if video is new) and the quizzes to the database.
     * This method handles its own errors internally by logging.
     *
     * @param videoId       YouTube Video ID
     * @param language      Language code
     * @param summaryResult The successful summary result.
     * @return Mono<Void> indicating completion of the background task (or error).
     */
    private Mono<Void> generateAndSaveQuizzesInBackground(Long userId, String videoId, String language, SummaryResult summaryResult) {
        log.info("Starting background task: Generate quizzes and save for videoId: {}", videoId);
        String summaryContent = summaryResult.getSummary();

        if (summaryContent == null || summaryContent.isBlank()) {
            log.warn("Background task: Summary content is blank for videoId: {}. Skipping quiz generation.", videoId);
            // Still attempt to save the summary if video is new
            return saveResultToDatabase(videoId, summaryResult, Collections.emptyList())
                    .doOnError(e -> log.error("Background task: Failed to save empty summary for videoId: {}", videoId, e))
                    .then();
        }

        // Generate quizzes concurrently
        Mono<List<Quiz>> multiChoiceMono = reactiveGptClient.sendSummariesAndGetQuizzesReactive(videoId, summaryContent, QuizType.MULTIPLE_CHOICE)
                .doOnSubscribe(s -> log.debug("Background task: Starting Multiple Choice quiz generation for {}", videoId))
                .doOnError(e -> log.error("Background task: Error generating Multiple Choice quizzes for {}", videoId, e))
                .onErrorReturn(Collections.emptyList());

        Mono<List<Quiz>> shortAnswerMono = reactiveGptClient.sendSummariesAndGetQuizzesReactive(videoId, summaryContent, QuizType.SHORT_ANSWER)
                .doOnSubscribe(s -> log.debug("Background task: Starting Short Answer quiz generation for {}", videoId))
                .doOnError(e -> log.error("Background task: Error generating Short Answer quizzes for {}", videoId, e))
                .onErrorReturn(Collections.emptyList());

        // Combine quiz results
        return Mono.zip(multiChoiceMono, shortAnswerMono)
                .flatMap(tuple -> {
                    List<Quiz> allQuizzes = new ArrayList<>();
                    allQuizzes.addAll(tuple.getT1());
                    allQuizzes.addAll(tuple.getT2());
                    log.info("Background task: Successfully generated {} total quizzes ({} MC, {} SA) for videoId: {}",
                            allQuizzes.size(), tuple.getT1().size(), tuple.getT2().size(), videoId);

                    // Save summary (if needed) and quizzes
                    return saveResultToDatabase(videoId, summaryResult, allQuizzes)
                            .then(Mono.fromRunnable(() -> { //db에 video 저장된 뒤 event 발행해서 기본 category에 저장하기
                                String title = youtubeSubtitleExtractor.getYouTubeTitle(videoId);
                                // 이벤트 발행
                                eventPublisher.publishEvent(
                                        new VideoProcessedEvent(videoId, userId, title)
                                );
                                log.info("Published video processed event for videoId: {}", videoId);
                            }))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .timeout(Duration.ofMinutes(10), Mono.defer(() -> { // Timeout for quiz generation + saving
                    log.error("Background task: Quiz generation/saving timed out for videoId: {}", videoId);
                    // Decide if you want to save just the summary on timeout
                    // return saveResultToDatabase(videoId, summaryResult, Collections.emptyList());
                    return Mono.error(new RuntimeException("Background quiz/save task timed out for videoId: " + videoId));
                }))
                .then(); // Convert to Mono<Void>
    }


    /**
     * Saves the Video summary (if new) and Quizzes to the database.
     * Handles blocking JPA calls. Runs within the background task.
     *
     * @param videoId       YouTube Video ID
     * @param summaryResult The summary result (containing status and content)
     * @param quizzes       The list of quizzes to save (can be empty)
     * @return Mono<Void> indicating completion or error.
     */
    private Mono<Void> saveResultToDatabase(String videoId, SummaryResult summaryResult, List<Quiz> quizzes) {
        // Check if the summary was actually successful before saving
        if (summaryResult.getStatus() != SummaryStatus.SUCCESS) {
            log.warn("Attempted to save result for videoId {} but summary status was {}. Skipping save.", videoId, summaryResult.getStatus());
            return Mono.empty(); // Or Mono.error() if this is unexpected
        }

        return Mono.fromRunnable(() -> {
                    log.info("Background task: Attempting to save results to DB for videoId: {}", videoId);

                    // Use findOrSave pattern for Video to avoid race conditions if possible,
                    // or stick to the check-then-save approach.
                    Video videoToSave = videoRepository.findByYoutubeId(videoId)
                            .orElseGet(() -> {
                                log.info("Background task: Video with id {} not found in DB. Creating new entry.", videoId);
                                // Only create if it doesn't exist
                                return new Video(videoId, summaryResult.getSummary());
                            });

                    // Ensure summary is set if the video was pre-existing but lacked one
                    if (videoToSave.getSummary() == null || videoToSave.getSummary().isBlank()) {
                        log.info("Background task: Existing video {} lacked summary. Setting summary.", videoId);
                        videoToSave.setSummary(summaryResult.getSummary()); // Assume setter exists
                    }

                    // Save video (either new or updated summary)
                    videoRepository.save(videoToSave);
                    log.info("Background task: Video entity saved/updated for videoId: {}", videoId);


                    // Save Quizzes
                    saveQuizzes(videoId, quizzes);

                    log.info("Background task: Finished DB save process for videoId: {}", videoId);
                })
                .subscribeOn(Schedulers.boundedElastic()) // Run blocking DB operations on appropriate scheduler
                .then(); // Convert Mono<Runnable> completion signal to Mono<Void>
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

}
