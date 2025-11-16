package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.FairyTaleDetailResponse;
import com.swulion.puppettale.dto.FairyTaleListResponse;
import com.swulion.puppettale.dto.UpdateFairyTaleTitleRequest;
import com.swulion.puppettale.service.FairyTaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children/{childId}/fairytales")
public class FairyTaleController {
    private final FairyTaleService fairyTaleService;

    // 동화 목록 조회
    @GetMapping
    public FairyTaleListResponse getFairyTaleList(@PathVariable Long childId) {
        return fairyTaleService.getFairyTaleList(childId);
    }

    // 동화 상세 조회
    @GetMapping("/{fairyTaleId}")
    public FairyTaleDetailResponse getFairyTaleDetail(
            @PathVariable Long childId,
            @PathVariable Long fairyTaleId
    ) {
        return fairyTaleService.getFairyTaleDetail(childId, fairyTaleId);
    }

    // 동화 제목 수정
    @PatchMapping("/{fairyTaleId}")
    public ResponseEntity<?> updateFairyTaleTitle(
            @PathVariable Long childId,
            @PathVariable Long fairyTaleId,
            @RequestBody UpdateFairyTaleTitleRequest request
    ) {
        fairyTaleService.updateFairyTaleTitle(childId, fairyTaleId, request);
        return ResponseEntity.ok("성공적으로 수정되었습니다.");
    }

    // 동화 삭제
    @DeleteMapping("/{fairyTaleId}")
    public ResponseEntity<?> deleteFairyTale(
            @PathVariable Long childId,
            @PathVariable Long fairyTaleId
    ) {
        fairyTaleService.deleteFairyTale(childId, fairyTaleId);
        return ResponseEntity.ok("성공적으로 삭제되었습니다.");
    }
}
