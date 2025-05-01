package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.dto.QuizAttemptDto;
import com.example.youtube_lecture_helper.dto.UserQuizAnswerDto;
import com.example.youtube_lecture_helper.entity.QuizAttempt;
import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.service.QuizAttemptService;
import com.example.youtube_lecture_helper.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final OpenAIGptClient gptClient;
    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;

    @GetMapping("/api/quizzes")
    public ResponseEntity<ApiResponse<List<Quiz>>> getAllQuizzes(@RequestParam String videoId){
        List<Quiz> quizzes = quizService.getQuizzes(videoId);
        if(quizzes.size()>0) {
            return ApiResponse.buildResponse(HttpStatus.OK, "success", quizzes);
        }else return ApiResponse.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "NO QUIZ GENERATED", null);
    }

    @GetMapping("/api/v2/quizzes")
    public ResponseEntity<ApiResponse<QuizService.CreatedQuizSetDTO>> getQuizzesWithDifficultyAndCount(
            @RequestParam String videoId,
            @RequestParam int difficulty,
            @RequestParam int count
    ){
        QuizService.CreatedQuizSetDTO quizzes = quizService.createQuizSetForUser(
                (long)1,difficulty,videoId,count
        );
        return ApiResponse.buildResponse(HttpStatus.OK,"success",quizzes);
    }

    //front에서는 userId=0으로 설정하고 전송.
    // UserQuizAnswerDto
    // private long userId;
    // private long quizId;
    // private boolean isShortQuiz;
    // private String userAnswer;
    @PostMapping("/api/quizzes/submit")
    public ResponseEntity<ApiResponse<List<Long>>> submitQuizAnswers(
            @RequestBody List<UserQuizAnswerDto> userQuizAnswerDtoList
            //@RequestHeader("Authorization") String token //토큰 나중에 추가하기
    ){
        //long userId = extractUserId(token)
        long userId = 1;
        userQuizAnswerDtoList.forEach(dto -> dto.setUserId(userId));
        List<Long> incorrectQuizIds = quizService.getWrongAnswerQuizIds(userQuizAnswerDtoList);

        return ApiResponse.buildResponse(
                HttpStatus.OK, "success", incorrectQuizIds
        );
    }
    @PostMapping("/api/async/quizzes/submit")
    public Mono<List<QuizAttemptDto>> submitQuizAnswers(
            @RequestParam Long quizSetId,
            @RequestBody List<UserQuizAnswerDto> answers
    ) {
        return quizAttemptService.processQuizAnswers(quizSetId, answers);
    }

}
