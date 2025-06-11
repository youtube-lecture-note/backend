package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.dto.QuizCountByDifficultyDto;
import com.example.youtube_lecture_helper.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz,Long> {
    List<Quiz> findByYoutubeId(String youtubeId);
    @Query(value = "SELECT * FROM quiz q WHERE q.difficulty = :difficulty AND q.youtube_id = :youtubeId", nativeQuery = true)
    List<Quiz> findByDifficultyAndYoutubeId(int difficulty, String youtubeId);

    @Query(value = "SELECT q.* FROM quiz q " +
               "LEFT JOIN quiz_attempt qa ON q.id = qa.quiz_id AND qa.user_id = :userId " +
               "WHERE q.youtube_id = :youtubeId " +
               "AND q.difficulty = :difficulty " +
               "AND qa.id IS NULL", 
       nativeQuery = true)
    List<Quiz> findUnsolvedQuizzesByDifficultyAndYoutubeIdNative(
        @Param("difficulty") int difficulty,
        @Param("youtubeId") String youtubeId,
        @Param("userId") Long userId
    );

    @Query("SELECT new com.example.youtube_lecture_helper.dto.QuizCountByDifficultyDto(q.difficulty, COUNT(q)) " +
            "FROM Quiz q " +
            "WHERE q.youtubeId = :youtubeId " +
            "GROUP BY q.difficulty")
    List<QuizCountByDifficultyDto> countQuizzesByDifficultyAndYoutubeId(@Param("youtubeId") String youtubeId);

    @Query("SELECT new com.example.youtube_lecture_helper.dto.QuizCountByDifficultyDto(q.difficulty, COUNT(q)) " +
       "FROM Quiz q " +
       "LEFT JOIN QuizAttempt qa ON q.id = qa.quiz.id AND qa.user.id = :userId " +  //left join이 not in보다 효율적
       "WHERE q.youtubeId = :youtubeId AND qa.id IS NULL " +
       "GROUP BY q.difficulty")
    List<QuizCountByDifficultyDto> countQuizzesRemainingByDifficultyAndYoutubeId(
        @Param("youtubeId") String youtubeId, 
        @Param("userId") Long userId
    );


    @Query("SELECT q FROM Quiz q WHERE q.id IN :ids")
    List<Quiz> findAllByIdIn(@Param("ids") List<Long> ids);

    List<Quiz> findAllByYoutubeId(String youtubeId);
}
