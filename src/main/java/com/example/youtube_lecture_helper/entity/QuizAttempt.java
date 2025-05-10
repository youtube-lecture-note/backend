package com.example.youtube_lecture_helper.entity;

import com.example.youtube_lecture_helper.dto.QuizAttemptDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_set_id")
    private QuizSet quizSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(name = "user_answer")
    private String userAnswer;

    @Column(name = "is_correct")
    private boolean isCorrect;

    public void setQuizSet(QuizSet quizSet){
        this.quizSet = quizSet;
    }
    public void setQuiz(Quiz quiz){
        this.quiz = quiz;
    }
}
