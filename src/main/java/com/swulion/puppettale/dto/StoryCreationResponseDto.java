package com.swulion.puppettale.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

// 동화 생성 API의 최종 응답 DTO
@Getter
@Builder
public class StoryCreationResponseDto {
    private String sessionId;
    private String createdStory;
    private String detectedEmotion;
    private List<StoryIngredientsDto> storyIngredients;
    private Long fairyTaleId;
}