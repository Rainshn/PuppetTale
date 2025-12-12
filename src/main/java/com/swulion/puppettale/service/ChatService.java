package com.swulion.puppettale.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swulion.puppettale.dto.*;
import com.swulion.puppettale.entity.ChatMessage;
import com.swulion.puppettale.entity.Speaker;
import com.swulion.puppettale.repository.ChatMessageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.core.io.Resource;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final RestTemplate restTemplate;
    private final ConcurrentMap<String, String> sessionSoundMap = new ConcurrentHashMap<>();

    // application.properties에서 API Key 주입
    @Value("${api.key.gemini}")
    private String apiKey;

    // API URL 및 모델 설정
    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=";

    // 리소스 파일 주입
    @Value("classpath:prompts/ai_systemPrompt_template.txt")
    private Resource aiSystemPromptResource;
    private String aiSystemPromptTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try (Reader reader = new InputStreamReader(aiSystemPromptResource.getInputStream(), UTF_8)) {
            this.aiSystemPromptTemplate = FileCopyUtils.copyToString(reader);
        } catch (IOException e){
            this.aiSystemPromptTemplate = "프롬프트 로드 실패";
        }
    }

    private static final List<SoundOptionDto> SOUND_OPTIONS = Arrays.asList(
            new SoundOptionDto("breeze", "잔잔한 바람소리", "현재 대화 배경은 잔잔한 바람 소리야."),
            new SoundOptionDto("amusement", "신나는 놀이공원", "현재 대화 배경은 신나는 놀이공원 소리야."),
            new SoundOptionDto("ocean", "시원한 바다", "현재 대화 배경은 시원한 바다 소리야."),
            new SoundOptionDto("none", "음악 없음", "현재 대화 배경은 조용하고 편안한 방이야.")
    );

    private static final Map<String, String> SOUND_IMAGE_MAP = Map.of(
            "breeze", "images/breeze.png",
            "amusement", "images/amusement.png",
            "ocean", "images/ocean.png",
            "none", "images/none.png"
    );

    // 사운드 ID에 맞는 AI 컨텍스트를 찾아주는 헬퍼 메서드
    private String getAiContext(String soundId) {
        return SOUND_OPTIONS.stream()
                .filter(option -> option.getId().equalsIgnoreCase(soundId))
                .findFirst()
                .map(SoundOptionDto::getAiContext)
                .orElse(SOUND_OPTIONS.get(3).getAiContext()); // 기본값은 'none'
    }

    @Transactional
    public ChatResponseDto processChat(ChatStartRequestDto request) { // DTO를 ChatStartRequestDto로 통일 (soundId 포함)
        String sessionId = request.getSessionId();
        String userMessage = request.getUserMessage();
        String soundId = Optional.ofNullable(request.getSoundId()).orElse("none"); // soundId가 없으면 "none" 사용

        String userName = Optional.ofNullable(request.getUserName()).orElse("아기사자");
        Integer userAge = request.getUserAge();
        String userConstraint = Optional.ofNullable(request.getUserConstraint()).orElse("없음");
        String puppetName = Optional.ofNullable(request.getPuppetName()).orElse("토리");

        LocalDateTime now = LocalDateTime.now();

        // 1. 사용자 메시지 저장
        saveMessage(sessionId, Speaker.USER, userMessage, now);

        // 2. AI 응답 생성 (soundId와 함께 Gemini 호출)
        String aiResponse = callGeminiApi(sessionId, userMessage, soundId,
                userName, userAge, userConstraint, puppetName);
        String finalSoundId = sessionSoundMap.getOrDefault(sessionId, "none");
        String backgroundUrl = SOUND_IMAGE_MAP.getOrDefault(finalSoundId, SOUND_IMAGE_MAP.get("none"));

        // 3. AI 응답 메시지 저장
        LocalDateTime aiResponseTime = LocalDateTime.now();
        saveMessage(sessionId, Speaker.AI, aiResponse, aiResponseTime);

        // 4. 응답 DTO 생성
        return ChatResponseDto.builder()
                .sessionId(sessionId)
                .aiResponse(aiResponse)
                .timestamp(aiResponseTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .currentSoundId(finalSoundId)
                .backgroundImageUrl(backgroundUrl)
                .build();
    }

    private String callGeminiApi(String sessionId, String userMessage, String soundId,
                                 String userName, Integer userAge, String userConstraint, String puppetName) {
        String currentSoundId = Optional.ofNullable(soundId).orElse("").toLowerCase();

        // soundId가 명시적으로 제공된 경우에만 맵을 업데이트
        if (!currentSoundId.isEmpty() && !currentSoundId.equals("null")) {
            sessionSoundMap.put(sessionId, currentSoundId);
        } else {
            // soundId가 없으면 (null이거나 "none"이거나 "null" 문자열이면), 맵에 저장된 이전 값을 사용
            currentSoundId = sessionSoundMap.getOrDefault(sessionId, "none");
        }

        // 사운드 컨텍스트와 기본 페르소나를 결합하여 최종 시스템 명령 생성
        String soundContext = getAiContext(currentSoundId); // 결정된 soundId 사용

        String systemInstruction = this.aiSystemPromptTemplate
                .replace("{Puppet_Name}", puppetName)
                .replace("{User_Name}", userName)
                .replace("{User_Age}", userAge != null ? userAge.toString() : "7")
                .replace("{User_Constraint}", userConstraint)
                + "\n\n" + soundContext;

        // 이전 대화 기록 조회 (시간 순)
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        // Contents 리스트 구성
        List<GeminiChatRequestDto.Content> contents = new ArrayList<>();

        // 1. System Instruction을 첫 번째 Content로 추가
        contents.add(
                GeminiChatRequestDto.Content.builder()
                        .role("user")
                        .parts(List.of(new GeminiChatRequestDto.Part(systemInstruction)))
                        .build()
        );

        // 2. 이전 대화 기록을 Gemini API 형식으로 변환하여 추가
        for (ChatMessage log : history) {
            String role = log.getSpeaker() == Speaker.USER ? "user" : "model";
            contents.add(
                    GeminiChatRequestDto.Content.builder()
                            .role(role)
                            .parts(List.of(new GeminiChatRequestDto.Part(log.getMessage())))
                            .build()
            );
        }

        // 3. 현재 사용자 메시지를 마지막 Content로 추가
        contents.add(
                GeminiChatRequestDto.Content.builder()
                        .role("user")
                        .parts(List.of(new GeminiChatRequestDto.Part(userMessage)))
                        .build()
        );

        // 요청 DTO 생성
        GeminiChatRequestDto requestBody = new GeminiChatRequestDto(contents);

        // Header 및 Entity 준비
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeminiChatRequestDto> entity = new HttpEntity<>(requestBody, headers);
        String apiUrlWithKey = GEMINI_API_URL + apiKey;

        final int MAX_RETRIES = 3;
        final long WAIT_TIME_MS = 1000; // 1초 대기

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // RestTemplate으로 API 호출
                ResponseEntity<String> response = restTemplate.postForEntity(
                        apiUrlWithKey, entity, String.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String jsonText = response.getBody();
                    GeminiApiResponse apiResponse = objectMapper.readValue(jsonText, GeminiApiResponse.class);

                    String innerJsonText = apiResponse.getCandidates().get(0)
                            .getContent().getParts().get(0).getText();

                    if (innerJsonText.startsWith("```")) {
                        innerJsonText = innerJsonText.replaceAll("^```json", "")
                                .replaceAll("^```", "")
                                .replaceAll("```$", "");
                    }
                    innerJsonText = innerJsonText.trim();

                    GeminiChatJsonContentDto geminiResult = objectMapper.readValue(innerJsonText, GeminiChatJsonContentDto.class);

                    String safetyStatus = geminiResult.getThoughtProcess().getSafetyStatus();
                    String finalResponse = geminiResult.getResponse();

                    if ("RED_FLAG".equalsIgnoreCase(safetyStatus)) {
                        finalResponse = "저런, 그건 너무 위험하고 무서운 생각이야. " +
                                puppetName + "는 " + userName + "이 너무 소중해서, 이 이야기는 보호자님께 꼭 전해야겠어.";
                    } else if ("MEDICAL".equalsIgnoreCase(safetyStatus)) {
                        finalResponse = userName + " 많이 아프구나. 의사 선생님이 도와줄 수 있게 말씀드려볼까? 주위에 보호자가 계실까?";
                    }

                    if (finalResponse != null) {
                        finalResponse = finalResponse.replace("\n", " ").replace("\r", " ").trim();
                    }

                    geminiResult.setResponse(finalResponse);
                    return geminiResult.getResponse();
                }

                // 200번대가 아닌 응답이지만 5xx 오류는 아닌 경우
                return "AI 연결 문제 발생. 응답 코드: " + response.getStatusCode();

            } catch (HttpServerErrorException.ServiceUnavailable e) {
                // 503 Service Unavailable 오류 발생 시 재시도 처리
                log.warn("Gemini 서버 일시적 장애 (503), 재시도 {}/{}", attempt, MAX_RETRIES); // 로그 추가
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(WAIT_TIME_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return "AI 서버 통신 중 대기 실패";
                    }
                } else {
                    return "AI 서버와 통신 중 오류: 지속적인 서버 과부하";
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // 400, 401, 429 등의 에러가 발생했을 때 상세 이유
                log.error("Gemini API 호출 에러 (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());

                return "AI 연결 오류: " + e.getStatusText() + " (" + e.getStatusCode() + ")";
            } catch (Exception e) {
                // 그 외 알 수 없는 에러
                log.error("Gemini API 알 수 없는 에러 발생", e);
                return "AI 서버와 통신 중 오류 (로그 확인 필요)";
            }
        }
        return "AI 서버와 통신 중 오류: 알 수 없는 이유로 실패";
    }

    @lombok.Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiApiResponse { private List<Candidate> candidates; }

    @lombok.Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate { private Content content; }

    @lombok.Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content { private List<Part> parts; }

    @lombok.Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part { private String text; }

    private void saveMessage(String sessionId, Speaker speaker, String message, LocalDateTime timestamp) {
        ChatMessage chatLog = ChatMessage.builder()
                .sessionId(sessionId)
                .speaker(speaker)
                .message(message)
                .timestamp(timestamp)
                .logDate(timestamp.toLocalDate())
                .keywords(null)
                .build();

        chatMessageRepository.save(chatLog);
    }
}