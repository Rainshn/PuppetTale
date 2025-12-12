package com.swulion.puppettale.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DischargeRequestDto {
    private String sessionId; // 퇴원 처리할 세션 ID
}