package com.example.youtube_lecture_helper.event;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VideoProcessedEvent {
    private final String youtubeId;
    private final Long userId;
    private final String title;

    public VideoProcessedEvent(String youtubeId, Long userId, String title) {
        this.youtubeId = youtubeId;
        this.userId = userId;
        this.title = title;
    }

    // Getters
}
