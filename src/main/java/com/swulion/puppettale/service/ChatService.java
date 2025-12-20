package com.swulion.puppettale.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swulion.puppettale.constant.PuppetMode;
import com.swulion.puppettale.dto.*;
import com.swulion.puppettale.entity.ChatMessage;
import com.swulion.puppettale.entity.Speaker;
import com.swulion.puppettale.repository.ChatMessageRepository;
import com.swulion.puppettale.util.KoreanParticleUtil;
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
    private final SoundService soundService;
    private final PuppetService puppetService;
    private final ConcurrentMap<String, String> sessionSoundMap = new ConcurrentHashMap<>();
    private final FairyTaleService fairyTaleService;

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

    @Value("classpath:prompts/ai_systemPrompt_affectionate.txt")
    private Resource affectionatePromptResource;
    private String affectionatePromptTemplate;

    @Value("classpath:prompts/ai_systemPrompt_energetic.txt")
    private Resource energeticPromptResource;
    private String energeticPromptTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            // 공통 기반 템플릿
            this.aiSystemPromptTemplate = FileCopyUtils.copyToString(
                    new InputStreamReader(aiSystemPromptResource.getInputStream(), UTF_8));
            // 모드별
            this.affectionatePromptTemplate = FileCopyUtils.copyToString(
                    new InputStreamReader(affectionatePromptResource.getInputStream(), UTF_8));
            this.energeticPromptTemplate = FileCopyUtils.copyToString(
                    new InputStreamReader(energeticPromptResource.getInputStream(), UTF_8));
        } catch (IOException e){
            this.aiSystemPromptTemplate = "프롬프트 로드 실패";
        }
    }

    private final ConcurrentMap<String, Long> lastRequestTimeMap = new ConcurrentHashMap<>();

    @Transactional
    public ChatResponseDto processChat(ChatStartRequestDto request) { // DTO를 ChatStartRequestDto로 통일 (soundId 포함)
        String sessionId = request.getSessionId();
        long currentTime = System.currentTimeMillis();

        // 중복 전송 방지 로직 (2초 이내 동일 세션 요청 차단)
        if (lastRequestTimeMap.containsKey(sessionId)) {
            long lastTime = lastRequestTimeMap.get(sessionId);
            if (currentTime - lastTime < 2000) { // 2초 간격
                log.warn("중복 채팅 요청 차단: sessionId={}", sessionId);
                return ChatResponseDto.builder().aiResponse("생각 중이야! 잠시만 기다려줘.").build();
            }
        }
        lastRequestTimeMap.put(sessionId, currentTime);

        Long childId = (request.getChildId() != null) ? request.getChildId() : 1L;
        PuppetMode currentMode = puppetService.getPuppetMode(childId);

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
                userName, userAge, userConstraint, puppetName, currentMode, childId);
        String finalSoundId = sessionSoundMap.getOrDefault(sessionId, "none");
        String backgroundUrl = soundService.getBackgroundImageUrl(finalSoundId);

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
                                 String userName, Integer userAge, String userConstraint,
                                 String puppetName, PuppetMode puppetMode, Long childId) {
        String currentSoundId = Optional.ofNullable(soundId).orElse("").toLowerCase();

        // soundId가 명시적으로 제공된 경우에만 맵을 업데이트
        if (!currentSoundId.isEmpty() && !currentSoundId.equals("null")) {
            sessionSoundMap.put(sessionId, currentSoundId);
        } else {
            // soundId가 없으면 (null이거나 "none"이거나 "null" 문자열이면), 맵에 저장된 이전 값을 사용
            currentSoundId = sessionSoundMap.getOrDefault(sessionId, "none");
        }

        // 사운드 컨텍스트와 기본 페르소나를 결합하여 최종 시스템 명령 생성
        String soundContext = soundService.getAiContext(currentSoundId); // 결정된 soundId 사용

        // 모드에 따른 지침 선택
        String modeInstruction = (puppetMode == PuppetMode.ENERGETIC) ? energeticPromptTemplate : affectionatePromptTemplate;
        String combinedConstraint = modeInstruction + "\n(추가 제약사항: " + userConstraint + ")";

        String systemInstruction = this.aiSystemPromptTemplate
                .replace("{Puppet_Name}", puppetName)
                .replace("{User_Name}", userName)
                .replace("{User_Age}", userAge != null ? userAge.toString() : "7")
                .replace("{User_Constraint}", combinedConstraint)
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
                    log.error("Gemini Raw Response Body: {}", jsonText);

                    GeminiApiResponse apiResponse = objectMapper.readValue(jsonText, GeminiApiResponse.class);
                    String innerJsonText = apiResponse.getCandidates().get(0)
                            .getContent().getParts().get(0).getText();

                    if (innerJsonText.contains("```json")) {
                        innerJsonText = innerJsonText.substring(innerJsonText.indexOf("```json") + 7);
                        innerJsonText = innerJsonText.substring(0, innerJsonText.lastIndexOf("```"));
                    } else if (innerJsonText.contains("```")) {
                        innerJsonText = innerJsonText.substring(innerJsonText.indexOf("```") + 3);
                        innerJsonText = innerJsonText.substring(0, innerJsonText.lastIndexOf("```"));
                    }
                    innerJsonText = innerJsonText.trim();

//                    GeminiChatJsonContentDto geminiResult = objectMapper.readValue(innerJsonText, GeminiChatJsonContentDto.class);
//                    String safetyStatus = geminiResult.getThoughtProcess().getSafetyStatus();
//                    String finalResponse = geminiResult.getResponse();

                    String finalResponse = "";
                    String safetyStatus = "GREEN";

                    try {
                        // JSON 형식인 경우에만 파싱 시도
                        if (innerJsonText.startsWith("{")) {
                            GeminiChatJsonContentDto geminiResult = objectMapper.readValue(innerJsonText, GeminiChatJsonContentDto.class);
                            safetyStatus = geminiResult.getThoughtProcess().getSafetyStatus();
                            finalResponse = geminiResult.getResponse();
                        } else {
                            // JSON이 아니면(평문이면) 받은 텍스트 그대로를 응답으로 간주
                            log.warn("Gemini가 JSON 형식을 지키지 않음. 평문 응답 처리: {}", innerJsonText);
                            finalResponse = innerJsonText;
                        }
                    } catch (Exception e) {
                        // 파싱 에러 발생 시 평문으로 치환
                        log.error("JSON 파싱 에러 발생, 평문으로 전환: {}", e.getMessage());
                        finalResponse = innerJsonText;
                    }

                    if ("RED_FLAG".equalsIgnoreCase(safetyStatus)) {
                        if (finalResponse == null || finalResponse.isBlank()) {
                            finalResponse = "저런, 그건 너무 위험하고 무서운 생각이야. " +
                                    puppetName + KoreanParticleUtil.eunNeun(puppetName) + " " +
                                    userName + KoreanParticleUtil.iGa(userName) + " 너무 소중해서, " +
                                    "이 이야기는 보호자님께 꼭 전해야겠어.";
                        }
                    } else if ("MEDICAL".equalsIgnoreCase(safetyStatus)) {
                        if (finalResponse == null || finalResponse.isBlank()) {
                            finalResponse =
                                    userName + KoreanParticleUtil.eunNeun(userName) +
                                            " 많이 아프구나. 의사 선생님이 도와줄 수 있게 말씀드려볼까? " +
                                            "주위에 보호자가 계실까?";
                        }
                    }

                    if (finalResponse != null) {
                        finalResponse = finalResponse
                                .replace("\n", " ")
                                .replace("\r", " ")
                                .replaceAll("\\s{2,}", " ")
                                .trim();
                    }

//                    geminiResult.setResponse(finalResponse);
//                    return geminiResult.getResponse();
                    if (finalResponse == null || finalResponse.isBlank()) {
                        finalResponse = "미안해, 토리가 잠시 생각을 정리하고 있어! 다시 한번 말해줄래?";
                    }
                    return finalResponse;
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