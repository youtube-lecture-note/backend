package com.example.youtube_lecture_helper.openai_api;

import com.example.youtube_lecture_helper.SummaryStatus;
import lombok.Getter;

@Getter
public class SummaryResult {
    private SummaryStatus status;
    private String summary;

    public SummaryResult(SummaryStatus status, String summary) {
        this.status = status;
        this.summary = summary;
    }
    public boolean isSuccess(){
        return getStatus()==SummaryStatus.SUCCESS;
    }
}
