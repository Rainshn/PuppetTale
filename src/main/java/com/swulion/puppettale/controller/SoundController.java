package com.swulion.puppettale.controller;

import com.swulion.puppettale.dto.SoundOptionDto;
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
@Slf4j
public class SoundController {

    private static final String BASE_SOUND_URL = "https://cdn.example.com/sounds/";

    private static final List<SoundOptionDto> SOUND_OPTIONS = Arrays.asList(
            new SoundOptionDto("breeze", "잔잔한 바람소리", "현재 대화 배경은 잔잔한 바람 소리야.", BASE_SOUND_URL + "breeze.mp3"),
            new SoundOptionDto("amusement", "신나는 놀이공원", "현재 대화 배경은 신나는 놀이공원 소리야.", BASE_SOUND_URL + "amusement.mp3"),
            new SoundOptionDto("ocean", "시원한 바다", "현재 대화 배경은 시원한 바다 소리야.", BASE_SOUND_URL + "ocean.mp3"),
            new SoundOptionDto("none", "음악 없음", "현재 대화 배경은 조용하고 편안한 방이야.", null)
    );

    // 클라이언트에게 사운드 옵션 목록을 제공하는 API
    // [GET] /api/sounds
    @GetMapping
    public List<SoundOptionDto> getSoundOptions() {
        return SOUND_OPTIONS;
    }
}