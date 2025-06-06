package com.example.youtube_lecture_helper.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizWithAnswerDto {
    private Long id;
    private String question;
    private boolean selective;
    private List<String> options;
    private String correctAnswer;
    private String comment;
}