package com.health.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.management.config.FoodVisionProperties;
import com.health.management.dto.HealthDtos.FoodRecognitionSuggestion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用云侧 OpenAI 兼容 chat/completions 多模态接口（支持豆包方舟、OpenAI 等）从餐食图估算食物与总热量（仅供参考）。
 */
@Service
public class FoodVisionCloudService {

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private static final String PROMPT = """
            你是营养助手的图像分析模块。用户上传一张餐食照片，请估计画面中主要食物及这一餐整体可参考的总热量（单位 kcal）。
            重要：只输出一个 JSON 数组，不要 Markdown、不要代码块、不要其它说明文字。
            数组中每一项必须是对象，字段为：
            - name：字符串，简要描述该条（可为整餐综合描述或分项）
            - estimatedTotalKcal：数字，该条对应的参考热量；若描述整餐则各分项可略有重叠时以整餐一条为准
            - basis：字符串，热量估算依据（如「按常见盒饭份量」）
            - note：字符串，不确定性说明，可做空字符串
            若完全无法辨认，只输出：[{"name":"无法识别","estimatedTotalKcal":0,"basis":"","note":"请手动输入或换更清晰的照片"}]
            """;

    private final FoodVisionProperties properties;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public FoodVisionCloudService(
            FoodVisionProperties properties,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    public List<FoodRecognitionSuggestion> recognize(String imagePublicUrl) throws IOException {
        if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "未启用食物图像识别：请在【不被提交的】application-local.properties 中设置 app.food-vision.enabled=true 与 app.food-vision.api-key，并确认 application.properties 顶行已包含 spring.config.import=optional:classpath:application-local.properties，然后重启后端");
        }
        byte[] bytes = fileStorageService.readPublicImageBytes(imagePublicUrl);
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("图片过大（超过 5MB），请先压缩后再试");
        }
        String mime = fileStorageService.guessMimeTypeForPublicUrl(imagePublicUrl);
        String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);

        String base = properties.getBaseUrl().trim().replaceAll("/+$", "");
        HttpClient jdkHttp = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkHttp);
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        RestClient client = RestClient.builder()
                .baseUrl(base)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", PROMPT));
        content.add(Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)));
        userMsg.put("content", content);
        messages.add(userMsg);
        body.put("messages", messages);
        body.put("max_tokens", 800);

        String raw;
        try {
            raw = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            String b = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            if (b != null && b.contains("batch inference")) {
                throw new IllegalStateException(
                        "当前 app.food-vision.model 对应的是方舟「批量推理」接入点，只支持 batch/chat/completions，不能用于本功能的实时 chat/completions。"
                                + "请到火山方舟控制台新建「在线推理」接入点（选支持图片的多模态模型），将 model 换成新的 ep- 接入点 ID 后重启后端。");
            }
            throw new IllegalStateException(
                    "云 API 错误：" + e.getStatusCode().value() + " "
                            + (b != null && !b.isBlank() ? b : e.getMessage()));
        }

        JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
        if (root.hasNonNull("error")) {
            String msg = root.path("error").path("message").asText(raw);
            throw new IllegalStateException("云 API 错误：" + msg);
        }
        String text = root.path("choices").path(0).path("message").path("content").asText("");
        return parseSuggestions(extractJsonArray(text));
    }

    private static String extractJsonArray(String text) {
        if (text == null || text.isBlank()) {
            return "[]";
        }
        String t = text.trim();
        int i = t.indexOf('[');
        int j = t.lastIndexOf(']');
        if (i >= 0 && j > i) {
            return t.substring(i, j + 1);
        }
        return "[]";
    }

    private List<FoodRecognitionSuggestion> parseSuggestions(String json) throws JsonProcessingException {
        JsonNode arr = objectMapper.readTree(json);
        if (!arr.isArray()) {
            return List.of();
        }
        List<FoodRecognitionSuggestion> out = new ArrayList<>();
        for (JsonNode n : arr) {
            FoodRecognitionSuggestion s = new FoodRecognitionSuggestion();
            s.setName(n.path("name").asText("未知"));
            JsonNode k = n.get("estimatedTotalKcal");
            if (k != null && k.isNumber()) {
                s.setEstimatedTotalKcal(BigDecimal.valueOf(k.asDouble()));
            } else if (k != null && k.isTextual()) {
                try {
                    s.setEstimatedTotalKcal(new BigDecimal(k.asText()));
                } catch (NumberFormatException e) {
                    s.setEstimatedTotalKcal(BigDecimal.ZERO);
                }
            } else {
                s.setEstimatedTotalKcal(BigDecimal.ZERO);
            }
            s.setBasis(n.path("basis").asText(""));
            s.setNote(n.path("note").asText(""));
            out.add(s);
        }
        return out;
    }
}
