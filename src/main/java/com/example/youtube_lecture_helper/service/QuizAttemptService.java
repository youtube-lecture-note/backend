package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.QuizAttemptDto;
import com.example.youtube_lecture_helper.entity.QuizAttempt;
import com.example.youtube_lecture_helper.repository.QuizAttemptProjection;
import com.example.youtube_lecture_helper.repository.QuizAttemptRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizAttemptService {
    private final QuizAttemptRepository quizAttemptRepository;
    public QuizAttemptService(QuizAttemptRepository quizAttemptRepository){
        this.quizAttemptRepository = quizAttemptRepository;
    }
    //userId로 전체 퀴즈 풀이 기록 조회
    public List<QuizAttemptProjection> getQuizHistorySummaries(Long userId) {
        return quizAttemptRepository.findQuizSetSummariesByUserId(userId);
        // Controller에서는 이 결과를 받아 (오답수 / 전체문제수) 계산 후 반환
    }

    //userId, youtubeId로 특정 비디오 관련 퀴즈 기록 조회
    public List<QuizAttemptProjection> getVideoQuizHistorySummaries(Long userId, String youtubeId) {
        return quizAttemptRepository.findQuizSetSummariesByUserIdAndYoutubeId(userId, youtubeId);
        // Controller에서는 이 결과를 받아 (오답수 / 전체문제수) 계산 후 반환
    }

    // 특정 QuizSet 상세 기록 조회
    public List<QuizAttemptDto> getQuizAttemptDetails(Long quizSetId) {
        return quizAttemptRepository.findDetailedAttemptDTOsByQuizSetId(quizSetId);
        // Controller에서는 이 결과를 DTO 등으로 변환하여 반환
    }

}
