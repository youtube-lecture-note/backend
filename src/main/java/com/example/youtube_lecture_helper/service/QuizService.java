package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.UserQuizAnswerDto;
import com.example.youtube_lecture_helper.exception.QuizNotFoundException;
import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.openai_api.QuizType;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizService {
    private final QuizRepository quizRepository;
    private final OpenAIGptClient gptClient;    //주관식 정답 맞출 때 필요
    private final QuizLogService quizLogService;
    public QuizService(OpenAIGptClient gptClient, QuizRepository quizRepository, QuizLogService quizLogService){
        this.gptClient = gptClient;
        this.quizRepository = quizRepository;
        this.quizLogService = quizLogService;
    }

    public List<Quiz> getQuizzes(String youtubeId){
        return quizRepository.findByYoutubeId(youtubeId);
    }

    //틀린 결과만 제공
    public List<Long> getWrongAnswerQuizIds (List<UserQuizAnswerDto> userQuizAnswerDtoList){
        return userQuizAnswerDtoList.stream()
                .filter(userQuizAnswerDto -> !isCorrect(userQuizAnswerDto)) //false만 필터링
                .map(UserQuizAnswerDto::getQuizId)
                .toList();
    }

    private boolean isCorrect(UserQuizAnswerDto userQuizAnswerDto){
        boolean result;
        long quizId = userQuizAnswerDto.getQuizId();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(Long.toString(quizId)));

        if(quiz.getQuizType()== QuizType.MULTIPLE_CHOICE){  //객관식이면 단순비교
            result = quiz.getCorrectAnswer().equals(userQuizAnswerDto.getUserAnswer());
        }else{ //주관식이면 gpt한테 물어보기
            result = gptClient.isCorrectSubjectiveAnswer(
                    quiz.getQuestion(),
                    quiz.getCorrectAnswer(),
                    userQuizAnswerDto.getUserAnswer()
            );
        }
        //오답일 경우 quizLogRepo에 오답 기록 저장
        if(!result){
            quizLogService.saveIncorrectAnswer(userQuizAnswerDto);
        }

        return result;
    }

}
