package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.UserQuizAnswerDto;
import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.entity.QuizLog;
import com.example.youtube_lecture_helper.entity.User;
import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.repository.QuizLogRepository;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import org.springframework.stereotype.Service;
import com.example.youtube_lecture_helper.exception.QuizNotFoundException;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class QuizLogService {
    private final QuizLogRepository quizLogRepository;
    private final QuizRepository quizRepository;

    public QuizLogService(QuizLogRepository quizLogRepository, QuizRepository quizRepository, OpenAIGptClient openAIGptClient){
        this.quizLogRepository = quizLogRepository;
        this.quizRepository = quizRepository;
    }

    public void saveIncorrectAnswer(UserQuizAnswerDto userQuizAnswerDto) {
        QuizLog quizLog = new QuizLog(
                new Quiz(userQuizAnswerDto.getQuizId()),
                new User(userQuizAnswerDto.getUserId()),
                LocalDate.now(),
                userQuizAnswerDto.getUserAnswer()
        );
        quizLogRepository.save(quizLog);
    }
}
