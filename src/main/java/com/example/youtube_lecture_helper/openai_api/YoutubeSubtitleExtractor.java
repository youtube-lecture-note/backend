package com.example.youtube_lecture_helper.openai_api;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
// Removed regex Pattern and Matcher as they are not directly needed for API call
// unless you need to parse something else.

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
// import org.springframework.beans.factory.annotation.Autowired; // Not used
// import org.springframework.core.env.Environment; // Not used

@Component
public class YoutubeSubtitleExtractor {

    @Value("${transcript.api.url}") // Updated property name
    private String transcriptApiBaseUrl;

    // Re-usable HttpClient
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .cookieHandler(new CookieManager()) // May not be needed for calling your Python API
            .build();

    // getYouTubeTitle can remain as is, it's a separate functionality
    public static String getYouTubeTitle(String youtubeId) {
        String url = "https://www.youtube.com/watch?v=" + youtubeId;
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();
            String title = doc.title();
            if (title.endsWith(" - YouTube")) {
                title = title.replaceFirst(" - YouTube$", "");
            }
            return title;
        } catch (Exception e) {
            System.err.println("Error fetching YouTube title for ID " + youtubeId + ": " + e.getMessage());
            // e.printStackTrace(); // Consider logging framework instead
            return "Error fetching title.";
        }
    }

    // This method now calls your Python API
    private String fetchTranscriptFromApi(String videoID, String lang) throws IOException, InterruptedException {
        String apiUrl = transcriptApiBaseUrl + "/transcript?video_id=" + videoID;
        System.out.println("DEBUG: Fetching transcript from API URL: " + apiUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/json") // Expecting JSON response
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int statusCode = response.statusCode();
        String responseBody = response.body();
        System.out.println("DEBUG: Received HTTP Status Code from transcript API: " + statusCode);

        if (statusCode < 200 || statusCode >= 300) {
            System.err.println("ERROR: Transcript API request failed with status code: " + statusCode + " for video ID: " + videoID);
            System.err.println("ERROR: Response Body: " + responseBody);
            throw new IOException("Transcript API request failed with status code: " + statusCode + ". Body: " + responseBody);
        }
        return responseBody;
    }

    public List<List<SubtitleLine>> getSubtitles(String videoID, String lang) throws IOException, InterruptedException {
        if (lang == null || lang.trim().isEmpty()) {
            lang = "ko"; // Default language
        }

        String transcriptJsonString = fetchTranscriptFromApi(videoID, lang);

        List<SubtitleLine> allLines = new ArrayList<>();
        try {
            JSONArray transcriptArray = new JSONArray(transcriptJsonString);
            for (int i = 0; i < transcriptArray.length(); i++) {
                JSONObject entry = transcriptArray.getJSONObject(i);
                // The Python API now directly gives integer 'start' and 'text'
                int start = entry.getInt("start"); // Already an int from Python API
                String text = entry.getString("text");
                allLines.add(new SubtitleLine(start, text));
            }
        } catch (JSONException e) {
            System.err.println("Error parsing JSON response from transcript API: " + e.getMessage());
            System.err.println("Received JSON String: " + transcriptJsonString);
            throw new RuntimeException("Error parsing JSON from transcript API: " + e.getMessage(), e);
        }

        if (allLines.isEmpty()) {
            // This case might happen if the API returns an empty list for a 2xx response,
            // though the Python API should return 404 if no transcript.
            System.out.println("No subtitle lines found for video " + videoID + " even after API call.");
            // Decide how to handle: throw exception or return empty list of lists
            throw new RuntimeException("No subtitles found for video: " + videoID + " (API returned empty list)");
        }

        // The chunking logic from your original parseTranscript can now be applied directly
        return chunkSubtitles(allLines);
    }

    // Renamed and refactored original parseTranscript to only do chunking
    private List<List<SubtitleLine>> chunkSubtitles(List<SubtitleLine> allLines) {
        List<List<SubtitleLine>> chunkedSubtitles = new ArrayList<>();
        if (allLines.isEmpty()) {
            return chunkedSubtitles; // Return empty list of lists if no lines
        }

        int lastTimestamp = 0;
        for (SubtitleLine line : allLines) {
            if (line.getStart() > lastTimestamp) {
                lastTimestamp = line.getStart();
            }
        }

        int totalDuration = lastTimestamp + 5; // Add a small buffer
        int baseChunkSize = 600;        // Default 10 minutes
        int minLastChunkSize = 180;     // Minimum 3 minutes for the last chunk

        int numFullChunks = totalDuration / baseChunkSize;
        int remainingDuration = totalDuration % baseChunkSize;

        // Adjust chunking if the last chunk is too small and there are preceding chunks
        if (remainingDuration > 0 && remainingDuration < minLastChunkSize && numFullChunks > 0) {
            numFullChunks--; // The last "full" chunk will absorb the small remainder
            remainingDuration += baseChunkSize; // New size of the last (now combined) chunk
        } else if (numFullChunks == 0 && remainingDuration == 0 && totalDuration > 0) {
            // Edge case: total duration is very small, less than baseChunkSize, but not zero.
            // Ensure at least one chunk is processed if there's any content.
            // This will be handled by the loop correctly if numFullChunks is 0 and remainingDuration > 0
        }


        List<SubtitleLine> currentChunk = new ArrayList<>();
        int currentChunkEndTimeTarget;
        int chunkIndex = 0;

        if (numFullChunks == 0) { // Video is shorter than baseChunkSize
            currentChunkEndTimeTarget = totalDuration;
        } else {
            currentChunkEndTimeTarget = baseChunkSize;
        }


        for (SubtitleLine subtitle : allLines) {
            // If the current subtitle starts beyond the current chunk's target end time
            // AND we are not supposed to be putting everything into the last adjusted chunk.
            if (subtitle.getStart() >= currentChunkEndTimeTarget && chunkIndex < numFullChunks) {
                if (!currentChunk.isEmpty()) {
                    chunkedSubtitles.add(new ArrayList<>(currentChunk)); // Add a copy
                    currentChunk.clear();
                }
                chunkIndex++;
                currentChunkEndTimeTarget = (chunkIndex + 1) * baseChunkSize;
            }
            // Add to the current chunk
            // This also handles the case where chunkIndex >= numFullChunks (i.e., the last chunk, possibly adjusted)
            currentChunk.add(subtitle);
        }

        // Add the last remaining chunk
        if (!currentChunk.isEmpty()) {
            chunkedSubtitles.add(currentChunk);
        }

        return chunkedSubtitles;
    }


    // Removed old fetchData2 and fetchDataViaProxy as they are replaced by fetchTranscriptFromApi
    // Removed old parseTranscript as its functionality is split:
    //   - JSON parsing is in getSubtitles
    //   - Chunking is in chunkSubtitles
}

// Make sure you have a SubtitleLine class/record somewhere accessible
// e.g. (if not already defined elsewhere):
/*
class SubtitleLine {
    private int start;
    private String text;

    public SubtitleLine(int start, String text) {
        this.start = start;
        this.text = text;
    }

    public int getStart() {
        return start;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        // Example: "00:01:30 Hello world"
        long hours = start / 3600;
        long minutes = (start % 3600) / 60;
        long seconds = start % 60;
        return String.format("%02d:%02d:%02d %s", hours, minutes, seconds, text);
    }
}
*/