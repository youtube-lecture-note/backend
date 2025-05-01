package com.example.youtube_lecture_helper.dto;

import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.entity.QuizAttempt;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuizAttemptDto {
    private Long attemptId;
    private String userAnswer;
    private boolean isCorrect;
    private Long quizId;
    private String questionText; // Quiz의 질문 내용
    // 필요하다면 다른 Quiz 필드 (보기, youtubeId 등) 추가
    private String youtubeId;
    private Long quizSetId; // 어떤 퀴즈 세트인지 식별자 추가

    // JPQL 생성자 표현식에서 사용할 생성자
    public QuizAttemptDto(Long attemptId, String userAnswer, boolean isCorrect, Long quizId, String questionText, String youtubeId, Long quizSetId) {
        this.attemptId = attemptId;
        this.userAnswer = userAnswer;
        this.isCorrect = isCorrect;
        this.quizId = quizId;
        this.questionText = questionText;
        this.youtubeId = youtubeId; // Quiz 엔티티에 youtubeId 필드가 있다고 가정
        this.quizSetId = quizSetId;
    }

    // Getters (Lombok @Getter 사용 가능)
    public Long getAttemptId() { return attemptId; }
    public String getUserAnswer() { return userAnswer; }
    public boolean isCorrect() { return isCorrect; }
    public Long getQuizId() { return quizId; }
    public String getQuestionText() { return questionText; }
    public String getYoutubeId() { return youtubeId; }
    public Long getQuizSetId() { return quizSetId; }
}
