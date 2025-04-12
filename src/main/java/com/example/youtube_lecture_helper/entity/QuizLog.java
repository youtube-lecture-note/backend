package com.example.youtube_lecture_helper.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;

@Entity
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class QuizLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //private String youtubeId;     //quiz 필드에 youtube_id 포함되어 있음

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate solveDate;
    private String userAnswer;
    public QuizLog(Quiz quiz, User user, LocalDate solveDate, String userAnswer){
        this.quiz = quiz;
        this.user = user;
        this.solveDate = solveDate;
        this.userAnswer = userAnswer;
    }

}
