package com.example.youtube_lecture_helper.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuizStatisticsDto {
    private Long id;
    private String question;
    private Integer difficulty;
    private Long totalAttempts;
    private Long correctAttempts;
    private BigDecimal accuracyRate;

    public QuizStatisticsDto(Long id, Long totalAttempts, Long correctAttempts, Double accuracyRate) {
        this.id = id;
        this.totalAttempts = totalAttempts;
        this.correctAttempts = correctAttempts;
        this.accuracyRate = accuracyRate != null ? 
            BigDecimal.valueOf(accuracyRate).setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
    }
}