package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.repository.VideoRepository;
import org.springframework.stereotype.Service;

@Service
public class VideoService {
    private final CreateSummaryAndQuizService createSummaryAndQuizService;
    private final VideoRepository videoRepository;

    public VideoService(CreateSummaryAndQuizService createSummaryAndQuizService,
                        VideoRepository videoRepository){
        this.createSummaryAndQuizService = createSummaryAndQuizService;
        this.videoRepository = videoRepository;
    }


}
