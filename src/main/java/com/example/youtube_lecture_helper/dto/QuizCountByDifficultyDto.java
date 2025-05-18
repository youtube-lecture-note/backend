package com.example.youtube_lecture_helper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
public class QuizCountByDifficultyDto {
    private Byte difficulty;
    private Long count;
    QuizCountByDifficultyDto(Byte difficulty, Long count){
        this.difficulty = difficulty;
        this.count = count;
    }
}
