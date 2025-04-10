package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizService {
    private final QuizRepository quizRepository;
    private final OpenAIGptClient gptClient;    //주관식 정답 맞출 때 필요
    public QuizService(OpenAIGptClient gptClient, QuizRepository quizRepository){
        this.gptClient = gptClient;
        this.quizRepository = quizRepository;
    }

    public List<Quiz> getQuizzes(String youtubeId){
        return quizRepository.findByYoutubeId(youtubeId);
    }

}
