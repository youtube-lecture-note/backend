// package com.example.youtube_lecture_helper.Quiz;


// import com.example.youtube_lecture_helper.dto.QuizHistorySummaryDto;
// import com.example.youtube_lecture_helper.entity.*;
// import com.example.youtube_lecture_helper.dto.QuizAttemptDto;
// import com.example.youtube_lecture_helper.repository.*;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// import java.time.LocalDateTime;
// import java.util.List;

// import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest
// @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// class QuizAttemptRepositoryTest {

//     @Autowired
//     private QuizAttemptRepository quizAttemptRepository;

//     @Autowired
//     private QuizSetRepository quizSetRepository;

//     @Autowired
//     private QuizRepository quizRepository;

//     @Autowired
//     private UserRepository userRepository;

//     @Test
//     @DisplayName("사용자별 퀴즈셋 요약 통계 조회")
//     void testFindQuizSetSummariesByUserId() {
//         // --- Given (테스트 데이터 저장) ---
//         User user = new User();
//         user = userRepository.save(user);

//         QuizSet quizSet = new QuizSet();
//         quizSet.setUser(user);
//         quizSet.setAttemptTime(LocalDateTime.now());
//         quizSet = quizSetRepository.save(quizSet);

//         Quiz quiz = new Quiz();
//         quiz.setQuestion("What is 2+2?");
//         quiz.setYoutubeId("abc123");
//         quiz = quizRepository.save(quiz);

//         QuizAttempt attempt = new QuizAttempt();
//         attempt.setQuizSet(quizSet);
//         attempt.setQuiz(quiz);
//         attempt.setUserAnswer("4");
//         attempt.setCorrect(true);
//         quizAttemptRepository.save(attempt);

//         // --- When ---
//         List<QuizHistorySummaryDto> summaries = quizAttemptRepository.findQuizSetSummariesByUserId(user.getId());

//         // --- Then ---
//         assertThat(summaries).isNotEmpty();
//         QuizHistorySummaryDto summary = summaries.get(0);
//         assertThat(summary.getAttemptId()).isEqualTo(quizSet.getId());
//         assertThat(summary.getTotalQuizzes()).isEqualTo(1L);
//         assertThat(summary.getWrongCount()).isEqualTo(0L);
//     }

//     @Test
//     @DisplayName("특정 QuizSet의 QuizAttempt DTO 리스트 조회")
//     void testFindDetailedAttemptDTOsByQuizSetId() {
//         // --- Given ---
//         User user = new User();
//         user = userRepository.save(user);

//         QuizSet quizSet = new QuizSet();
//         quizSet.setUser(user);
//         quizSet.setAttemptTime(LocalDateTime.now());
//         quizSet = quizSetRepository.save(quizSet);

//         Quiz quiz = new Quiz();
//         quiz.setQuestion("What is 3+3?");
//         quiz.setYoutubeId("def456");
//         quiz = quizRepository.save(quiz);

//         QuizAttempt attempt = new QuizAttempt();
//         attempt.setQuizSet(quizSet);
//         attempt.setQuiz(quiz);
//         attempt.setUserAnswer("6");
//         attempt.setCorrect(true);
//         quizAttemptRepository.save(attempt);

//         // --- When ---
//         List<QuizAttemptDto> dtos = quizAttemptRepository.findDetailedAttemptDTOsByQuizSetId(quizSet.getId());

//         // --- Then ---
//         assertThat(dtos).hasSize(1);
//         QuizAttemptDto dto = dtos.get(0);
//         assertThat(dto.getUserAnswer()).isEqualTo("6");
//         assertThat(dto.isCorrect()).isTrue();
//         assertThat(dto.getQuizId()).isEqualTo(quiz.getId());
//         assertThat(dto.getQuizSetId()).isEqualTo(quizSet.getId());
//     }
// }
