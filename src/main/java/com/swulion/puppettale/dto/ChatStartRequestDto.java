package com.swulion.puppettale.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatStartRequestDto {
    private String sessionId;
    private String userMessage;
    private String soundId;
}