package com.example.youtube_lecture_helper.openai_api;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@Component
public class YoutubeSubtitleExtractor {
    @Value("${proxy.server.addr}")
    private String proxyBaseUrl;
    @Value("${proxy.secret.key}")
    private String proxySecretKey;
//    private static String proxyBaseUrl;
//    private static String proxySecretKey;
//    @Autowired
//    public YoutubeSubtitleExtractor(Environment env) {
//        proxyBaseUrl = env.getProperty("proxy.server.addr");
//        proxySecretKey = env.getProperty("proxy.secret.key");
//    }

    private static final HttpClient client = HttpClient.newHttpClient();

    public static String getYouTubeTitle(String youtubeId) {
        String url = "https://www.youtube.com/watch?v=" + youtubeId;
        try {
            // User-Agent를 설정해야 403 에러 방지
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();
            // <title> 태그에서 제목 추출
            String title = doc.title();
            if (title.endsWith(" - YouTube")) {
                title = title.replaceFirst(" - YouTube$", "");
            }
            return title;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching title.";
        }
    }

    //소수점 버리고 정수부분만 저장(토큰 아끼기)+toString에서 metadata 제거: 토큰 사용 2400=>1900=>hh:mm:ss 사용으로 2300으로 증가.
    //hh:mm:ss 포맷 사용 안하면 퀴즈에 timestamp가 제대로 안나온다.

    private static String fetchData2(String urlString) throws IOException, InterruptedException {
        System.out.println("DEBUG: Fetching URL: " + urlString); // Log the URL

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .GET()
                .build();

        // You can reuse a client, but creating a new one is fine too.
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(new CookieManager()) // Helps manage cookies if needed for subsequent requests or sessions
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Specify UTF-8
        } catch (IOException | InterruptedException e) {
            System.err.println("ERROR: Failed to send request or interrupted for URL: " + urlString);
            e.printStackTrace(); // Print stack trace for detailed error
            throw e; // Re-throw the exception
        }

        int statusCode = response.statusCode();
        System.out.println("DEBUG: Received HTTP Status Code: " + statusCode + " for URL: " + urlString); // Log status code

        String responseBody = response.body();

        if (statusCode != 200) {
            System.err.println("ERROR: Received non-200 status code (" + statusCode + ") for URL: " + urlString);
            // Log the first part of the response body for clues (e.g., error message, CAPTCHA page)
            System.err.println("ERROR: Response Body Snippet: " +
                               (responseBody != null && responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody));
             // You might want to throw an exception here instead of returning the error page body
             // throw new IOException("HTTP request failed with status code: " + statusCode);
             // Or return an empty string/null if your calling code handles it, but throwing is often clearer
             return ""; // Returning empty string will likely cause the "Could not find captions" error later
        }

        // Optionally log a snippet of the successful response body for comparison
        // System.out.println("DEBUG: Response Body Snippet (Success): " +
        //                    (responseBody != null && responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody));

