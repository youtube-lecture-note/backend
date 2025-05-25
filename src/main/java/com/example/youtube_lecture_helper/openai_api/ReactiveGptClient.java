package com.example.youtube_lecture_helper.openai_api;

import com.example.youtube_lecture_helper.SummaryStatus;
import com.example.youtube_lecture_helper.entity.Quiz;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Component
@Slf4j
public class ReactiveGptClient {
    private final WebClient webClient;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String quizModel = "gpt-4o-2024-08-06";
    private static final String summaryModel = "gpt-4o-mini";
    private final YoutubeSubtitleExtractor youtubeSubtitleExtractor;


    public ReactiveGptClient(YoutubeSubtitleExtractor youtubeSubtitleExtractor) {
        this.webClient = WebClient.builder()
                .baseUrl(API_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.youtubeSubtitleExtractor = youtubeSubtitleExtractor;
    }

    public Mono<Boolean> isCorrectSubjectiveAnswer(String question, String correctAnswer, String userAnswer) {
        String prompt = String.format("""
            문제: %s
            정답: %s
            사용자 답변: %s
            """, question, correctAnswer, userAnswer);

        return ask(prompt, quizModel)
                .map(result -> result.trim().equals("1"));
    }

    private Mono<String> ask(String userPrompt, String model) {
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

        return webClient.post()
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> {
                    JSONObject json = new JSONObject(responseBody);
                    return json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just("0"); // 에러 발생 시 기본값으로 오답 처리
                });
    }

    private List<Quiz> parseQuizResponse(String videoId, String responseContent) {
        if (responseContent == null || responseContent.isBlank()) {
            return Collections.emptyList();
        }
        String[] quizLines = responseContent.split("\n");
        List<Quiz> quizzes = new ArrayList<>();

        for (String line : quizLines) {
            if (line == null || line.isBlank()) continue; // Skip empty lines

            try {
                log.debug("Parsing quiz line: {}", line);
                String[] quizElement = line.split(";");
                if (quizElement.length != 9) {
                    log.warn("Skipping malformed quiz line (expected 9 parts, got {}): {}", quizElement.length, line);
                    continue;
                }
                List<String> optionsList = Arrays.stream(quizElement, 1, 5)
                        .map(option -> option.replaceFirst("^\\d+\\.\\s*", "").trim())
                        .collect(Collectors.toList()); // Use mutable list

                // Validate required fields before creating Quiz object
                String question = quizElement[0].trim();
                String answer = quizElement[5].trim();
                String explanation = quizElement[6].trim();
                int timestamp = Integer.parseInt(quizElement[7].trim());
                int difficulty = Integer.parseInt(quizElement[8].trim());

                if (question.isEmpty() || answer.isEmpty() || optionsList.size() != 4 || optionsList.stream().anyMatch(String::isEmpty)) {
                    log.warn("Skipping quiz line with missing data: {}", line);
                    continue;
                }

                quizzes.add(new Quiz(
                        videoId,
                        (byte) difficulty, // Placeholder for difficulty
                        question,
                        optionsList,
                        answer,
                        explanation,
                        timestamp));
            } catch (NumberFormatException e) {
                log.error("Error parsing timestamp in quiz line, skipping: {}", line, e);
            } catch (ArrayIndexOutOfBoundsException e) {
                log.error("Error splitting quiz line (unexpected format), skipping: {}", line, e);
            }
            catch (Exception e) { // Catch broader exceptions last
                log.error("Unexpected error parsing quiz line, skipping: {}", line, e);
            }
        }
        return quizzes;
    }
    /**
     * Parses the raw string response from OpenAI into a list of Quiz objects (Short Answer).
     * Handles potential parsing errors for each line.
     */
    private List<Quiz> parseShortAnswerQuizResponse(String videoId, String responseContent) {
        if (responseContent == null || responseContent.isBlank()) {
            return Collections.emptyList();
        }
        String[] quizLines = responseContent.split("\n");
        List<Quiz> quizzes = new ArrayList<>();

        for (String line : quizLines) {
            if (line == null || line.isBlank()) continue; // Skip empty lines

            try {
                log.debug("Parsing short answer quiz line: {}", line);
                String[] parts = line.split(";");
                if (parts.length != 5) {
                    log.warn("Skipping malformed short answer line (expected 5 parts, got {}): {}", parts.length, line);
                    continue;
                }

                String question = parts[0].trim();
                String correctAnswer = parts[1].trim();
                String explanation = parts[2].trim();
                int timestamp = Integer.parseInt(parts[3].trim());
                int difficulty = Integer.parseInt(parts[4].trim());

                if (question.isEmpty() || correctAnswer.isEmpty()) {
                    log.warn("Skipping short answer line with missing data: {}", line);
                    continue;
                }

                quizzes.add(new Quiz(
                        videoId,
                        (byte) difficulty, // Placeholder for difficulty
                        question,
                        null, // No options for short answer
                        correctAnswer,
                        explanation,
                        timestamp
                ));
            } catch (NumberFormatException e) {
                log.error("Error parsing timestamp in short answer line, skipping: {}", line, e);
            } catch (ArrayIndexOutOfBoundsException e) {
                log.error("Error splitting short answer line (unexpected format), skipping: {}", line, e);
            } catch (Exception e) {
                log.error("Unexpected error parsing short answer line, skipping: {}", line, e);
            }
        }
        return quizzes;
    }

