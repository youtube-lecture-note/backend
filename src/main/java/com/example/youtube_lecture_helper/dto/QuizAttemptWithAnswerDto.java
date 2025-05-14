package com.example.youtube_lecture_helper.dto;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
@Getter
public class QuizAttemptWithAnswerDto {
    private Long attemptId;
    private String userAnswer;
    private Boolean isCorrect;
    private Long quizId;
    private String question;
    private String youtubeId;
    private Long quizSetId;

    private String correctAnswer;
    private String comment;
    private boolean selective;
    private byte difficulty;
    private int timestamp;
    private List<String> options;

    // 생성자
    public QuizAttemptWithAnswerDto(
            Long attemptId, String userAnswer, Boolean isCorrect,
            Long quizId, String question, String youtubeId, Long quizSetId,
            String option1, String option2, String option3, String option4,
            String correctAnswer, String comment, boolean selective,
            byte difficulty, int timestamp) {
        this.attemptId = attemptId;
        this.userAnswer = userAnswer;
        this.isCorrect = isCorrect;
        this.quizId = quizId;
        this.question = question;
        this.youtubeId = youtubeId;
        this.quizSetId = quizSetId;
        if(!selective)  //객관식일때만 options 넣기
            this.options = Arrays.asList(option1,option2,option3,option4);
        this.correctAnswer = correctAnswer;
        this.comment = comment;
        this.selective = selective;
        this.difficulty = difficulty;
        this.timestamp = timestamp;
    }
}
