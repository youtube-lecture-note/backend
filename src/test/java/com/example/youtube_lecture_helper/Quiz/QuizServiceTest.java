package com.example.youtube_lecture_helper.Quiz;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.entity.QuizAttempt;
import com.example.youtube_lecture_helper.entity.QuizSet;
import com.example.youtube_lecture_helper.entity.User;
import com.example.youtube_lecture_helper.repository.QuizAttemptRepository;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.repository.QuizSetRepository;
import com.example.youtube_lecture_helper.repository.UserRepository;
import com.example.youtube_lecture_helper.service.QuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class QuizServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizSetRepository quizSetRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @InjectMocks
    private QuizService quizService;

    private User testUser;
    private List<Quiz> testQuizzes;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = new User();
        testUser.setId(1L);

        // 테스트용 퀴즈 목록 생성
        testQuizzes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Quiz quiz = new Quiz();
            quiz.setId((long) i);
            quiz.setQuestion("테스트 질문 " + i);
            quiz.setOption1("선택지 1");
            quiz.setOption2("선택지 2");
            quiz.setOption3("선택지 3");
            quiz.setOption4("선택지 4");
            quiz.setCorrectAnswer("1"); // 1, 2, 3, 4 중 하나
            quiz.setDifficulty((byte)2);
            quiz.setYoutubeId("test-youtube-id");
            testQuizzes.add(quiz);
        }
    }

    @Test
    void createQuizSetForUser_Success() {
        // Given
        Long userId = 1L;
        int difficulty = 2;
        String youtubeId = "test-youtube-id";
        int numberOfQuestions = 3;

        QuizSet savedQuizSet = new QuizSet();
        savedQuizSet.setId(1L);
        savedQuizSet.setUser(testUser);

        // Mock repository methods
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(quizRepository.findByDifficultyAndYoutubeId(difficulty, youtubeId)).thenReturn(testQuizzes);
        when(quizSetRepository.save(any(QuizSet.class))).thenReturn(savedQuizSet);

        // When
        QuizService.CreatedQuizSetDTO result = quizService.createQuizSetForUser(userId, difficulty, youtubeId, numberOfQuestions);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(quizRepository).findByDifficultyAndYoutubeId(difficulty, youtubeId);
        verify(quizSetRepository).save(any(QuizSet.class));
        verify(quizAttemptRepository).saveAll(anyList());
    }
}
