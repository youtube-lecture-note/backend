package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.SummaryStatus;
import com.example.youtube_lecture_helper.entity.Video;
import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.openai_api.SummaryResult;
import com.example.youtube_lecture_helper.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VideoService {
    private final CreateSummaryAndQuizService createSummaryAndQuizService;
    private final VideoRepository videoRepository;

    public VideoService(CreateSummaryAndQuizService createSummaryAndQuizService,
                        VideoRepository videoRepository){
        this.createSummaryAndQuizService = createSummaryAndQuizService;
        this.videoRepository = videoRepository;
    }

    public SummaryResult getSummary(String youtubeId){
        //repo 보고 없으면 gptClient 호출하기
        Optional<Video> summary = videoRepository.findByYoutubeId(youtubeId);
        //summary 없으면 퀴즈와 함께 생성 후 둘다 레포지토리에 저장
        if(summary.isEmpty()){
            return createSummaryAndQuizService.generateSummaryQuizAndSave(youtubeId);
        }else{
            return new SummaryResult(SummaryStatus.SUCCESS,summary.get().getSummary());
        }
    }

}
