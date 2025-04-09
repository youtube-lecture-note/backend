//package com.example.youtube_lecture_helper.openai_api;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//
//public class Main {
//
//    public static void main(String[] args) {
//
//        //SzrVaYDLiHs : 빅뱅
//        //yHzJikY3LBo : 수소폭탄
//        //nJ-R9G1pepU : 사물궁이
//        //Bh6WtpsStpM : 중학교 2학년 과학 (17분)
//        //veTpPfu1-o8 : 뱀(58분)
//        //vLaFAKnaRJU : 영어강의
//
//        OpenAIGptClient gptClient = new OpenAIGptClient(10, "gpt-4o-mini", "gpt-4o-2024-08-06");
//        //주관식 질문 정답확인
//        long start = System.currentTimeMillis();
////        System.out.println("user Answer correct? " + gptClient.isCorrectSubjectiveAnswer(
////                "불꽃 반응 실험의 장점은 무엇인가?",
////                "은 이온",
////                "Ag +",
////                "gpt-4o-mini")
////                + ", answer in " + (int) (System.currentTimeMillis()-start) + " ms"
////        );
//
//        CompletableFuture<String> videoSummaryFuture = gptClient.getVideoSummaryAsync("Bh6WtpsStpM", "ko");
//        // 17분: 17초
//        // 6분 : 10초
//        // 6분 :  7초
//        // 58분: 18초
//
//        if (videoSummaryFuture != null) {
//            String videoSummary = videoSummaryFuture.join();
//            long diff = System.currentTimeMillis() - start;
//            System.out.println("time: " + (int) diff + " ms");
//            if (videoSummary != null && videoSummary.equals("-1"))
//                System.out.println("is not lecture? : " + videoSummary);
//
//            if (videoSummary != null && !videoSummary.equals("-1")) {
//                System.out.println(videoSummary);
//                //CompletableFuture<List<Quiz>> futureQuizzes = gptClient.sendSummariesAndGetQuizzesAsync(videoSummary);
//
//                CompletableFuture<List<Quiz>> futureQuizzesV2_choice = gptClient.sendSummariesAndGetQuizzesAsyncV2(videoSummary, QuizType.MULTIPLE_CHOICE);
//                CompletableFuture<List<Quiz>> futureQuizzesV2_short = gptClient.sendSummariesAndGetQuizzesAsyncV2(videoSummary, QuizType.SHORT_ANSWER);
//
//                //List<Quiz> quizList = futureQuizzes.join();
//
//                List<Quiz> quizListV2 = new ArrayList<>();
//                quizListV2.addAll(futureQuizzesV2_choice.join());
//                quizListV2.addAll(futureQuizzesV2_short.join());
//
//                //퀴즈 출력
//                System.out.println();
//                for (Quiz quiz : quizListV2) {
//                    System.out.println(quiz.getQuestion());
//                    int i = 1;
//                    if (quiz.getOptions() != null) {
//                        for (String option : quiz.getOptions()) {
//                            System.out.println((i++) + ". " + option);
//                        }
//                    }
//                    System.out.println("answer: " + quiz.getCorrectAnswer());
//                    System.out.println("reason: " + quiz.getComment());
//                    System.out.println("timestamp: " + quiz.getTimestamp());
//                    System.out.println();
//                }
//            }
//
////            try {
////                List<List<SubtitleLine>> captions  = YoutubeSubtitleExtractor.getSubtitles("Bh6WtpsStpM", "ko");
////                String captionString = captions.stream()
////                        .flatMap(List::stream)  // List<SubtitleLine>의 각 요소를 평탄화하여 스트림으로 변환
////                        .map(SubtitleLine::toString)  // SubtitleLine 객체를 문자열로 변환
////                        .collect(Collectors.joining(","));
//////                String quiz = gptClient.sendCaptionAndGetQuiz(captionString);
//////                System.out.println(quiz);
////                String quiz = gptClient.sendCaptionAndGetQuiz(captionString);
////                System.out.println(quiz);
////            }catch(Exception e){
////                e.printStackTrace();
////            }
////        }else{
////            System.out.println("Error occured");
////        }
//            gptClient.shutdown();
//        }
//    }
//}