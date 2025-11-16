package com.swulion.puppettale.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SoundOptionDto {
    private String id;
    private String name;
    private String aiContext; // AI에게 전달할 배경 설명
}