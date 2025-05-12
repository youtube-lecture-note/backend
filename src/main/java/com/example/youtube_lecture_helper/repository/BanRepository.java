package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.Ban;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BanRepository extends JpaRepository<Ban, String> {
    Optional<Ban> findByYoutubeId(String youtubeId);
}
