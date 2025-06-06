package com.example.youtube_lecture_helper.dto;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class QuizSetSummaryDto {
    private Long quizSetId;
    private String name;
    private LocalDateTime createdAt;
}