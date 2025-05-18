package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.QuizAttemptDto;
import com.example.youtube_lecture_helper.dto.QuizAttemptWithAnswerDto;
import com.example.youtube_lecture_helper.dto.UserQuizAnswerDto;
import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.entity.QuizAttempt;
import com.example.youtube_lecture_helper.entity.QuizSet;
import com.example.youtube_lecture_helper.exception.AccessDeniedException;
import com.example.youtube_lecture_helper.exception.QuizNotFoundException;
import com.example.youtube_lecture_helper.exception.QuizSetNotFoundException;
import com.example.youtube_lecture_helper.openai_api.ReactiveGptClient;
import com.example.youtube_lecture_helper.dto.QuizHistorySummaryDto;
import com.example.youtube_lecture_helper.repository.QuizAttemptRepository;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.repository.QuizSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizAttemptService {
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final QuizSetRepository quizSetRepository;
    private final ReactiveGptClient reactiveGptClient;


    //userId로 전체 퀴즈 풀이 기록 조회
    public List<QuizHistorySummaryDto> getQuizHistorySummaries(Long userId) {
        return quizAttemptRepository.findQuizSetSummariesByUserId(userId);
        // Controller에서는 이 결과를 받아 (오답수 / 전체문제수) 계산 후 반환
    }

    //userId, youtubeId로 특정 비디오 관련 퀴즈 기록 조회
    public List<QuizHistorySummaryDto> getVideoQuizHistorySummaries(Long userId, String youtubeId) {
        return quizAttemptRepository.findQuizSetSummariesByUserIdAndYoutubeId(userId, youtubeId);
    }

    // 특정 QuizSet 상세 기록 조회
    public List<QuizAttemptWithAnswerDto> getQuizAttemptDetails(Long quizSetId,Long userId) {
        QuizSet quizSet = quizSetRepository.findById(quizSetId)
                .orElseThrow(() -> new QuizSetNotFoundException("QuizSet not found with id: " + quizSetId));
        if (!quizSet.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to access this quiz set");
        }
        return quizAttemptRepository.findDetailedAttemptsWithAnswersByQuizSetId(quizSetId);
        // Controller에서는 이 결과를 DTO 등으로 변환하여 반환
    }
    //join 2번씀. 성능 문제 있을수도.
    public Mono<List<QuizAttemptDto>> processQuizAnswers(Long quizSetId, List<UserQuizAnswerDto> userQuizAnswerDtos) {
        // 1. 모든 퀴즈 ID 수집 (사용자 답변으로부터)
        List<Long> quizIdsFromUserAnswers = userQuizAnswerDtos.stream()
                .map(UserQuizAnswerDto::getQuizId)
                .collect(Collectors.toList());

        // 2. QuizSet과 모든 관련 Quiz를 병렬로 조회
        // QuizSet을 조회할 때, 연관된 모든 QuizAttempt+Quiz들도 함께 로드되어야 함
        Mono<QuizSet> quizSetMono = Mono.fromCallable(() ->
                quizSetRepository.findByIdWithAttempts(quizSetId)
                        .orElseThrow(() -> new QuizNotFoundException("QuizSet not found with ID: " + quizSetId))
        ).subscribeOn(Schedulers.boundedElastic()); // DB 호출은 boundedElastic 스레드에서

        // 사용자 답변에 해당하는 Quiz 객체들을 조회하여 Map으로 만듦
        Mono<Map<Long, Quiz>> quizDetailsMapMono = Mono.fromCallable(() ->
                quizRepository.findAllByIdIn(quizIdsFromUserAnswers).stream()
                        .collect(Collectors.toMap(Quiz::getId, Function.identity()))
        ).subscribeOn(Schedulers.boundedElastic());

        // 3. 두 Mono를 결합하여 처리
        return Mono.zip(quizSetMono, quizDetailsMapMono)
                .flatMap(tuple -> {
                    QuizSet quizSet = tuple.getT1();
                    Map<Long, Quiz> quizDetailsMap = tuple.getT2();

                    // QuizSet에 이미 로드된 QuizAttempt들을 Quiz ID를 키로 하는 Map으로 변환
                    // 이것은 업데이트할 기존 Attempt를 빠르게 찾기 위함입니다.
                    Map<Long, QuizAttempt> existingAttemptsMap = quizSet.getAttempts().stream()
                            .collect(Collectors.toMap(
                                    attempt -> attempt.getQuiz().getId(), // QuizAttempt가 Quiz 객체 참조를 가지고 있어야 함
                                    Function.identity()
                            ));

                    // 4. 각 사용자 답변을 처리하여 기존 QuizAttempt를 업데이트
                    return Flux.fromIterable(userQuizAnswerDtos)
                            .flatMap(userQuizAnswerDto -> {
                                Long currentQuizId = userQuizAnswerDto.getQuizId();
                                Quiz quiz = quizDetailsMap.get(currentQuizId); // Quiz 상세 정보 (질문, 정답 등)
                                if (quiz == null) {
                                    log.error("Quiz details not found for quiz ID: {}", currentQuizId);
                                    return Mono.error(new QuizNotFoundException("Quiz details not found for quiz ID: " + currentQuizId));
                                }

                                QuizAttempt existingAttempt = existingAttemptsMap.get(currentQuizId);
                                if (existingAttempt == null) {
                                    // 이 경우는 QuizSet 생성 시 해당 Quiz에 대한 QuizAttempt가 생성되지 않았음을 의미
                                    // 또는 userQuizAnswerDto에 quizSet에 속하지 않은 quizId가 온 경우
                                    log.error("Pre-existing QuizAttempt not found in QuizSet {} for quiz ID: {}", quizSetId, currentQuizId);
                                    return Mono.error(new QuizNotFoundException(
                                            "Pre-existing QuizAttempt not found in QuizSet " + quizSetId + " for quiz ID: " + currentQuizId
                                    ));
                                }

                                // 5. 정답 여부 판단 (선택형/주관형)
                                Mono<Boolean> isCorrectMono;
                                if (quiz.isSelective()) {
                                    isCorrectMono = Mono.just(
                                            Optional.ofNullable(quiz.getCorrectAnswer())
                                                    .map(ca -> ca.equals(userQuizAnswerDto.getUserAnswer()))
                                                    .orElse(false) // 정답이 null이면 오답 처리
                                    );
                                } else {
                                    isCorrectMono = reactiveGptClient.isCorrectSubjectiveAnswer(
                                            quiz.getQuestion(),
                                            quiz.getCorrectAnswer(),
                                            userQuizAnswerDto.getUserAnswer()
                                    );
                                }

                                // 6. 기존 QuizAttempt 업데이트
                                return isCorrectMono.map(isCorrect -> {
                                    existingAttempt.setUserAnswer(userQuizAnswerDto.getUserAnswer());
                                    existingAttempt.setCorrect(isCorrect);
                                    // quizSet.addQuizAttempt()는 호출할 필요 없음.
                                    // existingAttempt는 이미 quizSet.getAttempts() 컬렉션의 멤버이므로,
                                    // JPA/Hibernate가 변경을 감지하고 flush 시점에 UPDATE SQL을 실행합니다.
                                    return existingAttempt; // 업데이트된 Attempt 반환
                                });
                            })
                            .collectList() // 업데이트된 QuizAttempt 객체들의 리스트
                            .flatMap(updatedAttempts -> {
                                // 7. QuizSet 저장 (CascadeType.ALL에 의해 변경된 QuizAttempt들도 업데이트됨)
                                // 주의: 이 시점에서 updatedAttempts는 quizSet.getAttempts() 내의 객체와 동일한 참조를 가짐
                                return Mono.fromCallable(() -> {
                                            log.info("Saving QuizSet {} with updated attempts.", quizSet.getId());
                                            quizSetRepository.save(quizSet); // 변경 감지에 의해 QuizAttempt들도 업데이트됨
                                            return updatedAttempts; // DTO 변환을 위해 업데이트된 리스트를 그대로 반환
                                        })
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .flatMapMany(Flux::fromIterable)
                                        .map(this::toDto) // QuizAttempt를 QuizAttemptDto로 변환
                                        .collectList();
                            });
                });
    }

    private QuizAttemptDto toDto(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        QuizAttemptDto dto = new QuizAttemptDto();
        dto.setAttemptId(attempt.getId());
        dto.setUserAnswer(attempt.getUserAnswer());
        dto.setCorrect(attempt.isCorrect());
        dto.setQuizId(quiz.getId());
        dto.setQuestionText(quiz.getQuestion());
        dto.setYoutubeId(quiz.getYoutubeId());
        dto.setQuizSetId(attempt.getQuizSet().getId());
        return dto;
    }

}
