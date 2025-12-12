package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.ChatResponseDto;
import com.swulion.puppettale.dto.ChatStartRequestDto; // 사운드ID 포함 DTO로 통일
import com.swulion.puppettale.entity.ChatMessage;
import com.swulion.puppettale.service.ChatService;
import com.swulion.puppettale.service.DischargeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final DischargeService dischargeService;

    // 기능 명세 1.1.1, 1.2, 1.3 통합 엔드포인트
    // 모든 채팅 요청 (시작 대화, 일반 대화 모두)은 이 엔드포인트로 통합 처리
    // [POST] /api/chat/process
    @PostMapping("/process")
    public ResponseEntity<ChatResponseDto> processChat(@RequestBody ChatStartRequestDto requestDto, HttpServletRequest httpServletRequest) {
        String sessionId = Optional.ofNullable(requestDto.getSessionId()).orElse("default_user_session");

        if (requestDto.getUserMessage() == null || requestDto.getUserMessage().isEmpty()) {
            return ResponseEntity.badRequest().body(ChatResponseDto.builder()
                    .sessionId(sessionId)
                    .aiResponse("메시지를 입력해 주세요.")
                    .build());
        }
        ChatResponseDto response = chatService.processChat(requestDto);

        return ResponseEntity.ok(response);
    }

    // 특정 세션아이디의 대화 기록 조회
    // [GET] /api/chat/history/{sessionId}
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getSessionHistory(@PathVariable String sessionId) {
        try {
            List<ChatMessage> history = dischargeService.getConversationHistory(sessionId);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("대화 기록 조회 중 서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
}