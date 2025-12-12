package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.*;
import com.swulion.puppettale.service.DischargeService;
import com.swulion.puppettale.service.FairyTaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children/{childId}/fairytales")
public class FairyTaleController {
    private final FairyTaleService fairyTaleService;
    private final DischargeService dischargeService;

    /*
     * 퇴원 버튼 클릭 시, 해당 세션의 모든 대화 기록을 조회하고 동화책 생성 절차를 시작
     * [POST] /api/children/{childId}/fairytales/create
     */
    @PostMapping("/create")
    public ResponseEntity<StoryCreationResponseDto> createFairyTale(
            @PathVariable Long childId,
            @RequestBody StoryCreationRequestDto requestDto
    ) {
        if (requestDto.getSessionId() == null || requestDto.getSessionId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    StoryCreationResponseDto.builder()
                            .createdStory("세션 ID가 누락되어 동화 생성을 요청할 수 없습니다.")
                            .build()
            );
        }

        // childId가 body에 없으면 path의 childId 적용
        if (requestDto.getChildId() == null) {
            requestDto = requestDto.toBuilder()
                    .childId(childId)
                    .build();
        }

        StoryCreationResponseDto response = dischargeService.createStory(requestDto);

        // 실패 문자열 탐지
        if (response.getCreatedStory() != null &&
                (response.getCreatedStory().contains("생성할 수 없습니다")
                        || response.getCreatedStory().contains("안전 문제")
                        || response.getCreatedStory().contains("분석 데이터"))) {
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

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
