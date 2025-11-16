package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.UpdatePuppetModeRequest;
import com.swulion.puppettale.dto.UpdatePuppetNameRequest;
import com.swulion.puppettale.service.PuppetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children/{childId}/puppet")
public class PuppetController {
    private final PuppetService puppetService;

    // 퍼펫 이름 변경
    @PatchMapping("/name")
    public ResponseEntity<?> updatePuppetName(
            @PathVariable Long childId,
            @Valid @RequestBody UpdatePuppetNameRequest request) {
        puppetService.updatePuppetName(childId, request);
        return ResponseEntity.ok("성공적으로 변경되었습니다.");
    }

    // 퍼펫 모드 변경
    @PatchMapping("/mode")
    public ResponseEntity<?> updatePuppetMode(
            @PathVariable Long childId,
            @Valid @RequestBody UpdatePuppetModeRequest request) {
        puppetService.updatePuppetMode(childId, request);
        return ResponseEntity.ok("성공적으로 변경되었습니다.");
    }
}
