package com.swulion.puppettale.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class GeminiChatRequestDto {
    private List<Content> contents;

    public GeminiChatRequestDto(List<Content> contents) {
        this.contents = contents;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @lombok.Builder
    public static class Content {
        private String role;
        private List<Part> parts;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Part {
        private String text;
    }
}