    /**
     * Gets a summary of a YouTube video using its subtitles (Reactive version).
     *
     * @param videoId  The YouTube video ID
     * @param language The language code for subtitles
     * @return Mono that will emit the SummaryResult
     */
    public Mono<SummaryResult> getVideoSummaryReactive(String videoId, String language) {
        // Step 1: Get subtitles. Assume getSubtitles might be blocking.
        // Use Mono.fromCallable and subscribeOn to run it on a suitable scheduler.
        return Mono.fromCallable(() -> youtubeSubtitleExtractor.getSubtitles(videoId, language))
                .subscribeOn(Schedulers.boundedElastic()) // Schedule blocking call off the event loop
                .doOnSubscribe(s -> log.info("Starting subtitle extraction for videoId: {}", videoId))
                .doOnError(e -> log.error("Failed to get subtitles for videoId: {}", videoId, e))
                .onErrorResume(e -> Mono.just(Collections.<List<SubtitleLine>>emptyList())) // Handle subtitle extraction failure
                .flatMap(subtitleChunks -> {
                    if (subtitleChunks.isEmpty()) {
                        log.warn("No subtitles found or extraction failed for videoId: {}", videoId);
                        // Determine if this means NO_SUBTITLE or if the callable already handled specific errors
                        // Returning NO_SUBTITLE based on original logic's exception handling
                        return Mono.just(new SummaryResult(SummaryStatus.NO_SUBTITLE, null));
                    }
                    log.info("Subtitle chunks obtained for videoId: {}. Processing {} chunks.", videoId, subtitleChunks.size());
                    return processSubtitleChunksReactive(subtitleChunks);
                })
                .doOnSuccess(result -> log.info("Summary processing completed for videoId: {} with status: {}", videoId, result.getStatus()))
                .doOnError(e -> log.error("Error during summary processing pipeline for videoId: {}", videoId, e));
        // Add a timeout for the entire operation if desired
        // .timeout(Duration.ofMinutes(5));
    }

    private Mono<SummaryResult> processSubtitleChunksReactive(List<List<SubtitleLine>> subtitleChunks) {
        // Step 2: Process each chunk concurrently using flatMap
        return Flux.fromIterable(subtitleChunks)
                .flatMap(this::getSummaryBySubtitleChunkReactive) // Call OpenAI for each chunk
                .collectList() // Collect all summary parts
                .map(summaries -> {
                    // Step 3: Check results and combine
                    for (String summary : summaries) {
                        if (summary == null) { // Check for null results from API errors
                            log.warn("Received null summary part, treating as potential lecture content issue.");
                            // Decide how to handle nulls - here assuming it might indicate non-lecture
                            return new SummaryResult(SummaryStatus.NOT_LECTURE, null);
                        }
                        if (summary.trim().equals("-1")) {
                            log.info("Detected '-1' response, indicating non-lecture content.");
                            return new SummaryResult(SummaryStatus.NOT_LECTURE, null);
                        }
                    }
                    // Combine valid summary parts
                    String combinedSummary = String.join("\n\n", summaries);
                    return new SummaryResult(SummaryStatus.SUCCESS, combinedSummary);
                })
                .onErrorResume(e -> {
                    log.error("Failed to process subtitle chunks due to an error in the reactive stream: {}", e.getMessage(), e);
                    // Return a FAILED status or rethrow, depending on desired behavior
                    return Mono.just(new SummaryResult(SummaryStatus.FAILED, null)); // Example: Return FAILED status
                });
    }

