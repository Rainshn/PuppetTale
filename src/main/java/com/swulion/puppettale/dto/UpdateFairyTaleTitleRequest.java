package com.swulion.puppettale.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateFairyTaleTitleRequest {
    @NotBlank(message = "변경할 제목을 입력해 주세요.")
    private String title;
}
