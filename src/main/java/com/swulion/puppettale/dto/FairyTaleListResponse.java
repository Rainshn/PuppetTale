package com.swulion.puppettale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FairyTaleListResponse {
    // 동화 목록
    private List<FairyTaleListItem> fairyTales;
}
