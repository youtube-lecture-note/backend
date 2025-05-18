package com.example.youtube_lecture_helper.event;

import com.example.youtube_lecture_helper.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoEventListener {
    private final CategoryService categoryService;

    @EventListener
    public void handleVideoProcessedEvent(VideoProcessedEvent event) {
        try {
            categoryService.addVideoToCategory(
                    event.getUserId(),
                    event.getYoutubeId(),
                    CategoryService.DEFAULT_CATEGORY_ID,
                    event.getTitle()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
