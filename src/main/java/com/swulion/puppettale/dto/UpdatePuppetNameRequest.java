package com.swulion.puppettale.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
public class UpdatePuppetNameRequest {
    @NotBlank(message = "변경할 이름을 입력해 주세요.")
    private String puppetName;
}