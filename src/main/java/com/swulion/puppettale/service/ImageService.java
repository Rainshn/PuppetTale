package com.swulion.puppettale.service;

import com.swulion.puppettale.dto.StoryIngredientsDto;
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
    public String buildFinalPrompt(String pageText,
                                   String userName,
                                   String puppetName,
                                   List<StoryIngredientsDto> storyIngredients) {
        StringBuilder ingredientsDescription = new StringBuilder();
        for (StoryIngredientsDto ingredient : storyIngredients) {
            if (pageText.contains(ingredient.getFantasyElement())) {
                ingredientsDescription.append(ingredient.getFantasyElement()).append(", ");
            }
        }

        return "GENERATE_IMAGE_ONLY. NO TEXT. " +
                "Professional 3D Pixar claymation style illustration. " +
                "Style: Soft pastel colors, cute and rounded shapes, glowing magical lighting, cozy atmosphere. " +
                "Main Character: A cute 7-year-old child named " + userName + ". " +
                (pageText.contains(puppetName) ? "Include a friendly sea lion named " + puppetName + ". " : "") +
                "Visual Scene to paint: " + pageText.trim() + ". " +
                (ingredientsDescription.length() > 0 ? "Additional elements: " + ingredientsDescription.toString() : "") +
                "CRITICAL RULES: Strictly NO text, NO letters, NO words, NO signage, NO subtitles, NO speech bubbles. " +
                "Focus only on the visual storytelling. High-quality CGI render look.";
    }

    public String generateAndUploadImage(String fullPrompt) {
        String refinedPrompt = fullPrompt;

        // Gemini 이미지 생성 API URL
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + "gemini-2.5-flash-image-preview:generateContent?key=" + geminiApiKey;

        // 요청 Body 구성
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", refinedPrompt))
                        )
                )
        );
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
            Map<String, Object> responseBody = response.getBody();
            log.info("[Gemini API Response] : {}", response.getBody());

            if (responseBody == null || !responseBody.containsKey("candidates")) {
                throw new RuntimeException("Gemini API 응답에 candidates가 없습니다.");
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            String base64Data = null;
            for (Map<String, Object> part : parts) {
                if (part.containsKey("inlineData")) {
                    Map<String, Object> inlineData = (Map<String, Object>) part.get("inlineData");
                    base64Data = (String) inlineData.get("data");
                    break; // 이미지를 찾았으면 반복 중단
                }
            }

            // 이미지가 왔는지 확인
            if (base64Data != null) {
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                String fileName = "stories/" + UUID.randomUUID() + ".png";
                return s3StorageService.uploadImage(fileName, imageBytes);
            } else {
                // 이미지가 정말 없을 때만 텍스트 로그
                String textResponse = (parts.get(0).containsKey("text")) ? (String) parts.get(0).get("text") : "No Content";
                log.error("Gemini 응답에 이미지 데이터가 없음. 텍스트 내용: {}", textResponse);
                return "https://puppettale-images.s3.ap-northeast-2.amazonaws.com/default-placeholder.png";
            }

        } catch (Exception e) {
            log.error("이미지 생성 중 에러 발생: ", e);
            return "https://puppettale-images.s3.ap-northeast-2.amazonaws.com/default-placeholder.png";
        }
    }
}

//    // 테스트용 더미
//    public String generateAndUploadImage(String prompt) {
//        return "https://cdn.example.com/placeholder-image.png";
//    }
//}
