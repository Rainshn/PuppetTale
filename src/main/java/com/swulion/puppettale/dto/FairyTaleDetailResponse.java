package com.swulion.puppettale.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FairyTaleDetailResponse {
    // 동화 ID
    private Long id;

    // 제목
    private String title;

    // 썸네일 이미지 URL
    private String thumbnailUrl;

    // 페이지 리스트
    private List<FairyTalePageInfo> pages;

    @Getter
    @Builder
    public static class FairyTalePageInfo {
        // 페이지 번호
        private Integer pageNumber;

        // 이미지 URL
        private String imageUrl;

        // 페이지 내용
        private String text;
    }
}
