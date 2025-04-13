package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.SummaryStatus;
import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.entity.Video;
import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.openai_api.QuizType;
import com.example.youtube_lecture_helper.openai_api.SummaryResult;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class VideoService {
    private final VideoRepository videoRepository;
    private final OpenAIGptClient gptClient;
    private final QuizRepository quizRepository;

    public VideoService(OpenAIGptClient gptClient, QuizRepository quizRepository,
                        VideoRepository videoRepository){
        this.gptClient = gptClient;
        this.quizRepository = quizRepository;
        this.videoRepository = videoRepository;
    }

    public Boolean check_if_exists(){
        return check_if_exists();
    }

    public SummaryResult generateSummaryQuizAndSave(String videoId) {
        CompletableFuture<SummaryResult> videoSummaryFuture = gptClient.getVideoSummaryAsync(videoId, "ko");

        SummaryResult summaryResult = videoSummaryFuture.join();

        if (summaryResult.getStatus()==SummaryStatus.NO_SUBTITLE || summaryResult.getStatus()== SummaryStatus.NOT_LECTURE){
            return summaryResult;   //퀴즈 생성하지 않고 반환
        }
        String summaryString = summaryResult.getSummary();

        CompletableFuture<List<Quiz>> futureQuizzesV2_choice = gptClient.sendSummariesAndGetQuizzesAsyncV2(videoId, summaryString, QuizType.MULTIPLE_CHOICE);
        CompletableFuture<List<Quiz>> futureQuizzesV2_short = gptClient.sendSummariesAndGetQuizzesAsyncV2(videoId, summaryString, QuizType.SHORT_ANSWER);

        //List<Quiz> quizList = futureQuizzes.join();

        List<Quiz> quizListV2 = new ArrayList<>();
        quizListV2.addAll(futureQuizzesV2_choice.join());
        quizListV2.addAll(futureQuizzesV2_short.join());

        quizRepository.saveAll(quizListV2);
        videoRepository.save(new Video(videoId,summaryResult.getSummary()));
        return summaryResult;
    }

    public SummaryResult getSummary(String youtubeId){
        //repo 보고 없으면 gptClient 호출하기
        Optional<Video> summary = videoRepository.findByYoutubeId(youtubeId);
        //summary 없으면 퀴즈와 함께 생성 후 둘다 레포지토리에 저장
        if(summary.isEmpty()){
            return generateSummaryQuizAndSave(youtubeId);
        }else{
            return new SummaryResult(SummaryStatus.SUCCESS,summary.get().getSummary());
        }
    }

}
