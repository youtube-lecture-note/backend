package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.openai_api.Quiz;

import java.util.List;
import java.util.Optional;

public interface SummaryRepository {
    String findByVideoId(String videoId);
    String save(String videoId, String summary);

}
