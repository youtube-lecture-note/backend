package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.Quiz;
import com.example.youtube_lecture_helper.repository.projection.QuizId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz,Long> {
    List<QuizId> findByYoutubeId(String youtubeId);
}
