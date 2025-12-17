package com.swulion.puppettale.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FairyTalePageData {
    private int pageNumber;
    private String text;
    @JsonIgnore
    private String imagePrompt;
    private String imageUrl;
}
