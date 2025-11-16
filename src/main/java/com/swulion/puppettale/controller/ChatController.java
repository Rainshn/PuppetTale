package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.ChatRequestDto;
import com.swulion.puppettale.dto.ChatResponseDto;
import com.swulion.puppettale.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 기능명세서 1.1.1 - 텍스트 채팅
    // [POST] /api/chat/send
    @PostMapping("/send")
    public ResponseEntity<ChatResponseDto> sendMessage(@RequestBody ChatRequestDto chatRequestDto) {
        if (chatRequestDto.getSessionId() == null || chatRequestDto.getUserMessage() == null || chatRequestDto.getUserMessage().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ChatResponseDto response = chatService.processChat(chatRequestDto);

        return ResponseEntity.ok(response);
    }


    // TODO: 대화 기록 조회 (GET /api/chat/logs/{sessionId}) 등의 API를 추가할 수 있습니다.
}