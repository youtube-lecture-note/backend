package com.example.youtube_lecture_helper.openai_api;

import com.example.youtube_lecture_helper.SummaryStatus;
import lombok.Getter;

@Getter
public class SummaryResult {
    private final SummaryStatus status;
    private final String summary;

    public SummaryResult(SummaryStatus status, String summary) {
        this.status = status;
        this.summary = summary;
    }
}
