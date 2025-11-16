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

    private static final List<SoundOptionDto> SOUND_OPTIONS = Arrays.asList(
            new SoundOptionDto("sea", "잔잔한 바닷소리", "지금 배경은 잔잔한 바닷가 소리야."),
            new SoundOptionDto("forest", "평온한 숲", "지금 배경은 아이들이 노는 놀이터 소리야."),
            new SoundOptionDto("ocean", "시원한 바다", "지금 배경은 길거리 버스킹 소리야."),
            new SoundOptionDto("none", "음악 없음", "지금 배경은 조용하고 편안한 방이야.")
    );

    // 클라이언트에게 사운드 옵션 목록을 제공하는 API
    // [GET] /api/sounds
    @GetMapping
    public List<SoundOptionDto> getSoundOptions() {
        return SOUND_OPTIONS;
    }

    // 사운드 파일을 스트리밍하여 재생할 수 있게 해주는 API
    // [GET] /api/sounds/{soundId}/play
    @GetMapping("/{soundId}/play")
    public ResponseEntity<Resource> playSound(@PathVariable String soundId) throws IOException {

        // 'none' 옵션은 파일이 없으므로, 처리하지 않음
        if ("none".equals(soundId)) {
            return ResponseEntity.badRequest().body(null);
        }
        Path currentPath = Paths.get("").toAbsolutePath();
        Path staticPath = Paths.get(currentPath.toString(), "src", "main", "resources", "static", "sounds");
        File file = staticPath.resolve(soundId + ".mp3").toFile();

        if (!file.exists()) {
            log.warn("파일 존재하지 않음: {}", file.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                // 오디오 파일 타입 설정
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()))
                .body(resource);
    }
}