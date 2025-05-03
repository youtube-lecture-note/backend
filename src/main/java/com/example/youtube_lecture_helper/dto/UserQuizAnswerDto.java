package com.example.youtube_lecture_helper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class UserQuizAnswerDto {
    private long quizId;
    private String userAnswer;
    public long getQuizId(){
        return quizId;
    }
}
