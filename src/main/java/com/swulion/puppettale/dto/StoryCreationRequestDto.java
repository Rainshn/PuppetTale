package com.swulion.puppettale.dto;

import lombok.*;

@Builder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StoryCreationRequestDto {
    private String sessionId;
    private Long childId;
    private String userName;
    private Integer userAge;
    private String puppetName; // 기본값: 토리
}