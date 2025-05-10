package com.example.youtube_lecture_helper.dto;

import com.example.youtube_lecture_helper.SummaryStatus;
import com.example.youtube_lecture_helper.openai_api.SummaryResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor // Generates constructor for final fields
public class VideoSummaryResponseDto {
    private final SummaryStatus status;
    private final String summary; // Can be null if status is not SUCCESS
    private final String message; // Optional message (e.g., "Processing started", "Error details")

    // Static factory methods for convenience
    public static VideoSummaryResponseDto success(String summary) {
        return new VideoSummaryResponseDto(SummaryStatus.SUCCESS, summary, "Summary generated successfully. Quizzes are being processed.");
    }

    public static VideoSummaryResponseDto processingStarted() {
        // Use a specific status or just a message if you prefer
        return new VideoSummaryResponseDto(SummaryStatus.PROCESSING, null, "Video processing initiated. Summary and quizzes will be generated.");
    }

    public static VideoSummaryResponseDto failed(String errorMessage) {
        return new VideoSummaryResponseDto(SummaryStatus.FAILED, null, errorMessage);
    }

    public static VideoSummaryResponseDto fromSummaryResult(SummaryResult summaryResult) {
        if (summaryResult.isSuccess()) {
            return success(summaryResult.getSummary());
        } else {
            // Provide more specific error message if available in SummaryResult
            String message = summaryResult.getSummary() != null ? summaryResult.getSummary() : "Summary generation failed.";
            return failed(message);
        }
    }
}
