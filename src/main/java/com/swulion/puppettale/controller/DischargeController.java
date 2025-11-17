package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.DischargeRequestDto;
import com.swulion.puppettale.entity.ChatMessage;
import com.swulion.puppettale.service.DischargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/discharge")
@RequiredArgsConstructor
public class DischargeController {

    private final DischargeService dischargeService;

    /*
     * 퇴원 버튼 클릭 시, 해당 세션의 모든 대화 기록을 조회하고 동화책 생성 절차를 시작
     * [POST] /api/discharge
     */
    @PostMapping
    public ResponseEntity<?> dischargeSession(@RequestBody DischargeRequestDto request) {
        try {
            // 1. 대화 기록 조회 및 동화책 생성 준비
            List<ChatMessage> history = dischargeService.getConversationHistory(request.getSessionId());

            // (실제 서비스에서는 성공 메시지나 동화책 ID를 반환.)
            return ResponseEntity.ok("퇴원 처리가 시작되었으며, 총 " + history.size() + "개의 메시지가 조회되었습니다.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("퇴원 처리 중 서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
}