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
public class UserStatisticsDto {
    private String name;
    private Integer totalAttempts;
    private Integer correctAttempts;
    private BigDecimal accuracyRate;
}