package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.QuizLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizLogRepository extends JpaRepository<QuizLog,Long> {
}
