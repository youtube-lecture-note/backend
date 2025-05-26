package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.QuizCountByDifficultyDto;
import com.example.youtube_lecture_helper.dto.QuizCountDto;
import com.example.youtube_lecture_helper.entity.QuizAttempt;
import com.example.youtube_lecture_helper.entity.QuizSet;
import com.example.youtube_lecture_helper.entity.User;
import com.example.youtube_lecture_helper.exception.QuizNotFoundException;
import com.example.youtube_lecture_helper.exception.UserNotFoundException;
import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.repository.QuizAttemptRepository;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.repository.QuizSetRepository;
import com.example.youtube_lecture_helper.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizSetRepository quizSetRepository;
    private final UserRepository userRepository;

//    public QuizService(OpenAIGptClient gptClient, QuizRepository quizRepository, QuizLogService quizLogService){
//        this.gptClient = gptClient;
//        this.quizRepository = quizRepository;
//        this.quizLogService = quizLogService;
//    }
    public QuizCountDto getQuizCountByYoutubeId(String youtubeId){
        List<QuizCountByDifficultyDto> quizCountByDifficultyDtos = quizRepository.countQuizzesByDifficultyAndYoutubeId(youtubeId);
        long level1 = 0L, level2 = 0L, level3 = 0L;
        for(QuizCountByDifficultyDto dto: quizCountByDifficultyDtos){
            switch(dto.getDifficulty()){
                case 1->level1 = dto.getCount();
                case 2->level2 = dto.getCount();
                case 3->level3 = dto.getCount();
            }
        }
        return new QuizCountDto(level1,level2,level3);
    }

    @Transactional // Ensure all DB operations succeed or fail together
    public CreatedQuizSetDTO createQuizSetForUser(Long userId, int difficulty, String youtubeId, int numberOfQuestions, boolean isForMultiUsers) {

        // 1. Fetch the User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId)); // Example exception

        // 2. Fetch Candidate Quizzes
        List<Quiz> candidateQuizzes = quizRepository.findByDifficultyAndYoutubeId(difficulty, youtubeId);

        // 3. Check if enough quizzes are available
        if (candidateQuizzes.isEmpty()) {
            throw new QuizNotFoundException("No quizzes found for the given criteria."); // Example exception
        }
        if (candidateQuizzes.size() < numberOfQuestions) {
            // Decide: Throw an error or just use all available quizzes?
            // Let's use all available for this example. Adjust if needed.
            numberOfQuestions = candidateQuizzes.size();
            // Or: throw new InsufficientQuizzesException("Not enough quizzes found. Required: " + numberOfQuestions + ", Found: " + candidateQuizzes.size());
        }

        // 4. Select Randomly
        Collections.shuffle(candidateQuizzes); // Shuffle the list in place
        List<Quiz> selectedQuizzes = candidateQuizzes.subList(0, numberOfQuestions);

        // 5. Create Quiz Set
        QuizSet quizSet = new QuizSet();
        quizSet.setUser(user);
        quizSet.setAttemptTime(LocalDateTime.now());
        QuizSet savedQuizSet = quizSetRepository.save(quizSet); // Save to get the ID

        // 6. Create Quiz Attempts for each selected quiz
        List<QuizAttempt> quizAttempts = selectedQuizzes.stream().map(quiz -> {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setQuizSet(savedQuizSet);
            attempt.setQuiz(quiz);
            // userAnswer and isCorrect are initially null/false by default
            return attempt;
        }).collect(Collectors.toList());

        quizAttemptRepository.saveAll(quizAttempts); // Save all attempts (often more efficient)

        // 7. Prepare and Return Response DTO (important!)
        //    Return only the necessary info (quiz questions, options, quizSetId)
        //    DO NOT return the Quiz entities directly if they contain the answers.
        List<QuizQuestionDTO> questionDTOs = selectedQuizzes.stream()
                .map(this::mapToQuizQuestionDTO) // Helper method to map Quiz -> QuizQuestionDTO
                .collect(Collectors.toList());

        return new CreatedQuizSetDTO(savedQuizSet.getId(), questionDTOs);
    }

    //users에게 문제 뿌림, 문제 풀기 위해 무언가 필요할듯? redis 사용하는것도 괜찮을듯?



    private QuizQuestionDTO mapToQuizQuestionDTO(Quiz quiz) {
        // Assuming Quiz has methods like getId(), getQuestion(), getOptions()
        // Assuming QuizQuestionDTO has corresponding fields/constructor
        return new QuizQuestionDTO(quiz.getId(), quiz.getQuestion(), quiz.getOptions());
    }
    public static class CreatedQuizSetDTO {
        private Long quizSetId;
        private List<QuizQuestionDTO> questions;
        // Constructor, Getters
        public CreatedQuizSetDTO(Long quizSetId, List<QuizQuestionDTO> questions) {
            this.quizSetId = quizSetId;
            this.questions = questions;
        }
        // getters...
        public Long getQuizSetId() { return quizSetId; }
        public List<QuizQuestionDTO> getQuestions() { return questions; }
    }


    public static class QuizQuestionDTO {
        private Long quizId;
        private String question;
        private List<String> options;
        // Constructor, Getters
        public QuizQuestionDTO(Long quizId, String question, List<String> options) {
            this.quizId = quizId;
            this.question = question;
            this.options = options;
        }
        public Long getQuizId() { return quizId; }
        public String getQuestion() { return question; }
        public List<String> getOptions() { return options; }
    }

}
