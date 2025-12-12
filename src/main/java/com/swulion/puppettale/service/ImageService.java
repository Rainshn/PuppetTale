package com.swulion.puppettale.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3StorageService s3StorageService;
    private final RestTemplate restTemplate;

    @Value("${api.key.gemini}")
    private String geminiApiKey;

//    public String generateAndUploadImage(String prompt) {
//        // Gemini 이미지 생성 API URL
//        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
//                + "gemini-2.5-flash-image:generateContent?key=" + geminiApiKey;
//
//        // 요청 Body 구성
//        Map<String, Object> body = Map.of(
//                "contents", List.of(
//                        Map.of(
//                                "role", "user",
//                                "parts", List.of(Map.of("text", prompt))
//                        )
//                )
//        );
//        ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
//
//        Map candidate = (Map)((List)response.getBody().get("candidates")).get(0);
//        Map content = (Map)((List)candidate.get("content")).get(0);
//        Map part = (Map)((List)content.get("parts")).get(0);
//
//        Map inline = (Map) part.get("inlineData");
//        String base64 = (String) inline.get("data");
//
//        byte[] imageBytes = Base64.getDecoder().decode(base64);
//        String fileName = UUID.randomUUID() + ".png";
//
//        return s3StorageService.uploadImage(fileName, imageBytes);
//    }

    // 테스트용 더미
    public String generateAndUploadImage(String prompt) {
        return "https://cdn.example.com/placeholder-image.png";
    }
}