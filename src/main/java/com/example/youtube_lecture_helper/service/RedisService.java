package com.example.youtube_lecture_helper.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final int KEY_LENGTH = 6;
    private static final long TTL_SECONDS = 600; // 10분

    // 키 생성
    public String generateQuizKey(Long quizSetId) {
        String key;
        do {
            key = generateRandomNumericKey();
        } while (redisTemplate.hasKey("quizkey:" + key)); // 충돌 방지

        redisTemplate.opsForValue().set("quizkey:" + key, quizSetId.toString(), Duration.ofSeconds(TTL_SECONDS));
        return key;
    }

    // 키 검증
    public Optional<Long> resolveQuizSetId(String key) {
        String value = redisTemplate.opsForValue().get("quizkey:" + key);
        return value != null ? Optional.of(Long.parseLong(value)) : Optional.empty();
    }

    private String generateRandomNumericKey() {
        int number = new Random().nextInt(1_000_000); // 0 ~ 999999
        return String.format("%06d", number);
    }
}