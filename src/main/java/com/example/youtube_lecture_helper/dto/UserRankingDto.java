package com.example.youtube_lecture_helper.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRankingDto {
    private String username;
    private Integer totalAttempts;
    private BigDecimal accuracyRate;
    private Integer ranking;
}