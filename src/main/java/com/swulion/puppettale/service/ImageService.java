package com.swulion.puppettale.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ImageService {

    private final S3StorageService s3StorageService;
    private final RestTemplate restTemplate;

    @Value("${api.key.gemini}")
    private String geminiApiKey;

    // 이미지 생성 프롬프트
    private String buildFinalPrompt(String storyText) {
        return "A high-quality, professional 2D digital storybook illustration for children. " +
                "Style: Cute and rounded character designs, grainy chalk-like texture, soft pastel color palette. " +
                "Details: Gentle gradients, glowing warm lighting, dreamy and cozy atmosphere. " +
                "Subject: " + storyText + ". " +
                "CRITICAL RULES: DO NOT include any text, letters, or words. STRICTLY NO TYPOGRAPHY. " +
                "Focus entirely on depicting the scene described in the 'Subject' naturally and artistically." +
                "No 3D render, no realism. Maintain a flat but textured 2D look.";
    }

//    public String generateAndUploadImage(String prompt) {
//        String refinedPrompt = buildFinalPrompt(prompt);
//
//        // Gemini 이미지 생성 API URL
//        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
//                + "gemini-2.5-flash-image-preview:generateContent?key=" + geminiApiKey;
//
//        // 요청 Body 구성
//        Map<String, Object> body = Map.of(
//                "contents", List.of(
//                        Map.of(
//                                "role", "user",
//                                "parts", List.of(Map.of("text", refinedPrompt))
//                        )
//                )
//        );
//        try {
//            ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
//            Map<String, Object> responseBody = response.getBody();
//            log.info("[Gemini API Response] : {}", response.getBody());
//
//            if (responseBody == null || !responseBody.containsKey("candidates")) {
//                throw new RuntimeException("Gemini API 응답에 candidates가 없습니다.");
//            }
//
//            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
//            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
//            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
//            Map<String, Object> firstPart = parts.get(0);
//
//            // 이미지가 왔는지 확인
//            if (firstPart.containsKey("inlineData")) {
//                Map<String, Object> inlineData = (Map<String, Object>) firstPart.get("inlineData");
//                String base64 = (String) inlineData.get("data");
//
//                byte[] imageBytes = Base64.getDecoder().decode(base64);
//                String fileName = "stories/" + UUID.randomUUID() + ".png";
//
//                return s3StorageService.uploadImage(fileName, imageBytes);
//            } else {
//                // 이미지가 아니라 텍스트가 온 경우
//                log.error("Gemini가 이미지 대신 텍스트를 반환함: {}", firstPart.get("text"));
//                return "https://puppettale-images.s3.ap-northeast-2.amazonaws.com/default-placeholder.png"; // 준비해둔 기본 이미지 URL
//            }
//
//        } catch (Exception e) {
//            log.error("이미지 생성 중 에러 발생: ", e);
//            return "https://puppettale-images.s3.ap-northeast-2.amazonaws.com/default-placeholder.png";
//        }
//    }
//}

    // 테스트용 더미
    public String generateAndUploadImage(String prompt) {
        return "https://cdn.example.com/placeholder-image.png";
    }
}
