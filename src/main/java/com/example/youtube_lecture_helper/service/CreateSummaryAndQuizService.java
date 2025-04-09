package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.openai_api.Quiz;
import com.example.youtube_lecture_helper.openai_api.QuizType;
import com.example.youtube_lecture_helper.repository.DummyQuizRepository;
import com.example.youtube_lecture_helper.repository.DummySummaryRepository;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.repository.SummaryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CreateSummaryAndQuizService {
    private final OpenAIGptClient gptClient;
    private final SummaryRepository summaryRepository;
    private final QuizRepository quizRepository;

    public CreateSummaryAndQuizService(SummaryRepository summaryRepository, QuizRepository quizRepository, OpenAIGptClient gptClient ){
        this.summaryRepository = summaryRepository;
        this.quizRepository = quizRepository;
        this.gptClient = gptClient;
    }

    //null or summary 반환
    public String generateSummaryQuizAndSave(String videoId) {
        CompletableFuture<String> videoSummaryFuture = gptClient.getVideoSummaryAsync("Bh6WtpsStpM", "ko");

        String videoSummary = videoSummaryFuture.join();

        if (videoSummary==null || (videoSummary != null && videoSummary.equals("-1"))){
            return null;   //not a lecture video
        }
        
        CompletableFuture<List<Quiz>> futureQuizzesV2_choice = gptClient.sendSummariesAndGetQuizzesAsyncV2(videoId, videoSummary, QuizType.MULTIPLE_CHOICE);
        CompletableFuture<List<Quiz>> futureQuizzesV2_short = gptClient.sendSummariesAndGetQuizzesAsyncV2(videoId, videoSummary, QuizType.SHORT_ANSWER);

        //List<Quiz> quizList = futureQuizzes.join();

        List<Quiz> quizListV2 = new ArrayList<>();
        quizListV2.addAll(futureQuizzesV2_choice.join());
        quizListV2.addAll(futureQuizzesV2_short.join());

        quizRepository.save(videoId,quizListV2);
        summaryRepository.save(videoId,videoSummary);
        return videoSummary;
    }
}
