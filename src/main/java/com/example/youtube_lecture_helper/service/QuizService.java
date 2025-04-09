package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.openai_api.Quiz;
import com.example.youtube_lecture_helper.repository.DummyQuizRepository;
import com.example.youtube_lecture_helper.repository.DummySummaryRepository;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizService {
    private final QuizRepository quizRepository;
    private final OpenAIGptClient gptClient;
    public QuizService(OpenAIGptClient gptClient, DummyQuizRepository dummyQuizRepository, DummySummaryRepository dummySummaryRepository){
        this.gptClient = gptClient;
        this.quizRepository = dummyQuizRepository;
    }

    //dto로 변경?
    public List<Quiz> getQuizzes(String videoId){
        return quizRepository.findByVideoId(videoId);
    }
}
