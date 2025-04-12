package com.example.youtube_lecture_helper.entity;
import com.example.youtube_lecture_helper.openai_api.QuizType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @Enumerated(EnumType.STRING)
    private QuizType quizType;

    private String option1;
    private String option2;
    private String option3;
    private String option4;

    private String correctAnswer;
    private int timestamp;
    private String comment; //설명

    public Quiz(String videoId, String question, List<String> options, String correctAnswer, String comment, int timestamp) {
        this.youtubeId = videoId;
        this.question = question;

        if(options!=null && options.size()==4){
            this.option1 = options.get(0);
            this.option2 = options.get(1);
            this.option3 = options.get(2);
            this.option4 = options.get(3);
            this.quizType = QuizType.MULTIPLE_CHOICE;
        }else{
            this.quizType = QuizType.SHORT_ANSWER;
        }

        this.correctAnswer = correctAnswer;
        this.timestamp = timestamp;
        this.comment = comment;
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
}
