package com.example.youtube_lecture_helper.openai_api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class YoutubeSubtitleExtractorTest {

    @Test
    public void testGetSubtitles() {
        String videoId = "66rCiPTNagA"; // 실제 테스트용 유튜브 영상 ID를 입력하세요 (자막이 있는 영상)
        String language = "ko"; // 또는 "en"

        try {
            List<List<SubtitleLine>> subtitles = YoutubeSubtitleExtractor.getSubtitles(videoId, language);

            assertNotNull(subtitles, "Subtitles should not be null");
            assertFalse(subtitles.isEmpty(), "Subtitles list should not be empty");

            // 예시로 첫 청크, 첫 라인의 출력
            List<SubtitleLine> firstChunk = subtitles.get(0);
            assertFalse(firstChunk.isEmpty(), "First subtitle chunk should not be empty");

            SubtitleLine firstLine = firstChunk.get(0);
            System.out.println("Start time: " + firstLine.getStart() + "s");
            System.out.println("Text: " + firstLine.getText());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurred during subtitle extraction: " + e.getMessage());
        }
    }
}
