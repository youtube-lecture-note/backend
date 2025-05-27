package com.example.youtube_lecture_helper.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@Entity
public class QuizSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private boolean isMultiVideo;

    @Column(name = "attempt_time")
    private LocalDateTime attemptTime;
    public void setUser(User user){
        this.user = user;
    }
    public void setAttemptTime(LocalDateTime attemptTime){
        this.attemptTime = attemptTime;
    }
    @OneToMany(mappedBy = "quizSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttempt> attempts = new ArrayList<>();
    public void addQuizAttempt(QuizAttempt attempt) {
        attempts.add(attempt);
        attempt.setQuizSet(this);
    }

}
