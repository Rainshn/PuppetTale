package com.swulion.puppettale.dto;

import com.swulion.puppettale.constant.PuppetMode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePuppetModeRequest {
    @NotNull(message = "변경할 모드를 선택해 주세요.")
    private PuppetMode puppetMode;
}
