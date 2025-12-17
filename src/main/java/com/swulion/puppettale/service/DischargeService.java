package com.swulion.puppettale.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swulion.puppettale.dto.*;
import com.swulion.puppettale.entity.ChatMessage;
import com.swulion.puppettale.entity.FairyTale;
import com.swulion.puppettale.entity.Speaker;
import com.swulion.puppettale.repository.ChatMessageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
@Slf4j
public class DischargeService {

    private final ChatMessageRepository chatMessageRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FairyTaleService fairyTaleService;
    private final ImageService imageService;

    @Value("${api.key.gemini}")
    private String apiKey;
    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=";

    @Value("classpath:prompts/ai_systemPrompt_template.txt")
    private Resource aiSystemPromptResource;
    private String aiSystemPromptTemplate;

    @Value("classpath:prompts/story_creation_template.txt")
    private Resource storyCreationPromptResource;
    private String storyCreationPromptTemplate;

    @PostConstruct
    public void init() {
        try (Reader reader = new InputStreamReader(aiSystemPromptResource.getInputStream(), UTF_8)) {
            this.aiSystemPromptTemplate = FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            this.aiSystemPromptTemplate = "분석 프롬프트 로드 실패";
        }

        try (Reader reader = new InputStreamReader(storyCreationPromptResource.getInputStream(), UTF_8)) {
            this.storyCreationPromptTemplate = FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            this.storyCreationPromptTemplate = "동화 생성 프롬프트 로드 실패";
        }
    }

    // 최종 동화 생성 로직
    @Transactional
    public StoryCreationResponseDto createStory(StoryCreationRequestDto request) {

        Long childId = request.getChildId();
        String sessionId = request.getSessionId();
        String userName = Optional.ofNullable(request.getUserName()).orElse("아기사자");
        Integer userAge = request.getUserAge();
        String puppetName = Optional.ofNullable(request.getPuppetName()).orElse("토리");

        // 대화 분석
        GeminiChatJsonContentDto analysisResult = analyzeChatHistory(sessionId, userName, userAge, puppetName);

        if (analysisResult == null || analysisResult.getThoughtProcess() == null) {
            return StoryCreationResponseDto.builder()
                    .sessionId(sessionId)
                    .createdStory("분석 데이터가 부족하여 동화를 생성할 수 없습니다.")
                    .build();
        }

        // 안전 체크
        String safety = analysisResult.getThoughtProcess().getSafetyStatus();
        if ("RED_FLAG".equalsIgnoreCase(safety) || "MEDICAL".equalsIgnoreCase(safety)) {
            return StoryCreationResponseDto.builder()
                    .sessionId(sessionId)
                    .createdStory("안전 문제로 인해 동화를 생성할 수 없습니다.")
                    .detectedEmotion(analysisResult.getThoughtProcess().getDetectedEmotion())
                    .storyIngredients(analysisResult.getThoughtProcess().getStoryIngredients())
                    .build();
        }

        // 동화 텍스트 생성
        List<StoryIngredientsDto> ingredients = analysisResult.getThoughtProcess().getStoryIngredients();
        String detectedEmotion = analysisResult.getThoughtProcess().getDetectedEmotion();

        String ingredientsList = buildIngredientsList(ingredients);

        String finalStoryInstruction = storyCreationPromptTemplate
                .replace("{Puppet_Name}", puppetName)
                .replace("{User_Name}", userName)
                .replace("{User_Age}", userAge != null ? userAge.toString() : "7")
                .replace("{Story_Ingredients_List}", ingredientsList);

        String createdStory = callGeminiForStory(finalStoryInstruction);

        // 페이지 나누기
        List<FairyTalePageData> pages = splitStoryIntoPages(createdStory);

        // 동화 제목
        String newTitle = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        // 페이지별 이미지 생성 + S3 업로드
        for (FairyTalePageData page : pages) {
            String prompt = buildImagePromptForPage(page, userName, puppetName);
            String imageUrl = imageService.generateAndUploadImage(prompt);

            page.setImageUrl(imageUrl);
        }

        // 동화 저장
        FairyTale saved = fairyTaleService.saveFairyTale(childId, newTitle, pages);

        return StoryCreationResponseDto.builder()
                .sessionId(sessionId)
                .fairyTaleId(saved != null ? saved.getId() : null)
                .title(newTitle)
                .thumbnailUrl(pages.isEmpty() ? null : pages.get(0).getImageUrl())
                .pages(pages)
                .createdStory(createdStory)
                .detectedEmotion(detectedEmotion)
                .storyIngredients(ingredients)
                .build();
    }

    private String buildIngredientsList(List<StoryIngredientsDto> ingredients) {
        StringBuilder sb = new StringBuilder();
        ingredients.forEach(i -> sb.append("- Type: ")
                .append(i.getType())
                .append(", Element: ")
                .append(i.getFantasyElement())
                .append(", Memory: ")
                .append(i.getRealMemory())
                .append("\n"));
        return sb.toString();
    }

    private List<FairyTalePageData> splitStoryIntoPages(String story) {
        List<FairyTalePageData> pages = new ArrayList<>();
        if (story == null || story.isBlank()) return pages;

        // \n\n을 기준으로 분할하고 텍스트 정리
        String[] paragraphs = story.split("\n\n");

        int pageNo = 1;

        for (String rawParagraph : paragraphs) {
            String pageText = rawParagraph.trim();

            // 빈 문자열이거나 공백만 있는 경우 무시
            if (pageText.isEmpty()) continue;
            // 최대 4 페이지까지만 처리 (동화책 분량을 위해 제한)
            if (pageNo > 4) break;
            String cleanText = pageText
                    .replaceAll("[\r\n]+", " ")
                    .replace("\\\"", "\"")
                    .replace("\\", "");
            pages.add(
                    FairyTalePageData.builder()
                            .pageNumber(pageNo++)
                            .text(cleanText)
                            .build()
            );
        }
        // 최소 3 페이지 보장
        while (pages.size() < 3) {
            pages.add(
                    FairyTalePageData.builder()
                            .pageNumber(pages.size() + 1)
                            .text("더 많은 이야기를 들려줘서 고마워!")
                            .imageUrl("https://cdn.example.com/placeholder.png")
                            .build()
            );
        }
        return pages;
    }

