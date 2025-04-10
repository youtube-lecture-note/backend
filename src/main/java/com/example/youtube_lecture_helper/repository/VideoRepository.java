package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video,Long> {
    Optional<Video> findByYoutubeId(String youtubeId);
}
