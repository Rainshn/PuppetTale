package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.ChatResponseDto;
import com.swulion.puppettale.dto.ChatStartRequestDto; // 사운드ID 포함 DTO로 통일
import com.swulion.puppettale.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

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
}