    private String buildImagePromptForPage(FairyTalePageData page, String userName, String puppetName) {
        return "동화 삽화 생성: " + page.getText() +
                " 스타일: 아동용, 단순, 밝고 따뜻한 색감, 귀엽고 위로가 되는 분위기.";
    }

    // 이미지 생성 스텁
    private String callImageGeneration(String prompt) {
        // TODO: 제미나이 이미지 API 연동. 현재는 placeholder 반환
        return "https://cdn.example.com/placeholder-image.png";
    }

    // 특정 세션의 모든 대화 기록을 조회하여 동화책 생성 준비
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversationHistory(String sessionId) {
        // 모든 대화 기록을 시간 순으로 조회
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        if (history.isEmpty()) {
            throw new IllegalArgumentException("해당 세션 ID(" + sessionId + ")에 대한 대화 기록이 존재하지 않습니다.");
        }
        return history;
    }

    // 대화 로그를 기반으로 Gemini에 분석을 요청하고, ThoughtProcess DTO를 반환
    private GeminiChatJsonContentDto analyzeChatHistory(String sessionId, String userName, Integer userAge, String puppetName) {
        String systemInstruction = this.aiSystemPromptTemplate
                .replace("{Puppet_Name}", puppetName)
                .replace("{User_Name}", userName)
                .replace("{User_Age}", userAge != null ? userAge.toString() : "7")
                .replace("{User_Constraint}", "없음");

        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        if (history.isEmpty()) {
            log.warn("세션 {}에 대화 기록이 없어 분석을 시작할 수 없습니다.", sessionId);
            return null;
        }

        List<GeminiChatRequestDto.Content> contents = new ArrayList<>();
        contents.add(
                GeminiChatRequestDto.Content.builder().role("user").parts(List.of(new GeminiChatRequestDto.Part(systemInstruction))).build()
        );

        // 대화 기록 추가
        for (ChatMessage log : history) {
            String role = log.getSpeaker() == Speaker.USER ? "user" : "model";
            contents.add(
                    GeminiChatRequestDto.Content.builder().role(role).parts(List.of(new GeminiChatRequestDto.Part(log.getMessage()))).build()
            );
        }

        // 분석 요청 메시지 추가
        contents.add(
                GeminiChatRequestDto.Content.builder().role("user").parts(List.of(new GeminiChatRequestDto.Part("현재까지의 대화 내용을 바탕으로 분석 JSON을 출력해 주세요."))).build()
        );
        GeminiChatRequestDto requestBody = new GeminiChatRequestDto(contents);
        String apiUrlWithKey = GEMINI_API_URL + apiKey;

        try {
            ResponseEntity<String> response = performApiCall(apiUrlWithKey, requestBody);

            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseBody = response.getBody();
                GeminiChatResponseDto geminiResponse = objectMapper.readValue(responseBody, GeminiChatResponseDto.class);
                String jsonPayload = geminiResponse.getFirstText();

                if (jsonPayload != null) {
                    // Markdown 코드 블록 구문 제거
                    String cleanJson = jsonPayload
                            .trim() // 앞뒤 공백 제거
                            .replaceAll("```json", "") // 시작 코드 블록 구문 제거
                            .replaceAll("```", "") // 종료 코드 블록 구문 제거
                            .trim(); // 제거 후 다시 앞뒤 공백 제거

                    return objectMapper.readValue(cleanJson, GeminiChatJsonContentDto.class);
                }
            }
        } catch (Exception e) {
            log.error("분석 API 호출 중 파싱 실패:", e);
        }
        return null;
    }

    // 동화 생성을 위한 API 호출 (일반 텍스트 요청)
    private String callGeminiForStory(String storyInstruction) {
        List<GeminiChatRequestDto.Content> contents = List.of(
                GeminiChatRequestDto.Content.builder()
                        .role("user")
                        .parts(List.of(new GeminiChatRequestDto.Part(storyInstruction)))
                        .build()
        );
        GeminiChatRequestDto requestBody = new GeminiChatRequestDto(contents);
        String apiUrlWithKey = GEMINI_API_URL + apiKey;

        try {
            ResponseEntity<String> response = performApiCall(apiUrlWithKey, requestBody);
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                String responseBody = response.getBody();
                GeminiChatResponseDto geminiResponse = objectMapper.readValue(responseBody, GeminiChatResponseDto.class);

                String storyText = geminiResponse.getFirstText();
                if (storyText != null) {
                    return storyText.trim();
                }
            }
        } catch (Exception e) {
            log.error("동화 생성 API 호출 중 오류 및 파싱 실패:", e);
        }
        return null;
    }
    
    // API 호출 및 재시도 공통
    private ResponseEntity<String> performApiCall(String apiUrlWithKey, GeminiChatRequestDto requestBody) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeminiChatRequestDto> entity = new HttpEntity<>(requestBody, headers);

        final int MAX_RETRIES = 3;
        final long WAIT_TIME_MS = 1000;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return restTemplate.postForEntity(apiUrlWithKey, entity, String.class);
            } catch (Exception e) {
                log.warn("API 호출 실패 (시도: {})", attempt);
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(WAIT_TIME_MS);
                } else {
                    log.error("API 호출 최종 실패");
                    break;
                }
            }
        }
        return null;
    }
}