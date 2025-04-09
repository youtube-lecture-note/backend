package com.example.youtube_lecture_helper.openai_api;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class Quiz {
    private String videoId;
    private String question;
    private List<String> options;
    private String correctAnswer;
    private int timestamp;
    private String comment; //설명

    public Quiz(String videoId, String question, List<String> options, String correctAnswer, String comment, int timestamp) {
        this.videoId = videoId;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.timestamp = timestamp;
        this.comment = comment;
    }
    //주관식 문제: 기존 정답을 기반으로 채점 시 gpt에 요청하기
    public Quiz(String videoId, String question, String correctAnswer, int timestamp) {
        this.videoId = videoId;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.timestamp = timestamp;
    }

}
