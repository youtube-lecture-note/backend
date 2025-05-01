package com.example.youtube_lecture_helper.dto;

import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.openai_api.SummaryResult;
import lombok.Getter;

import java.util.List;

// Using a record for simplicity (requires Java 16+)
// If using older Java, create a standard class with fields, constructor, getters.
public record VideoProcessingResult(
        SummaryResult summaryResult,
        List<Quiz> quizzes // Can be null or empty if summary failed or no quizzes generated
) {
    // Helper to check overall success (summary success + quizzes generated if applicable)
    public boolean isFullySuccessful() {
        return summaryResult != null && summaryResult.isSuccess() && quizzes != null; // Adjust based on whether quizzes are mandatory
    }

    public boolean hasSummary() {
        return summaryResult != null && summaryResult.isSuccess();
    }
}
