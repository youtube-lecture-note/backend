package org.example;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class Quiz {
    private String question;
    private List<String> options;
    private String correctAnswer;
    private int timestamp;
    private String subjectiveAnswer; //주관식 응답

    public Quiz(String question, List<String> options, String correctAnswer, int timestamp) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.timestamp = timestamp;
    }
    public Quiz(String question, String subjectiveAnswer, int timestamp) {
        this.question = question;
        this.subjectiveAnswer = subjectiveAnswer;
        this.timestamp = timestamp;
    }

}
