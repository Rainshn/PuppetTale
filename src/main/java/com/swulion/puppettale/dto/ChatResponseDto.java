package com.swulion.puppettale.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatResponseDto {
    private String sessionId;
    private String aiResponse; // AI 퍼펫의 응답 메시지
    private String timestamp;  // 응답 시각
    private String currentSoundId;
    private String backgroundImageUrl;

    private String detectedEmotion;
    private List<StoryIngredientsDto> storyIngredientsDtoList;
}