package com.swulion.puppettale.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiChatJsonContentDto {
    @JsonProperty("thought_process")
    private ThoughtProcessDto thoughtProcess;

    private String response;
}