    /**
     * Sends a single subtitle chunk to OpenAI API and returns the summary part reactively.
     */
    private Mono<String> getSummaryBySubtitleChunkReactive(List<SubtitleLine> chunk) {
        String caption = chunk.stream()
                .map(SubtitleLine::toString) // Assuming SubtitleLine has a sensible toString()
                .collect(Collectors.joining("\n"));

        if (caption.isBlank()) {
            log.warn("Subtitle chunk is blank, skipping API call.");
            return Mono.just(""); // Return empty string for blank chunks
        }

        return sendOpenAIRequestReactive(summaryModel, generateSummarySystemPrompt(), caption, 0.2)
                .doOnError(e -> log.error("Failed to get summary for a chunk: {}", e.getMessage()));
        // Decide how to handle errors - return null, empty string, or propagate error
        // .onErrorResume(e -> Mono.just("")); // Example: Return empty string on error
        // Or return null to be handled in processSubtitleChunksReactive
        // .onErrorReturn(null);
    }

    /**
     * Sends combined summaries to OpenAI to generate quizzes reactively.
     */
    public Mono<List<Quiz>> sendSummariesAndGetQuizzesReactive(String videoId, String summary, QuizType quizType) {
        List<String> summaryChunks = LectureSummarySplitter.splitLectureSummary(summary);
        if (summaryChunks.isEmpty()) {
            log.warn("No summary chunks to process for quiz generation for videoId: {}", videoId);
            return Mono.just(Collections.emptyList());
        }

        log.info("Generating {} quizzes for videoId: {} from {} summary chunks.", quizType, videoId, summaryChunks.size());

        // Process each summary chunk to get quizzes
        return Flux.fromIterable(summaryChunks)
                .flatMap(chunk -> sendSummaryAndGetQuizReactive(videoId, chunk, quizType)) // Get quizzes for each chunk
                .collectList() // Collect List<Quiz> from each chunk's result
                .map(listOfQuizLists -> listOfQuizLists.stream() // Flatten the list of lists into a single list
                        .flatMap(List::stream)
                        .collect(Collectors.toList()))
                .doOnSuccess(quizzes -> log.info("Successfully generated {} {} quizzes for videoId: {}", quizzes.size(), quizType, videoId))
                .doOnError(e -> log.error("Error generating quizzes for videoId: {}", videoId, e))
                .onErrorReturn(Collections.emptyList()); // Return empty list on error
    }

    /**
     * Sends a single summary chunk to OpenAI API to generate a list of quizzes reactively.
     */
    private Mono<List<Quiz>> sendSummaryAndGetQuizReactive(String videoId, String summaryChunk, QuizType quizType) {
        if (summaryChunk == null || summaryChunk.isBlank()) {
            log.warn("Summary chunk is blank, skipping quiz generation for this chunk.");
            return Mono.just(Collections.emptyList());
        }

        String systemPrompt = generateQuizSystemPromptByDifficulty(quizType);
        if (systemPrompt.isEmpty()) {
            log.error("Could not generate system prompt for quiz type: {}", quizType);
            return Mono.just(Collections.emptyList());
        }

        return sendOpenAIRequestReactive(quizModel, systemPrompt, summaryChunk, 0.5)
                .map(responseContent -> {
                    // Parse the response based on quiz type
                    return quizType == QuizType.MULTIPLE_CHOICE
                            ? parseQuizResponse(videoId, responseContent)
                            : parseShortAnswerQuizResponse(videoId, responseContent);
                })
                .doOnError(e -> log.error("Failed to get quizzes for a summary chunk: {}", e.getMessage()))
                .onErrorReturn(Collections.emptyList()); // Return empty list if API call or parsing fails
    }


    // --- Centralized OpenAI API Call Method ---

