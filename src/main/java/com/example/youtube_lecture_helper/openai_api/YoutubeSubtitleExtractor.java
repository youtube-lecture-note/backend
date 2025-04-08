package org.example;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

public class YoutubeSubtitleExtractor {

    private static final HttpClient client = HttpClient.newHttpClient();

    //소수점 버리고 정수부분만 저장(토큰 아끼기)+toString에서 metadata 제거: 토큰 사용 2400=>1900=>hh:mm:ss 사용으로 2300으로 증가.
    //hh:mm:ss 포맷 사용 안하면 퀴즈에 timestamp가 제대로 안나온다.

    private static String fetchData2(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .GET()
                .build();

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(new CookieManager())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static List<List<SubtitleLine>> getSubtitles(String videoID, String lang) throws IOException, InterruptedException {
        if (lang == null) {
            lang = "ko";
        }

        String data = fetchData2("https://youtube.com/watch?v=" + videoID);


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
        int intervalTimes = 300; //5분 단위로 끊어서 반환 리스트에 넣기
        //마지막 자막이 7분이 넘었을 경우에 5분 단위로 끈힉,
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
        int baseChunkSize = 300;        //기본 5분 이상
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