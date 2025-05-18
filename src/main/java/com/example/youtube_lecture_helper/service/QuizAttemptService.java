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
import com.example.youtube_lecture_helper.repository.QuizAttemptProjection;
import com.example.youtube_lecture_helper.repository.QuizAttemptRepository;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.repository.QuizSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final QuizSetRepository quizSetRepository;
    private final ReactiveGptClient reactiveGptClient;


    //userId로 전체 퀴즈 풀이 기록 조회
    public List<QuizAttemptProjection> getQuizHistorySummaries(Long userId) {
        return quizAttemptRepository.findQuizSetSummariesByUserId(userId);
        // Controller에서는 이 결과를 받아 (오답수 / 전체문제수) 계산 후 반환
    }

    //userId, youtubeId로 특정 비디오 관련 퀴즈 기록 조회
    public List<QuizAttemptProjection> getVideoQuizHistorySummaries(Long userId, String youtubeId) {
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

    public Mono<List<QuizAttemptDto>> processQuizAnswers(Long quizSetId, List<UserQuizAnswerDto> userQuizAnswerDtos) {
        // QuizSet 조회 (한 번만)
        Mono<QuizSet> quizSetMono = Mono.fromCallable(() ->
                quizSetRepository.findById(quizSetId)
                        .orElseThrow(() -> new QuizNotFoundException(Long.toString(quizSetId)))
        );

        // 각 답안에 대해 채점 및 QuizAttempt 생성
        return quizSetMono.flatMap(quizSet ->
                Flux.fromIterable(userQuizAnswerDtos)
                    .flatMap(userQuizAnswerDto -> {
                        long quizId = userQuizAnswerDto.getQuizId();

                        // 퀴즈 조회
                        Mono<Quiz> quizMono = Mono.fromCallable(() ->
                                quizRepository.findById(quizId)
                                        .orElseThrow(() -> new QuizNotFoundException(Long.toString(quizId)))
                        );

                        return quizMono.flatMap(quiz -> {
                            Mono<Boolean> isCorrectMono;

                            if (quiz.isSelective()) {
                                isCorrectMono = Mono.just(quiz.getCorrectAnswer().equals(userQuizAnswerDto.getUserAnswer()));
                            } else {
                                isCorrectMono = reactiveGptClient.isCorrectSubjectiveAnswer(
                                        quiz.getQuestion(),
                                        quiz.getCorrectAnswer(),
                                        userQuizAnswerDto.getUserAnswer()
                                );
                            }

                            return isCorrectMono.map(isCorrect -> {
                                QuizAttempt quizAttempt = new QuizAttempt();
                                quizAttempt.setQuiz(quiz);
                                quizAttempt.setQuizSet(quizSet);
                                quizAttempt.setUserAnswer(userQuizAnswerDto.getUserAnswer());
                                quizAttempt.setCorrect(isCorrect);
                                return quizAttempt;
                            });
                        });
                    })
                    // QuizAttempt 저장 (비동기)
                        .flatMap(quizAttempt -> Mono.fromCallable(() -> quizAttemptRepository.save(quizAttempt)))
                        // DTO로 변환
                        .map(this::toDto)
                        .collectList()
        );
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
