package com.swulion.puppettale.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationRequestDto {
    private String prompt;
    private int width;
    private int height;
}