        return responseBody;
    }

    private static final HttpClient client2 = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .cookieHandler(new CookieManager())
            .build();

    // private static final String PROXY_BASE_URL = System.getenv("PROXY_SERVER_ADDR");
    // private static final String PROXY_SECRET_KEY = System.getenv("PROXY_SECRET_KEY");

    private String fetchDataViaProxy(String targetUrlString) throws IOException, InterruptedException {
        System.out.println("DEBUG: Target URL: " + targetUrlString);

        String encodedTargetUrl = URLEncoder.encode(targetUrlString, StandardCharsets.UTF_8.name());
        String proxyUrlString = proxyBaseUrl + encodedTargetUrl;

        System.out.println("DEBUG: Fetching via Proxy URL: " + proxyUrlString);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(proxyUrlString))
                .header("X-Proxy-Secret", proxySecretKey)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            System.err.println("ERROR: Failed to send request to proxy or interrupted for target URL: " + targetUrlString);
            e.printStackTrace();
            throw e;
        }

        int statusCode = response.statusCode();
        System.out.println("DEBUG: Received HTTP Status Code from Proxy: " + statusCode + " for target URL: " + targetUrlString);

        String responseBody = response.body();

        if (statusCode < 200 || statusCode >= 300) {
            System.err.println("ERROR: Received non-2xx status code (" + statusCode + ") from proxy for target URL: " + targetUrlString);
            System.err.println("ERROR: Response Body Snippet: " +
                               (responseBody != null && responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody));
             throw new IOException("Proxy request failed with status code: " + statusCode + " for target: " + targetUrlString);
            // return ""; // 또는 빈 문자열 반환
        }

        return responseBody;
    }

    public List<List<SubtitleLine>> getSubtitles(String videoID, String lang) throws IOException, InterruptedException {
        if (lang == null) {
            lang = "ko";
        }

        String data = fetchDataViaProxy("https://youtube.com/watch?v=" + videoID);


        // 캡션 트랙 데이터 확인
        if (!data.contains("captionTracks")) {
            throw new RuntimeException("Could not find captions for video: " + videoID);
        }

        // 정규식으로 캡션 트랙 데이터 추출
        Pattern regex = Pattern.compile("\"captionTracks\":(\\[.*?\\])");
        Matcher matcher = regex.matcher(data);

        if (!matcher.find()) {
            throw new RuntimeException("Could not parse caption tracks data");
        }

        String match = matcher.group(1);
        System.out.println(match);

        try {
            JSONArray captionTracks = new JSONArray(match);
            JSONObject subtitle = null;

            // 요청된 언어의 자막 찾기
            for (int i = 0; i < captionTracks.length(); i++) {
                JSONObject track = captionTracks.getJSONObject(i);
                String vssId = track.optString("vssId", "");

                if (vssId.equals("." + lang) || vssId.equals("a." + lang) || vssId.contains("." + lang)) {
                    subtitle = track;
                    break;
                }
            }

            // 한국어 자막을 찾지 못한 경우
            if (subtitle == null || !subtitle.has("baseUrl")) {
                System.out.println("Could not find " + lang + " captions for " + videoID + ". Trying English captions...");

                // 영어 자막 찾기
                subtitle = null;
                for (int i = 0; i < captionTracks.length(); i++) {
                    JSONObject track = captionTracks.getJSONObject(i);
                    String vssId = track.optString("vssId", "");

                    if (vssId.equals("a.en") || vssId.contains(".en")) {
                        subtitle = track;
                        break;
                    }
                }

                // 영어 자막도 없으면 예외 처리
                if (subtitle == null || !subtitle.has("baseUrl")) {
                    System.out.println("Could not find English captions for " + videoID);
                    throw new RuntimeException("Could not find English captions for " + videoID);
                }
            }

            // 자막 데이터 가져오기
            String transcriptUrl = subtitle.getString("baseUrl");
            String transcript = fetchData2(transcriptUrl);

            // XML 파싱 및 자막 라인 추출
            return parseTranscript(transcript);

        } catch (JSONException e) {

            throw new RuntimeException("Error parsing JSON: " + e.getMessage());
        }
    }



    private static List<List<SubtitleLine>> parseTranscript(String transcript) {
        List<List<SubtitleLine>> ret = new ArrayList<>();
        List<SubtitleLine> lines = new ArrayList<>();

        // XML 태그 제거 및 정리
        String cleanedTranscript = transcript
                .replace("<?xml version=\"1.0\" encoding=\"utf-8\" ?><transcript>", "")
                .replace("</transcript>", "");

        // 텍스트 라인별로 분리
        String[] textLines = cleanedTranscript.split("</text>");

        int currentChunkStartTime = 0;
        int intervalTimes = 600; //10분 단위로 끊어서 반환 리스트에 넣기
        int lastTimestamp = 0;
        
        //모든 자막 먼저 얻어서 마지막 자막 시간 알아놓기
        for (String line : textLines) {
            line = line.trim();
            if (line.isEmpty()) { continue; }
            // 시작 시간 추출
            Pattern startRegex = Pattern.compile("start=\"([\\d.]+)\"");
            Matcher startMatcher = startRegex.matcher(line);
            if (!startMatcher.find()) {
                continue;
            }
            //xx.xxx초 => x초 (int로 변경)
            int start = (int) Double.parseDouble(startMatcher.group(1));
            if (start > lastTimestamp) {
                lastTimestamp = start;
            }
            String text = line.replaceAll("<text[^>]*>", "")
                    .replace("&amp;", "&");

            // HTML 태그 제거
            text = Jsoup.parse(text).text();
            lines.add(new SubtitleLine(start, text));
        }

        int totalDuration = lastTimestamp + 5;
        int baseChunkSize = 600;        //기본 10분 이상
        int minLastChunkSize = 180;     //마지막 청크 : 최소 3분 이상
        int numFullChunks = totalDuration / baseChunkSize;
        int lastChunkSize = totalDuration % baseChunkSize;
        //마지막 청크 길이가 짧을 경우 이전 청크에 붙이기.
        if (lastChunkSize > 0 && lastChunkSize < minLastChunkSize && numFullChunks > 0) {
            numFullChunks--;
            lastChunkSize = baseChunkSize + lastChunkSize;
        }

        List<SubtitleLine> currentChunk = new ArrayList<>();
        int currentChunkEndTime = (numFullChunks > 0) ? baseChunkSize : totalDuration;
        int chunkIndex = 0;

        for (SubtitleLine subtitle : lines) {
            if (subtitle.getStart() < currentChunkEndTime || chunkIndex >= numFullChunks) {
                // 현재 청크에 추가
                currentChunk.add(subtitle);
            } else {
                // 현재 청크 완성하고 새 청크 시작
                if (!currentChunk.isEmpty()) {
                    ret.add(currentChunk);
                    currentChunk = new ArrayList<>();
                }
                chunkIndex++;

                // 다음 청크의 종료 시간 설정
                if (chunkIndex < numFullChunks) {
                    currentChunkEndTime = (chunkIndex + 1) * baseChunkSize;
                } else {
                    currentChunkEndTime = totalDuration;    // 마지막 청크
                }
                currentChunk.add(subtitle);
            }
        }

        // 마지막 청크 추가
        if (!currentChunk.isEmpty()) {
            ret.add(currentChunk);
        }
        return ret;
    }
}