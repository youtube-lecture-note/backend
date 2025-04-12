package com.example.youtube_lecture_helper.openai_api;

import com.example.youtube_lecture_helper.SummaryStatus;
import com.example.youtube_lecture_helper.entity.Quiz;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;


//영상 5분당 요청 1번 보내기=>비동기 처리
@Component
public class OpenAIGptClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    private final OkHttpClient client;
    private final ExecutorService executor;
    private String summaryModel;
    private String quizModel;

    public OpenAIGptClient(){
        this.client = new OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newFixedThreadPool(8);
        this.summaryModel = "gpt-4o-mini";
        this.quizModel = "gpt-4o-2024-08-06";
    }

    public CompletableFuture<List<Quiz>> sendSummariesAndGetQuizzesAsync(String videoId, String summary) {
        List<String> summaries = LectureSummarySplitter.splitLectureSummary(summary);
        int i =0;
        for(String sm: summaries){
            System.out.println("chunck " + i++  + "번째: " + sm.length());
        }
//
        List<CompletableFuture<List<Quiz>>> futures = new ArrayList<>();

        for (String sm : summaries) {
            futures.add(sendSummaryAndGetQuizAsync(videoId, sm));
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
    private CompletableFuture<List<Quiz>> sendSummaryAndGetQuizAsync(String videoId, String summary) {

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", quizModel);
        requestBody.put("messages", new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", "You are an educational quiz generator who communicates in Korean. " +
                        "Create quizzes question strictly based on the information provided in lecutre summaries. " +
                        "Focus on the key points and important ideas. Avoid questions about trivial or overly detailed information. " +
                        "Do not use any external knowledge or assumptions beyond what is explicitly stated in the input content. " +
                        "Each question must be directly answerable from the lecture content providedEach question must be based on the lecture content and logically inferable from the provided information, ensuring it aligns with the main themes and concepts discussed. " +
                        "Provide four answer choices, including one correct answer and three plausible but incorrect options. " +
                        "The question should test conceptual understanding rather than just memorization. "+
                        "And give timestamps corresponding to each question" +
                        "Generate a quiz in the following format without numbering:\n" +
                        "[Question];[1. Option 1];[2. Option 2];[3. Option 3];[4. Option 4];[Answer Option Number];[Explanation of the correct answer];[timestamp]\n" +
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
                return parseQuizResponse(videoId, content);
            } catch (IOException e) {
                e.printStackTrace();
                return null; // Handle error scenario
            }
        },executor);
    }

    private List<Quiz> parseQuizResponse(String videoId, String responseContent) {
        String[] quizLines = responseContent.split("\n");
        List<Quiz> quizzes = new ArrayList<>();

        for (int i = 0; i < quizLines.length; i++) {
            try {
                System.out.println(quizLines[i]);
                String[] quizElement = quizLines[i].split(";");
                if (quizElement.length!=8) {    //배열 크기가 올바르지 않을 경우 건너뛰기
                    continue;
                }
                List<String> optionsList = Arrays.stream(quizElement, 1, 5) // index 1~4
                        .map(option -> option.replaceFirst("^\\d+\\.\\s*", "")) // "1. " 제거
                        .toList();
                // 각 문제에 대해 Quiz 객체 생성
                quizzes.add(new Quiz(videoId, quizElement[0].trim(), optionsList, quizElement[5].trim(), quizElement[6].trim(), Integer.parseInt(quizElement[7].trim())));
//                System.out.println("Print: " + quizElement[0].trim() + optionsList + quizElement[5].trim() + Integer.parseInt(quizElement[6].trim()));
            }catch(Exception e){
                //파싱 실패할 경우 처리해야 함
                System.out.println("Error parsing line, skipping: " + quizLines[i]);
                e.printStackTrace(); 
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
    public CompletableFuture<SummaryResult> getVideoSummaryAsync(String videoId, String language) {
        return CompletableFuture.supplyAsync(() -> {
            try{
                List<List<SubtitleLine>> subtitleChunks = YoutubeSubtitleExtractor.getSubtitles(videoId, language);
                System.out.println("subtitle check complete");
                return processSubtitleChunks(subtitleChunks);
            }catch(Exception e){
                return new SummaryResult(SummaryStatus.NO_SUBTITLE,null);
            }
        }, executor);
    }

    private SummaryResult processSubtitleChunks(List<List<SubtitleLine>> subtitleChunks) {
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
            for(String summary: summaries){
                if(summary.trim().equals("-1")){
                    return new SummaryResult(SummaryStatus.NOT_LECTURE,null);
                }
            }
            return new SummaryResult(SummaryStatus.SUCCESS, String.join("\n\n", summaries));
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
                .map(SubtitleLine::toString)
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

    public CompletableFuture<List<Quiz>> sendSummariesAndGetQuizzesAsyncV2(String videoId, String summary, QuizType quizType) {
        List<String> summaries = LectureSummarySplitter.splitLectureSummary(summary);
        List<CompletableFuture<List<Quiz>>> futures = new ArrayList<>();

        for (String sm : summaries) {
            futures.add(sendSummaryAndGetQuizAsyncV2(videoId, sm, quizType));
        }

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
    private String generateSystemPrompt(QuizType type) {
        if (type == QuizType.MULTIPLE_CHOICE) {
            return "You are an educational quiz generator who communicates in Korean. " +
                    "Create quizzes question strictly based on the information provided in lecutre summaries. " +
                    "Focus on the key points and important ideas. Avoid questions about trivial or overly detailed information. " +
                    "Do not use any external knowledge or assumptions beyond what is explicitly stated in the input content. " +
                    "Each question must be based on the lecture content and logically inferable from the provided information, ensuring it aligns with the main themes and concepts discussed. " +
                    "Provide four answer choices, including one correct answer and three plausible but incorrect options. " +
                    "The question should test conceptual understanding rather than just memorization. "+
                    "And give timestamps corresponding to each question" +
                    "Generate a quiz in the following format without numbering:\n" +
                    "[Question];[1. Option 1];[2. Option 2];[3. Option 3];[4. Option 4];[Answer Option Number];[Explanation of the correct answer];[timestamp]\n" +
                    "Example: " + "What does a molecular formula represent?;It represents the properties of an atom.;It represents the types and numbers of atoms in a molecule.;It represents the state of a substance.;It represents the result of a chemical reaction.;2;10  \n" +
                    "What does a cation mean?;It is an ion that loses electrons and carries a positive charge.;It is an ion that gains electrons and carries a negative charge.;It has an equal number of electrons and protons.;It is an ion without electrons.;1;250  ";
        } else if (type == QuizType.SHORT_ANSWER) {
            return "You are an educational quiz generator who communicates in Korean. " +
                    "Generate short answer questions from the following lecture summary. " +
                    "Focus on the key points and important ideas. Avoid questions about trivial or overly detailed information. " +
                    "The question should test conceptual understanding rather than just memorization. "+
                    "Each question must be based on the lecture content and logically inferable from the provided information, ensuring it aligns with the main themes and concepts discussed. " +
                    "Each question should test understanding of key concepts. Provide the correct answer and timestamp corresponding to each question. " +
                    "Format:\nQuestion;Correct Answer;Explanation of the correct answer;Timestamp as second\n" +
                    "Example: What is the main function of red blood cells?;To carry oxygen throughout the body.;Red blood cells contain hemoglobin, which binds to oxygen in the lungs and transports it to tissues throughout the body.;240\n";
        }
        return "";
    }

    private CompletableFuture<List<Quiz>> sendSummaryAndGetQuizAsyncV2(String videoId, String summary, QuizType quizType) {
        String systemPrompt = generateSystemPrompt(quizType);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", quizModel);
        requestBody.put("messages", new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", systemPrompt))
                .put(new JSONObject().put("role", "user").put("content", summary))
        );
        requestBody.put("temperature", 0.5);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        return CompletableFuture.supplyAsync(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Error: " + response.code() + " - " + response.message());
                }

                JSONObject responseBody = new JSONObject(response.body().string());
                String content = responseBody.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                // 퀴즈 유형에 따라 파싱 함수 분기
                return quizType == QuizType.MULTIPLE_CHOICE
                        ? parseQuizResponse(videoId, content)
                        : parseShortAnswerQuizResponse(videoId, content);

            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }, executor);
    }

    private List<Quiz> parseShortAnswerQuizResponse(String videoId, String responseContent) {
        String[] quizLines = responseContent.split("\n");
        List<Quiz> quizzes = new ArrayList<>();

        for (String line : quizLines) {
            try {
                String[] parts = line.split(";");
                if (parts.length != 4) continue;

                quizzes.add(new Quiz(
                        videoId,
                        parts[0].trim(), // question
                        null,            // options (null for short-answer)
                        parts[1].trim(), // correctAnswer
                        parts[2].trim(), // explanation
                        Integer.parseInt(parts[3].trim()) // timestamp
                ));
            } catch (Exception e) {
                System.out.println("Error parsing short-answer line: " + line);
                e.printStackTrace();
            }
        }
        return quizzes;
    }

    public boolean isCorrectSubjectiveAnswer(String question, String correctAnswer, String userAnswer){
        String prompt = String.format("""
            문제: %s
            정답: %s
            사용자 답변: %s
            """, question, correctAnswer, userAnswer);

        String result = ask(prompt,quizModel);
        result = result.trim();

        // 결과가 정확하게 1 또는 0인지 확인 후 정수 반환
        if (result.equals("1")) return true;
        else return false;
    }
    private String ask(String userPrompt, String model) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("messages", new JSONArray()
                .put(new JSONObject().put("role", "system").put("content",
                    "You are a grading assistant who communicates in Korean. You will receive a question, a correct answer, and a user's answer. " +
                    "If the user's answer means the same or is very close in meaning to the correct answer, return \"1\"\n"+
                    "If the user's answer is incorrect or does not match the meaning, return \"0\"." +
                    "Be lenient for minor phrasing differences, but don't allow completely wrong answers. " +
                    "Respond only with \"1\" or \"0\", without any explanation."
                ))
                .put(new JSONObject().put("role", "user").put("content", userPrompt))
        );
        requestBody.put("temperature", 0.5);

        // 2. HTTP 요청 생성
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        // 3. 응답 처리
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }

            String responseBody = response.body().string();

            // 4. 응답 JSON 파싱해서 answer 추출
            JSONObject json = new JSONObject(responseBody);
            String answer = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            return answer.trim(); // 공백 제거해서 정제된 답 반환
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

