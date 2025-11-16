package com.swulion.puppettale.service;

import com.swulion.puppettale.dto.ChatRequestDto;
import com.swulion.puppettale.dto.ChatResponseDto;
import com.swulion.puppettale.dto.GeminiChatRequestDto;
import com.swulion.puppettale.dto.GeminiChatResponseDto;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final RestTemplate restTemplate;

    // application.properties에서 API Key 주입
    @Value("${api.key.gemini}")
    private String apiKey;

    // API URL 및 모델 설정
    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=";

    private static final String SYSTEM_INSTRUCTION =
            "너는 친절하고 호기심 많은 AI 퍼펫이야. 사용자는 아동이며, 너는 아동과 대화하면서 나중에 이 대화 내용을 바탕으로 재미있는 동화를 만들어 줄 거야. " +
                    "아동의 상상력을 자극하고 긍정적인 대화를 유도해. " +
                    "사용자에게 명확하고 쉬운 한국어로 답변하되, **과도한 감탄사나 불필요한 이모지(Emoji) 사용은 자제하고 차분하고 친근한 말투로 응답해.**";

    @Transactional
    public ChatResponseDto processChat(ChatRequestDto request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getUserMessage();
        LocalDateTime now = LocalDateTime.now();

        // 사용자 메시지 저장 (AI 호출 전 DB에 저장)
        saveMessage(sessionId, Speaker.USER, userMessage, now);

        // AI 응답 생성 (대화 맥락을 포함하여 Gemini 호출)
        String aiResponse = getAiResponse(sessionId, userMessage);

        // AI 응답 메시지 저장
        LocalDateTime aiResponseTime = LocalDateTime.now();
        saveMessage(sessionId, Speaker.AI, aiResponse, aiResponseTime);

        // 응답 DTO 생성
        return ChatResponseDto.builder()
                .sessionId(sessionId)
                .aiResponse(aiResponse)
                .timestamp(aiResponseTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private String getAiResponse(String sessionId, String userMessage) {

        // 이전 대화 기록 조회 (시간 순)
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        // Contents 리스트 구성
        List<GeminiChatRequestDto.Content> contents = new ArrayList<>();

        // System Instruction을 첫 번째 Content로 추가
        // 사용자 역할(user)로 첫 메시지에 시스템 명령어를 포함시켜 페르소나를 부여
        contents.add(
                GeminiChatRequestDto.Content.builder()
                        .role("user")
                        .parts(List.of(new GeminiChatRequestDto.Part(SYSTEM_INSTRUCTION))) // 시스템 명령어 삽입
                        .build()
        );

        // 이전 대화 기록을 Gemini API 형식으로 변환하여 추가
        for (ChatMessage log : history) {
            // 이전에 시스템 명령어를 넣었으므로, 이전 대화 기록은 'model'과 'user' 역할만 번갈아 나타남
            String role = log.getSpeaker() == Speaker.USER ? "user" : "model";

            contents.add(
                    GeminiChatRequestDto.Content.builder()
                            .role(role)
                            .parts(List.of(new GeminiChatRequestDto.Part(log.getMessage())))
                            .build()
            );
        }

        // 현재 사용자 메시지를 마지막 Content로 추가
        contents.add(
                GeminiChatRequestDto.Content.builder()
                        .role("user")
                        .parts(List.of(new GeminiChatRequestDto.Part(userMessage)))
                        .build()
        );

        // 요청 DTO 생성 (이제 contents 리스트만 전달)
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
        // 1.3.1 (하루 단위 DB 저장) 요구사항 충족
        ChatMessage chatLog = ChatMessage.builder()
                .sessionId(sessionId)
                .speaker(speaker)
                .message(message)
                .timestamp(timestamp)
                .logDate(timestamp.toLocalDate())
                .keywords(null) // 추후 구현 예정
                .build();

        chatMessageRepository.save(chatLog);
    }
}