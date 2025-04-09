package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.openai_api.Quiz;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class DummySummaryRepository implements SummaryRepository{
    private final Map<String, String> summaryMap = new HashMap<>();
    @Override
    public String findByVideoId(String videoId) {
        return summaryMap.get(videoId);
    }
    @Override
    public String save(String videoId, String summary){
        summaryMap.put(videoId,summary);
        return summary;
    }
}
