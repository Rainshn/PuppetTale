package com.swulion.puppettale.service;

import com.swulion.puppettale.dto.*;
import com.swulion.puppettale.entity.ChatMessage;
import com.swulion.puppettale.entity.Speaker;
import com.swulion.puppettale.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    private static final String AI_PERROT_PROMPT =
            "너는 친절하고 호기심 많은 AI 퍼펫이야. 사용자는 아동이며, 너는 아동과 대화하면서 나중에 이 대화 내용을 바탕으로 재미있는 동화를 만들어 줄 거야. " +
                    "아동의 상상력을 자극하고 긍정적인 대화를 유도해. " +
                    "사용자에게 명확하고 쉬운 한국어로 답변하되, 과도한 감탄사나 불필요한 이모지(Emoji) 사용은 자제하고 차분하고 친근한 말투로 응답해.";

    private static final List<SoundOptionDto> SOUND_OPTIONS = Arrays.asList(
            new SoundOptionDto("sea", "잔잔한 바닷소리", "현재 대화 배경은 잔잔한 바닷가 소리야."),
            new SoundOptionDto("forest", "평온한 숲", "현재 대화 배경은 평온한 숲 소리야."),
            new SoundOptionDto("ocean", "시원한 바다", "현재 대화 배경은 시원한 바다 소리야."),
            new SoundOptionDto("none", "음악 없음", "현재 대화 배경은 조용하고 편안한 방이야.")
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

        LocalDateTime now = LocalDateTime.now();

        // 1. 사용자 메시지 저장
        saveMessage(sessionId, Speaker.USER, userMessage, now);

        // 2. AI 응답 생성 (soundId와 함께 Gemini 호출)
        String aiResponse = callGeminiApi(sessionId, userMessage, soundId);
        String finalSoundId = sessionSoundMap.getOrDefault(sessionId, "none");

        // 3. AI 응답 메시지 저장
        LocalDateTime aiResponseTime = LocalDateTime.now();
        saveMessage(sessionId, Speaker.AI, aiResponse, aiResponseTime);

        // 4. 응답 DTO 생성
        return ChatResponseDto.builder()
                .sessionId(sessionId)
                .aiResponse(aiResponse)
                .timestamp(aiResponseTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .currentSoundId(finalSoundId)
                .build();
    }

    private String callGeminiApi(String sessionId, String userMessage, String soundId) {
        log.info("DEBUG MAP: Current sessionSoundMap state for {}: {}", sessionId, sessionSoundMap.get(sessionId));
        String currentSoundId = Optional.ofNullable(soundId).orElse("").toLowerCase();

        // soundId가 명시적으로 제공된 경우에만 맵을 업데이트
        if (!currentSoundId.isEmpty() && !currentSoundId.equals("none") && !currentSoundId.equals("null")) {
            log.info("DEBUG MAP: Setting new soundId {} for session {}", currentSoundId, sessionId);
            sessionSoundMap.put(sessionId, currentSoundId);
        } else {
            // soundId가 없으면 (null이거나 "none"이거나 "null" 문자열이면), 맵에 저장된 이전 값을 사용
            currentSoundId = sessionSoundMap.getOrDefault(sessionId, "none");
            log.info("DEBUG MAP: Reverting to stored soundId {} for session {}", currentSoundId, sessionId);
        }

        // 사운드 컨텍스트와 기본 페르소나를 결합하여 최종 시스템 명령 생성
        String soundContext = getAiContext(currentSoundId); // 결정된 soundId 사용
        String finalSystemInstruction = soundContext + "\n\n" + AI_PERROT_PROMPT;

        // 이전 대화 기록 조회 (시간 순)
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        // Contents 리스트 구성
        List<GeminiChatRequestDto.Content> contents = new ArrayList<>();

        // 1. System Instruction을 첫 번째 Content로 추가
        contents.add(
                GeminiChatRequestDto.Content.builder()
                        .role("user")
                        .parts(List.of(new GeminiChatRequestDto.Part(finalSystemInstruction)))
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

        try {
            // RestTemplate으로 API 호출
            ResponseEntity<GeminiChatResponseDto> response = restTemplate.postForEntity(
                    apiUrlWithKey, entity, GeminiChatResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String aiText = response.getBody().getFirstText();

                // 줄 바꿈 문자 제거 및 공백 치환
                if (aiText != null) {
                    aiText = aiText.replace("\n", " ").replace("\r", " ").trim();
                }
                return aiText;
            }

            log.warn("Gemini API가 200이 아닌 응답 반환: {}", response.getStatusCode());
            return "AI 연결 문제 발생. 응답 코드: " + response.getStatusCode();

        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage(), e);
            return "AI 서버와 통신 중 오류";
        }
    }

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