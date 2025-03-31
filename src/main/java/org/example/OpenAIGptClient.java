package org.example;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//영상 5분당 요청 1번 보내기=>비동기 처리
public class OpenAIGptClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    private final OkHttpClient client;
    private final ExecutorService executor;
    private String summaryModel;
    private String quizModel;

    public void setQuizModel(String quizModel){
        this.quizModel = quizModel;
    }
    public void setSummaryModel(String summaryModel){
        this.summaryModel = summaryModel;
    }

    public OpenAIGptClient(int threadPoolSize, String summaryModel, String quizModel) {
        System.out.println(API_KEY);
        this.client = new OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.summaryModel = summaryModel;
        this.quizModel = quizModel;
    }

    public String sendCaptionAndGetQuiz(String caption) throws IOException{
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", "You are an educational quiz generator who communicates in Korean. " +
                        "Create quizzes question strictly based on the information provided in lecutre summaries. " +
                        "Focus on the key points and important ideas. Avoid questions about trivial or overly detailed information. " +
                        "Do not use any external knowledge or assumptions beyond what is explicitly stated in the input content. " +
                        "Each question must be directly answerable from the lecture content provided and should be clear and relevant to test the understanding of the material. " +
                        "Provide four answer choices, including one correct answer and three plausible but incorrect options. " +
                        "The question should test conceptual understanding rather than just memorization. "+
                        "And give timestamps corresponding to each question" +
                        "Generate a quiz in the following format without numbering:\n" +
                        "[Question];[Option 1];[Option 2];[Option 3];[Option 4];[Answer];[timestamp]" +
                        "Example: " + "What does a molecular formula represent?;It represents the properties of an atom.;It represents the types and numbers of atoms in a molecule.;It represents the state of a substance.;It represents the result of a chemical reaction.;B;10  \n" + "What does a cation mean?;It is an ion that loses electrons and carries a positive charge.;It is an ion that gains electrons and carries a negative charge.;It has an equal number of electrons and protons.;It is an ion without electrons.;A;250  "
                ))
                .put(new JSONObject().put("role", "user").put("content", caption))
        );
        requestBody.put("temperature", 0.5);

        // HTTP 요청 생성
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        // 요청 실행 및 응답 처리
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error: " + response.code() + " - " + response.message();
            }

            // JSON 응답에서 메시지 추출
            JSONObject responseBody = new JSONObject(response.body().string());
            return responseBody.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        }
    }




    public CompletableFuture<List<Quiz>> sendSummariesAndGetQuizzesAsync(String summary) {
        List<String> summaries = LectureSummarySplitter.splitLectureSummary(summary);
        int i =0;
        for(String sm: summaries){
            System.out.println("chunck " + i++  + "번째: " + sm.length());
        }
//
        List<CompletableFuture<List<Quiz>>> futures = new ArrayList<>();

        for (String sm : summaries) {
            futures.add(sendSummaryAndGetQuizAsync(sm));
        }

        // Combine all futures to a list of QuizQuestions
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<Quiz> quizQuestions = new ArrayList<>();
                    for (CompletableFuture<List<Quiz>> future : futures) {
                        try {
                            quizQuestions.addAll(future.get());
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    return quizQuestions;
                });
    }
    private CompletableFuture<List<Quiz>> sendSummaryAndGetQuizAsync(String summary) {

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", quizModel);
        requestBody.put("messages", new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", "You are an educational quiz generator who communicates in Korean. " +
                        "Create quizzes question strictly based on the information provided in lecutre summaries. " +
                        "Focus on the key points and important ideas. Avoid questions about trivial or overly detailed information. " +
                        "Do not use any external knowledge or assumptions beyond what is explicitly stated in the input content. " +
                        "Each question must be directly answerable from the lecture content provided and should be clear and relevant to test the understanding of the material. " +
                        "Provide four answer choices, including one correct answer and three plausible but incorrect options. " +
                        "The question should test conceptual understanding rather than just memorization. "+
                        "And give timestamps corresponding to each question" +
                        "Generate a quiz in the following format without numbering:\n" +
                        "[Question];[1. Option 1];[2. Option 2];[3. Option 3];[4. Option 4];[Answer Option Number];[timestamp]\n" +
                        "Example: " + "What does a molecular formula represent?;It represents the properties of an atom.;It represents the types and numbers of atoms in a molecule.;It represents the state of a substance.;It represents the result of a chemical reaction.;2;10  \n" + "What does a cation mean?;It is an ion that loses electrons and carries a positive charge.;It is an ion that gains electrons and carries a negative charge.;It has an equal number of electrons and protons.;It is an ion without electrons.;A;250  "
                ))
                .put(new JSONObject().put("role", "user").put("content", summary))
        );
        requestBody.put("temperature", 0.5);

        // HTTP 요청 생성
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        // 요청 실행 및 응답 처리
        return CompletableFuture.supplyAsync(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Error: " + response.code() + " - " + response.message());
                }

                // JSON 응답에서 메시지 추출
                JSONObject responseBody = new JSONObject(response.body().string());
                String content = responseBody.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                return parseQuizResponse(content);
            } catch (IOException e) {
                e.printStackTrace();
                return null; // Handle error scenario
            }
        },executor);
    }

    private List<Quiz> parseQuizResponse(String responseContent) {
        String[] quizLines = responseContent.split("\n");
        List<Quiz> quizzes = new ArrayList<>();

        for (int i = 0; i < quizLines.length; i++) {
            try {
                String[] quizElement = quizLines[i].split(";");
                if (quizElement.length!=7) {    //배열 크기가 올바르지 않을 경우 건너뛰기
                    continue;
                }
                List<String> optionsList = Arrays.stream(quizElement, 1, 5) // index 1~4
                        .map(option -> option.replaceFirst("^\\d+\\.\\s*", "")) // "1. " 제거
                        .toList();
                // 각 문제에 대해 Quiz 객체 생성
                quizzes.add(new Quiz(quizElement[0].trim(), optionsList, quizElement[5].trim(), Integer.parseInt(quizElement[6].trim())));
//                System.out.println("Print: " + quizElement[0].trim() + optionsList + quizElement[5].trim() + Integer.parseInt(quizElement[6].trim()));
            }catch(Exception e){
                System.out.println("Error parsing line, skipping: " + quizLines[i]);
                e.printStackTrace();  // 필요시 스택 트레이스를 출력하여 디버깅 정보 제공
            }
        }

        return quizzes;
    }

    private static class LectureSummarySplitter {
        private static final int CHUNK_SIZE = 3000; // maximum byte size per chunk
        private static final int MIN_LAST_CHUNK_SIZE = 1500; // minimum size for last chunk

        // Method to split the lecture notes into chunks of 3000 bytes
        public static List<String> splitLectureSummary(String lectureSummary) {
            List<String> chunks = new ArrayList<>();
            int length = lectureSummary.length();
            int start = 0;

            // Loop to split the lectureSummary into chunks
            while (start < length) {
                int end = Math.min(start + CHUNK_SIZE, length);
                chunks.add(lectureSummary.substring(start, end));
                start = end;
            }

            // If the last chunk is too small, merge it with the previous chunk
            if (chunks.size() > 1) {
                String lastChunk = chunks.get(chunks.size() - 1);
                if (lastChunk.length() <= MIN_LAST_CHUNK_SIZE) {
                    chunks.set(chunks.size() - 2, chunks.get(chunks.size() - 2) + lastChunk);
                    chunks.remove(chunks.size() - 1);
                }
            }

            return chunks;
        }
    }

    /**
     * Gets a summary of a YouTube video using its subtitles (asynchronous version)
     * @param videoId The YouTube video ID
     * @param language The language code for subtitles
     * @return CompletableFuture that will contain the complete summary
     */
    public CompletableFuture<String> getVideoSummaryAsync(String videoId, String language) {
        return CompletableFuture.supplyAsync(() -> {
            try{
                List<List<SubtitleLine>> subtitleChunks = YoutubeSubtitleExtractor.getSubtitles(videoId, language);
                System.out.println("subtitle check complete");
                return processSubtitleChunks(subtitleChunks);
            }catch(Exception e){
                return null;
            }
        }, executor);
    }

    private String processSubtitleChunks(List<List<SubtitleLine>> subtitleChunks) {
        try {
            List<CompletableFuture<String>> futures = new ArrayList<>();

            // Create and collect all futures
            for (List<SubtitleLine> chunk : subtitleChunks) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> getSummaryBySubtitleChunk(chunk), executor));
            }

            // Wait for all futures to complete and collect results
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            // Block until all are complete and join results
            CompletableFuture<List<String>> resultsFuture = allFutures.thenApply(v ->
                    futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList())
            );

            List<String> summaries = resultsFuture.get();
            return String.join("\n\n", summaries);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process subtitle chunks: " + e.getMessage(), e);
        }
    }

    public String sendCaptionAndGetSummary(String userMessage) throws IOException {
        // JSON 요청 바디 생성
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", summaryModel);
        requestBody.put("messages", new JSONArray()
                .put(new JSONObject()
                        .put("role", "system")
                        .put("content", "You are an AI assistant that summarizes lecture content into concise notes. " +
                                "Each input data consists of time(second) and content. " +
                                "If the input is not related to a lecture or knowledge-based topic, return \"-1\" instead of a summary. " +
                                "If it IS a lecture video, provide a structured summary of the content. " +
                                "If the provided subtitles are not in Korean, translate them into Korean before summarizing. " +
                                "Each summary consists of a title and content. " +
                                "Format the summary with appropriate line breaks, grouping related ideas into separate sections or bullet points. " +
                                "Each point should contain a brief, clear statement of the idea discussed in the lecture, and should be formatted in a way " +
                                "that would resemble lecture notes taken by a student. " +
                                "The start time (in seconds) should be included at the beginning of the title, formatted as '<time; title>' without extra text. " +
                                "Time intervals should be dynamically adjusted based on content changes " +
                                "No time should be included in the content. " +
                                "The summary should resemble a bullet-point list of key ideas and should use concise phrasing typical of lecture notes."))
                .put(new JSONObject()
                        .put("role", "user")
                        .put("content", userMessage))
        );
        requestBody.put("temperature", 0.2);

        // HTTP 요청 생성
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        // 요청 실행 및 응답 처리
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error: " + response.code() + " - " + response.message();
            }

            // JSON 응답에서 메시지 추출
            JSONObject responseBody = new JSONObject(response.body().string());
            return responseBody.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        }
    }

    private String getSummaryBySubtitleChunk(List<SubtitleLine> chunk) {
        String caption = chunk.stream()
                .map(org.example.SubtitleLine::toString)
                .reduce("", (a, b) -> a + "\n" + b);
        try{
            return sendCaptionAndGetSummary(caption);
        }catch(IOException e){
            e.printStackTrace();
            return "Failed";
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

}

