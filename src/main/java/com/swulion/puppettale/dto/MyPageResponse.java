package com.swulion.puppettale.dto;

import com.swulion.puppettale.constant.PuppetMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyPageResponse {
    // 이름
    private String name;

    // 나이
    private String age;

    // 입원 일수
    private String hospitalizationDays;

    // 퍼펫 이름
    private String puppetName;

    // 퍼펫 모드
    private PuppetMode puppetMode;

    // 프로필 이미지 URL, 추후 확장
    private String profileImageUrl;
}
