package com.example.youtube_lecture_helper.dto;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuizStatisticsDto {
    private Long id;
    private String question;
    private Integer difficulty;
    private Integer totalAttempts;
    private Integer correctAttempts;
    private BigDecimal accuracyRate;
}