package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.repository.VideoRepository;
import com.example.youtube_lecture_helper.repository.UserVideoCategoryRepository;
import com.example.youtube_lecture_helper.entity.Video;
import com.example.youtube_lecture_helper.entity.UserVideoCategory;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final CreateSummaryAndQuizService createSummaryAndQuizService;
    private final VideoRepository videoRepository;
    private final UserVideoCategoryRepository userVideoCategoryRepository;

    public String getVideoSummaryById(Long videoId){
        return videoRepository.findById(videoId).orElseThrow(()->new RuntimeException("no video found for id"))
                .getSummary();
    }

    public void updateUserVideoName(String youtubeId, Long userId, String title) {
        // 입력 검증
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        
        // youtubeId로 Video 찾기
        Video video = videoRepository.findByYoutubeId(youtubeId)
            .orElseThrow(() -> new RuntimeException("Video not found"));
        
        // video.id와 userId로 UserVideoCategory 찾기
        UserVideoCategory userVideoCategory = userVideoCategoryRepository
            .findByVideoIdAndUserId(video.getId(), userId)
            .orElseThrow(() -> new RuntimeException("User video category not found"));
        
        // userVideoName 업데이트
        userVideoCategory.setUserVideoName(title.trim());
        userVideoCategoryRepository.save(userVideoCategory);
    }

}
