package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.example.YoutubeSubtitleExtractor.getSubtitles;

public class Main {

    public static void main(String[] args) {

        //SzrVaYDLiHs : 빅뱅
        //yHzJikY3LBo : 수소폭탄
        //nJ-R9G1pepU : 사물궁이
        //Bh6WtpsStpM : 중학교 2학년 과학 (17분)
        //veTpPfu1-o8 : 뱀(58분)
        //vLaFAKnaRJU : 영어강의

        OpenAIGptClient gptClient = new OpenAIGptClient(10,"gpt-4o-mini","gpt-4o-2024-08-06");

        CompletableFuture<String> videoSummaryFuture = gptClient.getVideoSummaryAsync("yHzJikY3LBo", "ko");

        if(videoSummaryFuture!=null){
            String videoSummary = videoSummaryFuture.join();
            if(videoSummary!=null && videoSummary.equals("-1"))
                System.out.println("is not lecture? : "+videoSummary);

            if(videoSummary!=null && !videoSummary.equals("-1")){
                System.out.println(videoSummary);
                CompletableFuture<List<Quiz>> futureQuizzes = gptClient.sendSummariesAndGetQuizzesAsync(videoSummary);
                List<Quiz> quizzList = futureQuizzes.join();

                //퀴즈 출력
                System.out.println();
                for(Quiz quiz: quizzList){
                    System.out.println(quiz.getQuestion());
                    int i=1;
                    for(String option: quiz.getOptions()){
                        System.out.println((i++) +". "+ option);
                    }
                    System.out.println("answer: "+quiz.getCorrectAnswer());
                    System.out.println("timestamp: "+quiz.getTimestamp());
                    System.out.println();
                }
            }

//            try {
//                List<List<SubtitleLine>> captions  = YoutubeSubtitleExtractor.getSubtitles("Bh6WtpsStpM", "ko");
//                String captionString = captions.stream()
//                        .flatMap(List::stream)  // List<SubtitleLine>의 각 요소를 평탄화하여 스트림으로 변환
//                        .map(SubtitleLine::toString)  // SubtitleLine 객체를 문자열로 변환
//                        .collect(Collectors.joining(","));
////                String quiz = gptClient.sendCaptionAndGetQuiz(captionString);
////                System.out.println(quiz);
//                String quiz = gptClient.sendCaptionAndGetQuiz(captionString);
//                System.out.println(quiz);
//            }catch(Exception e){
//                e.printStackTrace();
//            }
        }else{
            System.out.println("Error occured");
        }
        gptClient.shutdown();
    }
}