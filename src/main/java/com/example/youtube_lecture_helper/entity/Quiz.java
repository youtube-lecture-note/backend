package com.example.youtube_lecture_helper.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Getter
@Entity
@NoArgsConstructor
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//  @ManyToOne(fetch = FetchType.LAZY)
//  @JoinColumn(name = "video_id")
//  private Video video;
    private String youtubeId;   //video와 별도로 일관성 유지 필요
    private String question;

    private boolean selective;  //객관식 문제인지?

    private byte difficulty;    // 1=easy, 2=normal, 3=hard

    private String option1;
    private String option2;
    private String option3;
    private String option4;

    private String correctAnswer;
    private int timestamp;
    private String comment; //설명

    public Quiz(String videoId, byte difficulty, String question, List<String> options, String correctAnswer, String comment, int timestamp) {
        this.youtubeId = videoId;
        this.question = question;

        if(options!=null && options.size()==4){
            this.option1 = options.get(0);
            this.option2 = options.get(1);
            this.option3 = options.get(2);
            this.option4 = options.get(3);
            this.selective = true;
        }else{
            this.selective = false;
        }

        this.correctAnswer = correctAnswer;
        this.timestamp = timestamp;
        this.comment = comment;
        this.difficulty = difficulty;

    }
    //주관식 문제: 기존 정답을 기반으로 채점 시 gpt에 요청하기
    public Quiz(String videoId, String question, String correctAnswer, int timestamp) {
        this.youtubeId = videoId;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.timestamp = timestamp;
    }

    public Quiz(Long quizId){
        this.id = quizId;
    }
    public List<String> getOptions() {
        return Stream.of(option1, option2, option3, option4)
                .filter(Objects::nonNull)  // null 값 제외
                .toList();
    }
}