    /**
     * Generic method to send a request to the OpenAI Chat Completions API reactively.
     *
     * @param model        The OpenAI model to use.
     * @param systemPrompt The system message content.
     * @param userMessage  The user message content.
     * @param temperature  The temperature setting for the generation.
     * @return A Mono emitting the content string from the API response.
     */
    private Mono<String> sendOpenAIRequestReactive(String model, String systemPrompt, String userMessage, double temperature) {
        // JSON request body using org.json (could also use Jackson ObjectMapper)
        JSONObject requestBodyJson = new JSONObject();
        requestBodyJson.put("model", model);
        requestBodyJson.put("messages", new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", systemPrompt))
                .put(new JSONObject().put("role", "user").put("content", userMessage))
        );
        requestBodyJson.put("temperature", temperature);

        String requestBodyString = requestBodyJson.toString();
        log.debug("Sending OpenAI request. Model: {}, Temp: {}, System Prompt: {}...", model, temperature, systemPrompt.substring(0, Math.min(50, systemPrompt.length())));
        // log.trace("OpenAI Request Body: {}", requestBodyString); // Be careful logging full prompts/content

        return this.webClient.post()
                .body(BodyInserters.fromValue(requestBodyString)) // Send JSON string
                .retrieve()
                // --- Enhanced Error Handling ---
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("OpenAI Client Error ({}): {}", clientResponse.statusCode(), errorBody);
                            // You could throw a custom exception here
                            return Mono.error(new OpenAiClientException("OpenAI client error: " + clientResponse.statusCode() + " Body: " + errorBody));
                        })
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("OpenAI Server Error ({}): {}", clientResponse.statusCode(), errorBody);
                            // Retry logic could be added here using .retryWhen() before this onStatus
                            return Mono.error(new OpenAiServerException("OpenAI server error: " + clientResponse.statusCode() + " Body: " + errorBody));
                        })
                )
                // --- Response Processing ---
                .bodyToMono(String.class) // Get the response body as a String
                .flatMap(responseBodyString -> {
                    // log.trace("OpenAI Response Body: {}", responseBodyString);
                    try {
                        JSONObject responseJson = new JSONObject(responseBodyString);
                        // Basic check for expected structure
                        if (responseJson.has("choices") && responseJson.getJSONArray("choices").length() > 0) {
                            String content = responseJson.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");
                            log.debug("Received content from OpenAI. Length: {}", content.length());
                            return Mono.just(content.trim());
                        } else {
                            log.error("Unexpected OpenAI response format: {}", responseBodyString);
                            return Mono.error(new OpenAiClientException("Invalid response structure from OpenAI."));
                        }
                    } catch (Exception e) { // Catch JSON parsing errors specifically
                        log.error("Failed to parse OpenAI JSON response: {}", responseBodyString, e);
                        return Mono.error(new OpenAiClientException("Failed to parse OpenAI response.", e));
                    }
                })
                // Add a timeout specific to this API call if needed
                .timeout(Duration.ofSeconds(120), Mono.error(new OpenAiTimeoutException("OpenAI API call timed out after 120 seconds.")));
        // General error logging for the specific request
        // .doOnError(e -> log.error("Error during OpenAI API call to model {}: {}", model, e.getMessage()));

    }

    // --- Prompts ---
    // (Keep your existing prompt generation methods)

    private String generateSummarySystemPrompt() {
        return "You are an AI assistant that summarizes lecture content into concise notes. " +
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
                "The summary should resemble a bullet-point list of key ideas and should use concise phrasing typical of lecture notes.";
    }

    private String generateQuizSystemPrompt(QuizType type) {
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
                    "Example: " + "What does a molecular formula represent?;It represents the properties of an atom.;It represents the types and numbers of atoms in a molecule.;It represents the state of a substance.;It represents the result of a chemical reaction.;2;분자식은 분자 내 원자의 종류와 수를 나타냅니다.;10\n" +
                    "양이온이란 무엇인가?;전자를 잃고 양전하를 띄는 이온이다.;전자를 얻고 음전하를 띄는 이온이다.;전자와 양성자의 수가 같은 이온이다.;전자가 없는 이온이다.;1;양이온은 원자가 전자를 잃어 양전하를 갖게 된 입자입니다.;250"; // Example output should also be in Korean if the generator communicates in Korean
        } else if (type == QuizType.SHORT_ANSWER) {
            return "You are an educational quiz generator who communicates in Korean. " +
                    "Generate **short answer questions** from the following lecture summary. " +
                    "Generate only as many questions as are necessary to cover the key conceptual points in the lecture summary. " +
                    "Each question must be based solely on the lecture content and be logically inferable from it—do not use external knowledge. " +
                    "Avoid questions that test rote memorization or trivial details. Focus on questions that assess **understanding and reasoning**.\n\n" +

                    "난이도는 다음 기준에 따라 부여하세요:\n" +
                    "- 난이도 1 (쉬움): 기본 개념, 정의, 용어를 직접적으로 묻는 문제\n" +
                    "- 난이도 2 (보통): 개념 간의 관계, 비교, 간단한 추론이나 응용을 요구하는 문제\n" +
                    "- 난이도 3 (어려움): 복합 개념 적용, 고차원적 사고, 간접적 추론을 포함한 문제\n" +
                    "- 난이도 1과 2가 모호하게 겹칠 경우 둘 중 하나는 생략해도 좋습니다.\n\n" +

                    "각 문항은 다음 형식으로 한 줄로 출력해주세요:\n" +
                    "[Question];[Correct Answer];[Explanation of the correct answer];[Timestamp in seconds];[Difficulty Level (1~3)]\n\n" +

                    "예시:\n" +
                    "적혈구의 주요 기능은 무엇인가?;몸 전체에 산소를 운반하는 것.;적혈구는 헤모글로빈을 함유하고 있어 폐에서 산소와 결합하여 몸 전체의 조직으로 운반합니다.;240;1\n" +
                    "파동함수의 절댓값 제곱이 물리적으로 어떤 의미를 가지며, 이를 통해 얻을 수 있는 예측은 무엇인가?;입자의 위치 확률 분포를 나타내며, 특정 구간에 존재할 확률을 계산할 수 있다.;파동함수의 절댓값 제곱은 해당 위치에서 입자를 발견할 확률 밀도를 의미하므로, 이를 통해 입자의 위치 예측이 가능합니다.;1802;2\n\n" +

                    "⚠️ Examples of what NOT to do (for reference only — do NOT copy these):\n" +
                    "- Bad (rote recall): 막스 보른은 슈레딩거 방정식에서 어떤 기여를 했는가?;파동 함수의 절댓값 제곱을 확률 해석에 도입했다.;막스 보른은 파동 함수의 물리적 의미를 확률적으로 해석하는 데 기여했습니다.;1802;1";
        }
        return "";
    }

    private String generateQuizSystemPromptByDifficulty(QuizType type) {
        if (type == QuizType.MULTIPLE_CHOICE) {
            return "You are an educational quiz generator who communicates in Korean. " +
                    "You will receive a Korean lecture summary and generate multiple-choice quizzes based strictly on its content. " +
                    "Each quiz question must be logically inferred only from the summary and not based on external knowledge. " +
                    "Consider the difficulty level of the lecture content and assign a difficulty score to each quiz you create. " +
                    "You may skip creating a question for a specific difficulty level (especially between 난이도 1 and 2) if the distinction is unclear.\n\n" +

                    "난이도 정의:\n" +
                    "- 난이도 1 (쉬움): 정의, 용어, 핵심 개념 등 기본적인 내용을 묻는 문제\n" +
                    "- 난이도 2 (보통): 개념 간 관계, 간단한 응용, 비교 등 논리적 이해를 요하는 문제\n" +
                    "- 난이도 3 (어려움): 복합적인 추론, 고차원적 사고, 깊이 있는 응용 문제\n\n" +

                    "각 문제는 정확하게 다음 형식으로 작성해주세요. 줄바꿈 없이 한 줄로 출력하며, 숫자나 항목 앞에는 불필요한 마크업 없이:\n" +
                    "[Question];[1. Option 1];[2. Option 2];[3. Option 3];[4. Option 4];[Answer Option Number];[Explanation of correct answer];[Timestamp in seconds];[Difficulty Level 1~3]\n\n" +

                    "예시:\n" +
                    "분자식은 무엇을 나타내는가?;원자의 성질;분자 내 원자의 종류와 수;물질의 상태;화학 반응의 결과;2;분자식은 분자 내 원자의 종류와 수를 나타냅니다.;10;1\n" +
                    "양이온과 음이온의 차이는 무엇인가?;양이온은 전자를 얻고, 음이온은 전자를 잃는다.;양이온은 음전하, 음이온은 양전하를 띈다.;양이온은 전자를 잃고, 음이온은 전자를 얻는다.;둘 다 전자를 얻는다.;3;양이온은 전자를 잃고 양전하를, 음이온은 전자를 얻고 음전하를 갖게 됩니다.;120;2\n" +
                    "어떤 조건에서 이온 결합이 잘 형성되는가?;같은 전기적 성질을 가질 때;공유 전자가 있을 때;전자 친화도와 이온화 에너지 차이가 클 때;원자의 크기가 같을 때;3;이온 결합은 전자를 쉽게 잃는 원자와 잘 받는 원자 사이에서 형성됩니다.;210;3";
        }
        else if (type == QuizType.SHORT_ANSWER) {
            return "You are an educational quiz generator who communicates in Korean. " +
                    "Generate **short answer questions** from the following lecture summary. " +
                    "Generate only as many questions as are necessary to cover the key conceptual points in the lecture summary. " +
                    "Each question must be based solely on the lecture content and be logically inferable from it—do not use external knowledge. " +
                    "Avoid questions that test rote memorization or trivial details. Focus on questions that assess **understanding and reasoning**.\n\n" +

                    "난이도는 다음 기준에 따라 부여하세요:\n" +
                    "- 난이도 1 (쉬움): 기본 개념, 정의, 용어를 직접적으로 묻는 문제\n" +
                    "- 난이도 2 (보통): 개념 간의 관계, 비교, 간단한 추론이나 응용을 요구하는 문제\n" +
                    "- 난이도 3 (어려움): 복합 개념 적용, 고차원적 사고, 간접적 추론을 포함한 문제\n" +
                    "- 난이도 1과 2가 모호하게 겹칠 경우 둘 중 하나는 생략해도 좋습니다.\n\n" +

                    "각 문항은 다음 형식으로 한 줄로 출력해주세요:\n" +
                    "[Question];[Correct Answer];[Explanation of the correct answer];[Timestamp in seconds];[Difficulty Level (1~3)]\n\n" +

                    "예시:\n" +
                    "적혈구의 주요 기능은 무엇인가?;몸 전체에 산소를 운반하는 것.;적혈구는 헤모글로빈을 함유하고 있어 폐에서 산소와 결합하여 몸 전체의 조직으로 운반합니다.;240;1\n" +
                    "파동함수의 절댓값 제곱이 물리적으로 어떤 의미를 가지며, 이를 통해 얻을 수 있는 예측은 무엇인가?;입자의 위치 확률 분포를 나타내며, 특정 구간에 존재할 확률을 계산할 수 있다.;파동함수의 절댓값 제곱은 해당 위치에서 입자를 발견할 확률 밀도를 의미하므로, 이를 통해 입자의 위치 예측이 가능합니다.;1802;2\n\n" +

                    "⚠️ Examples of what NOT to do (for reference only — do NOT copy these):\n" +
                    "- Bad (rote recall): 막스 보른은 슈레딩거 방정식에서 어떤 기여를 했는가?;파동 함수의 절댓값 제곱을 확률 해석에 도입했다.;막스 보른은 파동 함수의 물리적 의미를 확률적으로 해석하는 데 기여했습니다.;1802;1";
        }
        return "";
    }

    private static class LectureSummarySplitter {

        // 10분(600초) 단위로 청크를 나눔
        public static List<String> splitLectureSummary(String lectureSummary) {
            List<String> chunks = new ArrayList<>();
            if (lectureSummary == null || lectureSummary.isBlank()) return chunks;

            // 항목별 분리: <로 시작하는 항목
            String[] items = lectureSummary.split("(?=<)");
            // 정규표현식으로 time 추출
            Pattern pattern = Pattern.compile("^<([0-9]+);");

            // Map<청크번호, StringBuilder>
            Map<Integer, StringBuilder> chunkMap = new TreeMap<>();

            for (String item : items) {
                if (item.isBlank()) continue;
                Matcher matcher = pattern.matcher(item.trim());
                if (matcher.find()) {
                    int timeSec = Integer.parseInt(matcher.group(1));
                    int chunkIndex = timeSec / 600; // 10분 단위
                    chunkMap.computeIfAbsent(chunkIndex, k -> new StringBuilder()).append(item);
                }
            }

            // Map의 value를 순서대로 List로 변환
            for (StringBuilder sb : chunkMap.values()) {
                chunks.add(sb.toString());
            }
            return chunks;
        }
    }

    // --- Custom Exceptions for better error handling ---
    public static class OpenAiClientException extends RuntimeException {
        public OpenAiClientException(String message) {
            super(message);
        }
        public OpenAiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class OpenAiServerException extends RuntimeException {
        public OpenAiServerException(String message) {
            super(message);
        }
    }
    public static class OpenAiTimeoutException extends RuntimeException {
        public OpenAiTimeoutException(String message) {
            super(message);
        }
    }


}
