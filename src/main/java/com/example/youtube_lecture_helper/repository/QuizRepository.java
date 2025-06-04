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

    @Query("SELECT new com.example.youtube_lecture_helper.dto.QuizCountByDifficultyDto(q.difficulty, COUNT(q)) " +
            "FROM Quiz q " +
            "WHERE q.youtubeId = :youtubeId " +
            "GROUP BY q.difficulty")
    List<QuizCountByDifficultyDto> countQuizzesByDifficultyAndYoutubeId(@Param("youtubeId") String youtubeId);

    @Query("SELECT q FROM Quiz q WHERE q.id IN :ids")
    List<Quiz> findAllByIdIn(@Param("ids") List<Long> ids);

    List<Quiz> findAllByYoutubeId(String youtubeId);
}
