package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QuizController {

    private final OpenAIGptClient gptClient;
    private final QuizService quizService;
    public  QuizController(OpenAIGptClient gptClient, QuizService quizService){
        this.gptClient = gptClient;
        this.quizService=quizService;
    }

    @GetMapping("/api/quizzes")
    public ResponseEntity<ApiResponse<List<Quiz>>> getAllQuizzes(@RequestParam String videoId){
        List<Quiz> quizzes = quizService.getQuizzes(videoId);
        if(quizzes.size()>0) {
            return ApiResponse.buildResponse(HttpStatus.OK, "success", quizzes);
        }else return ApiResponse.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "NO QUIZ GENERATED", null);

    }
}
