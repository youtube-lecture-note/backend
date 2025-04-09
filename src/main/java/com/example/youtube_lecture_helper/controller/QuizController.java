package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.openai_api.Quiz;
import com.example.youtube_lecture_helper.repository.DummyQuizRepository;
import com.example.youtube_lecture_helper.service.SummaryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QuizController {

    private final OpenAIGptClient gptClient;
    private final DummyQuizRepository dummyQuizRepository;
    public  QuizController(OpenAIGptClient gptClient, DummyQuizRepository dummyQuizRepository){
        this.gptClient = gptClient;
        this.dummyQuizRepository=dummyQuizRepository;
    }

    @GetMapping("/api/quizzes")
    public ResponseEntity<ApiResponse<List<Quiz>>> getAllQuizzes(@RequestParam String videoId){
        List<Quiz> quizzes = dummyQuizRepository.findByVideoId(videoId);
        if(quizzes.size()>0) {
            return ApiResponse.buildResponse(HttpStatus.OK, "success", dummyQuizRepository.findByVideoId(videoId));
        }else return ApiResponse.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "NO QUIZ GENERATED", null);

    }
}
