package com.example.youtube_lecture_helper.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
public class QuizSetResultsDto {
    private Long quizSetId;
    private int totalQuizCount;
    private int participantCount;
    private List<ParticipantResultDto> participantResults;
    private List<QuizStatisticsDto> quizStatistics;
}