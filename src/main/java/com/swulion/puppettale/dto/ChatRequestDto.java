package com.swulion.puppettale.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequestDto {
    private String sessionId;   // 사용자 식별자
    private String userMessage; // 사용자가 입력한 메시지
}