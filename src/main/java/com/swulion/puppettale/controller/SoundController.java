package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.SoundOptionDto;
import com.swulion.puppettale.service.SoundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/sounds")
@RequiredArgsConstructor
@Slf4j
public class SoundController {

    private final SoundService soundService;

    // 클라이언트에게 사운드 옵션 목록을 제공하는 API
    // [GET] /api/sounds
    @GetMapping
    public List<SoundOptionDto> getSoundOptions() {
        return soundService.getSoundOptions();
    }
}