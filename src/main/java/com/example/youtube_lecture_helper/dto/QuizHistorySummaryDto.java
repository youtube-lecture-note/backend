package com.example.youtube_lecture_helper.dto;

import java.time.LocalDateTime;

public interface QuizHistorySummaryDto {
    String getYoutubeId();   // Video.externalId
    String getUserVideoName();     // UserVideoCategory.customName 또는 Video.title
    Long getVideoId();      // Video.id
    LocalDateTime getDate();
    Long getAttemptId();
    Long getTotalQuizzes();   // 해당 QuizSet의 전체 문항 수
    Long getWrongCount(); // 해당 QuizSet의 오답 수
}
