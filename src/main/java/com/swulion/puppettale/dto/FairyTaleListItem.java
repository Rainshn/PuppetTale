package com.swulion.puppettale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FairyTaleListItem {
    // 동화 ID
    private Long id;

    // 제목
    private String title;

    // 썸네일 이미지 URL
    private String thumbnailUrl;

    // 생성 시각
    private String createdAt;
}
