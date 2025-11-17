package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.MyPageResponse;
import com.swulion.puppettale.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children")
public class MyPageController {
    private final MyPageService myPageService;

    // 마이페이지 조회
    @GetMapping("/{childId}/mypage")
    public MyPageResponse getMyPage(@PathVariable Long childId) {
        return myPageService.getMyPage(childId);
    }
}