package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.QuizCountByDifficultyDto;
import com.example.youtube_lecture_helper.dto.QuizCountDto;
import com.example.youtube_lecture_helper.entity.*;
import com.example.youtube_lecture_helper.exception.KeyNotFoundException;
import com.example.youtube_lecture_helper.exception.QuizNotFoundException;
import com.example.youtube_lecture_helper.exception.QuizSetNotFoundException;
import com.example.youtube_lecture_helper.exception.UserNotFoundException;
import com.example.youtube_lecture_helper.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
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
    private final QuizSetMultiRepository quizSetMultiRepository;
    private final UserRepository userRepository;

    private final RedisService redisService;

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
        quizSet.setMultiVideo(false);
        quizSet.setAttemptTime(LocalDateTime.now());
        QuizSet savedQuizSet = quizSetRepository.save(quizSet); // Save to get the ID

        if(isForMultiUsers){
            String quizSetkey = redisService.generateQuizKey(savedQuizSet.getId()); //redis로 key 만들어서 저장
            List<QuizSetMulti> quizSetMultiList = selectedQuizzes.stream()  //selected quiz로 quizSetMulti에 저장
                    .map(quiz -> {
                        QuizSetMulti quizSetMulti = new QuizSetMulti();
                        quizSetMulti.setQuizSet(savedQuizSet);
                        quizSetMulti.setQuiz(quiz);
                        return quizSetMulti;
                    })
                    .toList();
            quizSetMultiRepository.saveAll(quizSetMultiList);
            return new CreatedQuizSetDTO(savedQuizSet.getId(), null, quizSetkey);
        }

        else{
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
                    .toList();

            return new CreatedQuizSetDTO(savedQuizSet.getId(), questionDTOs);
        }
    }

    //뿌린 quizSetKey로 퀴즈셋 접근해서 quizAttempt에 만들기
    //캐시 적용 비교해보기
    @Transactional
    public CreatedQuizSetDTO getQuizSetQuizzesByRedisQuizSetKey(Long userId, String redisKey){
        Long quizSetId = redisService.resolveQuizSetId(redisKey).orElseThrow(()-> new KeyNotFoundException("Wrong Redis Key"));
        List<QuizSetMulti> quizSetMultiList = quizSetMultiRepository.findByQuizSetId(quizSetId);
        QuizSet quizSet = quizSetRepository.findById(quizSetId)
                .orElseThrow(() -> new QuizSetNotFoundException("QuizSet not found"));

        List<Quiz> selectedQuizzes = quizSetMultiList.stream()
                .map(QuizSetMulti::getQuiz)
                .toList();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));   //user

        List<QuizAttempt> quizAttempts = selectedQuizzes.stream()
                .map(quiz -> {
                    QuizAttempt attempt = new QuizAttempt();
                    attempt.setQuizSet(quizSet);
                    attempt.setQuiz(quiz);
                    attempt.setUser(user); // 사용자 설정 (QuizAttempt에 user 필드가 있다고 가정)
                    // userAnswer and isCorrect are initially null/false by default
                    return attempt;
                })
                .toList();
        quizAttemptRepository.saveAll(quizAttempts);
        //quiz->quizdto(answer 제거)
        List<QuizQuestionDTO> questionDTOs = selectedQuizzes.stream()
                .map(this::mapToQuizQuestionDTO)
                .toList();

        return new CreatedQuizSetDTO(quizSetId, questionDTOs);
    }

    private QuizQuestionDTO mapToQuizQuestionDTO(Quiz quiz) {
        // Assuming Quiz has methods like getId(), getQuestion(), getOptions()
        // Assuming QuizQuestionDTO has corresponding fields/constructor
        return new QuizQuestionDTO(quiz.getId(), quiz.getQuestion(), quiz.getOptions());
    }
    public static class CreatedQuizSetDTO {
        private Long quizSetId;
        private List<QuizQuestionDTO> questions;
        private String redisQuizSetKey;
        // Constructor, Getters
        public CreatedQuizSetDTO(Long quizSetId, List<QuizQuestionDTO> questions) {
            this.quizSetId = quizSetId;
            this.questions = questions;
        }
        public CreatedQuizSetDTO(Long quizSetId, List<QuizQuestionDTO> questions,String redisQuizSetKey) {
            this.quizSetId = quizSetId;
            this.questions = questions;
            this.redisQuizSetKey = redisQuizSetKey;
        }
        // getters...
        public Long getQuizSetId() { return quizSetId; }
        public List<QuizQuestionDTO> getQuestions() { return questions; }
        public String getRedisQuizSetKey(){return redisQuizSetKey;}
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
