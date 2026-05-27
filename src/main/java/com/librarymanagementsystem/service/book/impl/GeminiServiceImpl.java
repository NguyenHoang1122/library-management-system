package com.librarymanagementsystem.service.book.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanagementsystem.service.book.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ModerationResult moderateText(String text, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key is empty. Skipping AI moderation.");
            return new ModerationResult(false, new ArrayList<>(), text, "API key không hợp lệ");
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            String systemPrompt = "Bạn là trợ lý AI kiểm duyệt bình luận cho ứng dụng Truyện 360.\n" +
                    "Nhiệm vụ của bạn là phân tích văn bản bình luận sau và xác định xem nó có chứa từ ngữ thô tục, chửi thề, nhạy cảm, xúc phạm, khiêu dâm, vi phạm pháp luật hoặc không phù hợp với môi trường thư viện thân thiện hay không.\n" +
                    "Hãy phản hồi CHỈ bằng một chuỗi JSON hợp lệ với định dạng chính xác sau (không kèm ký tự markdown như ```json, không giải thích gì thêm):\n" +
                    "{\n" +
                    "  \"sensitive\": true/false,\n" +
                    "  \"sensitiveWordsFound\": [\"từ_nhạy_cảm_1\", \"từ_nhạy_cảm_2\"],\n" +
                    "  \"censoredText\": \"bản_censor_thay_từ_nhạy_cảm_bằng_****\",\n" +
                    "  \"reason\": \"lý_do_ngắn_gọn_bằng_tiếng_Việt\"\n" +
                    "}";

            // Build request body
            Map<String, Object> payload = Map.of(
                "contents", List.of(
                    Map.of(
                        "parts", List.of(
                            Map.of("text", systemPrompt + "\n\nVăn bản cần kiểm tra:\n\"" + text + "\"")
                        )
                    )
                ),
                "generationConfig", Map.of(
                    "responseMimeType", "application/json"
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            String responseStr = restTemplate.postForObject(url, request, String.class);

            JsonNode rootNode = objectMapper.readTree(responseStr);
            String rawJsonResult = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
            // Clean up LLM response
            String cleanJson = cleanJsonText(rawJsonResult);
            JsonNode resultNode = objectMapper.readTree(cleanJson);

            boolean sensitive = resultNode.path("sensitive").asBoolean(false);
            List<String> sensitiveWords = new ArrayList<>();
            if (resultNode.has("sensitiveWordsFound") && resultNode.get("sensitiveWordsFound").isArray()) {
                for (JsonNode wordNode : resultNode.get("sensitiveWordsFound")) {
                    sensitiveWords.add(wordNode.asText().toLowerCase().trim());
                }
            }
            String censoredText = resultNode.path("censoredText").asText(text);
            String reason = resultNode.path("reason").asText("");

            return new ModerationResult(sensitive, sensitiveWords, censoredText, reason);

        } catch (Exception e) {
            log.error("Error during Gemini AI moderation: ", e);
            throw new RuntimeException("Lỗi kết nối AI: " + e.getMessage(), e);
        }
    }

    private String cleanJsonText(String text) {
        if (text == null) return "{}";
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}
