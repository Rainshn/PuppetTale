package com.swulion.puppettale.service;

import com.swulion.puppettale.dto.SoundOptionDto;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class SoundService {

    // S3 URl
    private static final String BASE_SOUND_URL = "https://puppettale-images.s3.ap-northeast-2.amazonaws.com/sounds/";
    private static final String DEFAULT_SOUND_ID = "none";

    private static final List<SoundOptionDto> SOUND_OPTIONS = Arrays.asList(
            new SoundOptionDto("breeze", "잔잔한 바람소리", "현재 대화 배경은 잔잔한 바람 소리야.", BASE_SOUND_URL + "breeze.mp3"),
            new SoundOptionDto("amusement", "신나는 놀이공원", "현재 대화 배경은 신나는 놀이공원 소리야.", BASE_SOUND_URL + "amusement.mp3"),
            new SoundOptionDto("ocean", "시원한 바다", "현재 대화 배경은 시원한 바다 소리야.", BASE_SOUND_URL + "ocean.mp3"),
            new SoundOptionDto("none", "음악 없음", "현재 대화 배경은 조용하고 편안한 방이야.", null)
    );

    // AI 채팅방 배경 이미지 URL
    private static final Map<String, String> SOUND_IMAGE_MAP = Map.of(
            "breeze", "images/breeze.png",
            "amusement", "images/amusement.png",
            "ocean", "images/ocean.png",
            "none", "images/none.png"
    );

    // 사운드 옵션 목록 제공
    public List<SoundOptionDto> getSoundOptions() {
        return SOUND_OPTIONS;
    }

    // 사운드 ID에 맞는 AI 컨텍스트를 찾아주는 메서드
    public String getAiContext(String soundId) {
        return SOUND_OPTIONS.stream()
                .filter(option -> option.getId().equalsIgnoreCase(soundId))
                .findFirst()
                .map(SoundOptionDto::getAiContext)
                .orElse(getAiContext(DEFAULT_SOUND_ID)); // 기본값 'none'의 컨텍스트 반환
    }

    // 사운드 ID에 맞는 배경 이미지 URL을 찾아주는 메서드
    public String getBackgroundImageUrl(String soundId) {
        return SOUND_IMAGE_MAP.getOrDefault(soundId.toLowerCase(), SOUND_IMAGE_MAP.get(DEFAULT_SOUND_ID));
    }
}