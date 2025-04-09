package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.repository.DummySummaryRepository;
import com.example.youtube_lecture_helper.repository.SummaryRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SummaryService {
    private final SummaryRepository summaryRepository;
    private final CreateSummaryAndQuizService createSummaryAndQuizService;
    private final OpenAIGptClient gptClient;
    public SummaryService(OpenAIGptClient gptClient, DummySummaryRepository dummySummaryRepository, CreateSummaryAndQuizService createSummaryAndQuizService){
        this.gptClient = gptClient;
        this.summaryRepository = dummySummaryRepository;
        this.createSummaryAndQuizService = createSummaryAndQuizService;
    }

    public String getSummary(String videoId){
        //repo 보고 없으면 gptClient 호출하기
        String summary = summaryRepository.findByVideoId(videoId);
        //summary 없으면 퀴즈와 함께 생성 후 둘다 레포지토리에 저장
        if(summary==null){
            summary = createSummaryAndQuizService.generateSummaryQuizAndSave(videoId);
        }
        return summary;
    }
}
