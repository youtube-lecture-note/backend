package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.*;
import com.example.youtube_lecture_helper.entity.*;
import com.example.youtube_lecture_helper.exception.*;
import com.example.youtube_lecture_helper.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizRepository;
    private final VideoRepository videoRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizSetRepository quizSetRepository;
    private final QuizSetMultiRepository quizSetMultiRepository;
    private final UserRepository userRepository;
    private final UserVideoCategoryRepository userVideoCategoryRepository;

    private final RedisService redisService;

    public QuizCountDto getQuizCountByYoutubeId(String youtubeId, Long userId, boolean isRemaining) {
        List<QuizCountByDifficultyDto> quizCountByDifficultyDtos;
        if (isRemaining) {
            quizCountByDifficultyDtos = quizRepository.countQuizzesRemainingByDifficultyAndYoutubeId(youtubeId, userId);
        } else {
            quizCountByDifficultyDtos = quizRepository.countQuizzesByDifficultyAndYoutubeId(youtubeId);
        }
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
                attempt.setUser(user);
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
    //난이도별 개수 다르게 생성
    @Transactional
    public CreatedQuizSetDTO createQuizSetForUserByCounts(
            Long userId,
            String youtubeId,
            int level1Count,
            int level2Count,
            int level3Count,
            boolean isForMultiUsers,
            String name,
            boolean onlyUnsolvedQuizzes
    ) {
        // 1. Fetch the User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        List<Quiz> level1Quizzes;
        List<Quiz> level2Quizzes;
        List<Quiz> level3Quizzes;
        
        //List<Quiz> 모두 가져온 뒤 랜덤하게 개수 select 하기
        // 2. Fetch quizzes by difficulty & unsolved quizzes if required
        if(onlyUnsolvedQuizzes){
            level1Quizzes = quizRepository.findUnsolvedQuizzesByDifficultyAndYoutubeIdNative(1, youtubeId, userId);
            level2Quizzes = quizRepository.findUnsolvedQuizzesByDifficultyAndYoutubeIdNative(2, youtubeId, userId);
            level3Quizzes = quizRepository.findUnsolvedQuizzesByDifficultyAndYoutubeIdNative(3, youtubeId, userId);

        }else{  //중복 허용
            level1Quizzes = quizRepository.findByDifficultyAndYoutubeId(1, youtubeId);
            level2Quizzes = quizRepository.findByDifficultyAndYoutubeId(2, youtubeId);
            level3Quizzes = quizRepository.findByDifficultyAndYoutubeId(3, youtubeId);
        }

        // 3. Check if enough quizzes are available for each level
        if (level1Quizzes.size() < level1Count) {
            throw new InsufficientQuizzesException("쉬움 난이도 문제 부족: " + level1Quizzes.size() + "개 있음, " + level1Count + "개 필요");
        }
        if (level2Quizzes.size() < level2Count) {
            throw new InsufficientQuizzesException("보통 난이도 문제 부족: " + level2Quizzes.size() + "개 있음, " + level2Count + "개 필요");
        }
        if (level3Quizzes.size() < level3Count) {
            throw new InsufficientQuizzesException("어려움 난이도 문제 부족: " + level3Quizzes.size() + "개 있음, " + level3Count + "개 필요");
        }

        // 4. Select randomly for each level
        Collections.shuffle(level1Quizzes);
        Collections.shuffle(level2Quizzes);
        Collections.shuffle(level3Quizzes);

        List<Quiz> selectedQuizzes = new ArrayList<>();
        selectedQuizzes.addAll(level1Quizzes.subList(0, level1Count));
        selectedQuizzes.addAll(level2Quizzes.subList(0, level2Count));
        selectedQuizzes.addAll(level3Quizzes.subList(0, level3Count));

        // 5. Shuffle the combined list for randomness
        Collections.shuffle(selectedQuizzes);

        // 6. Create Quiz Set
        QuizSet quizSet = new QuizSet();
        quizSet.setUser(user);
        quizSet.setMultiVideo(false);
        quizSet.setName(name);
        quizSet.setAttemptTime(LocalDateTime.now());
        QuizSet savedQuizSet = quizSetRepository.save(quizSet);

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

        // 7. Create Quiz Attempts
        List<QuizAttempt> quizAttempts = selectedQuizzes.stream().map(quiz -> {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setQuizSet(savedQuizSet);
            attempt.setQuiz(quiz);
            attempt.setUser(user);
            return attempt;
        }).toList();

        quizAttemptRepository.saveAll(quizAttempts);

        // 8. Prepare and Return Response DTO
        List<QuizQuestionDTO> questionDTOs = selectedQuizzes.stream()
                .map(this::mapToQuizQuestionDTO)
                .toList();

        return new CreatedQuizSetDTO(savedQuizSet.getId(), questionDTOs);
    }

    //뿌린 quizSetKey로 퀴즈셋 접근해서 quizAttempt에 만들기
    //캐시 적용 비교해보기
    @Transactional
    public CreatedQuizSetDTO getQuizSetQuizzesByRedisQuizSetKey(Long userId, String redisKey){
        Long quizSetId = redisService.resolveQuizSetId(redisKey).orElseThrow(()-> new KeyNotFoundException("Wrong Redis Key"));
        
        boolean alreadyAttempted = quizAttemptRepository.existsByUserIdAndQuizSetId(userId, quizSetId);
        if (alreadyAttempted) {
            throw new QuizAlreadyAttemptedException("이미 시도한 퀴즈셋입니다. 다시 풀 수 없습니다.");
        }
        
        //N+1
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
        
        Video video = videoRepository.findByYoutubeId(selectedQuizzes.get(0).getYoutubeId())
            .orElseThrow(() -> new RuntimeException("Video not found"));

        Optional<UserVideoCategory> uvcOpt = userVideoCategoryRepository.findByUserIdAndVideoId(userId, video.getId());
        if (uvcOpt.isEmpty()) {
            Category category = new Category();
            category.setId(1L);  //default category로 저장
            String userVideoName = video.getYoutubeId(); // 또는 원하는 이름
            UserVideoCategory uvc = new UserVideoCategory(user, video, category, userVideoName);
            userVideoCategoryRepository.save(uvc);
        }

        return new CreatedQuizSetDTO(quizSetId, questionDTOs);
    }

    public boolean isQuizSetCreator(Long quizSetId, Long userId) {
        return quizSetRepository.existsByIdAndUserId(quizSetId, userId);
    }
    
    public QuizSetResultsDto getQuizSetResultsMulti(Long quizSetId) {
        // 1. 총 문제 수 조회
        Long totalQuizCount = quizSetMultiRepository.countQuizzesByQuizSetId(quizSetId);
        
        // 2. 참여자별 결과 (중복 제거됨)
        List<ParticipantResultDto> participantResults = quizAttemptRepository
                .findParticipantResultsByQuizSetId(quizSetId);
        
        // 3. 문항별 정답률 통계
        List<QuizStatisticsDto> quizStatistics = quizAttemptRepository
                .findQuizStatisticsByQuizSetId(quizSetId);
        
        return QuizSetResultsDto.builder()
                .quizSetId(quizSetId)
                .totalQuizCount(totalQuizCount.intValue())
                .participantCount(participantResults.size())
                .participantResults(participantResults)
                .quizStatistics(quizStatistics)
                .build();
    }


    // 특정 유저가 제작한 멀티 퀴즈셋 가져오기
    public List<QuizSetSummaryDto> getMultiQuizSetsByUser(Long userId) {
    List<QuizSet> quizSets = quizSetRepository.findMultiQuizSetsByUserId(userId);
    
    return quizSets.stream()
            .map(qs -> new QuizSetSummaryDto(qs.getId(), qs.getName(),qs.getAttemptTime()))
            .toList();
    }

    // 특정 퀴즈셋의 퀴즈를 모두 가져오기
    public List<QuizWithAnswerDto> getAllQuizzesInSet(Long quizSetId, Long userId) {
        if (!isQuizSetCreator(quizSetId, userId)) {
            throw new AccessDeniedException("해당 퀴즈셋의 퀴즈를 조회할 권한이 없습니다.");
        }
        List<Quiz> quizzes = quizSetMultiRepository.findQuizzesByQuizSetId(quizSetId);
        return quizzes.stream()
                .map(this::convertToQuizWithAnswerDto)
                .toList();
    }

    //helper method to convert Quiz to QuizWithAnswerDto (답안 포함)
    private QuizWithAnswerDto convertToQuizWithAnswerDto(Quiz quiz) {
        QuizWithAnswerDto dto = new QuizWithAnswerDto();
        dto.setId(quiz.getId());
        dto.setQuestion(quiz.getQuestion());
        dto.setSelective(quiz.isSelective());
        dto.setCorrectAnswer(quiz.getCorrectAnswer());
        dto.setComment(quiz.getComment());
        
        if (quiz.isSelective()) {
            dto.setOptions(quiz.getOptions());
        }
        return dto;
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
