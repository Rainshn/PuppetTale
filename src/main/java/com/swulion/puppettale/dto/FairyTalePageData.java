package com.swulion.puppettale.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FairyTalePageData {
    private int pageNumber;
    private String text;
    private String imagePrompt;
    private String imageUrl;
}
