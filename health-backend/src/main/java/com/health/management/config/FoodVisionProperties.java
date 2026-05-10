package com.health.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 云侧「图像理解 + 热量估算」，请求体为 OpenAI 兼容的 chat/completions（多模态：text + image_url）。
 * <p><b>豆包（火山方舟）</b>：base-url 设为 {@code https://ark.cn-beijing.volces.com/api/v3}（以控制台为准），
 * api-key 为方舟 API Key，model 为<b>推理接入点 ID</b>（如 {@code ep-xxxx}），需选用支持视觉理解的接入点。
 * <p>其他厂商若提供同类兼容网关，可只改 base-url、model 与 key。
 */
@Data
@ConfigurationProperties(prefix = "app.food-vision")
public class FoodVisionProperties {

    /** 为 false 或未配 api-key 时，拒绝调用远端，避免误扣费 */
    private boolean enabled = false;

    /** 方舟：控制台「API Key 管理」中创建的密钥 */
    private String apiKey = "";

    /**
     * 不含末尾斜杠。豆包方舟一般为 {@code https://ark.cn-beijing.volces.com/api/v3}；
     * OpenAI 官方为 {@code https://api.openai.com/v1}。
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 豆包须填<b>推理接入点 ID</b>（{@code ep-...}）；OpenAI 可填模型名如 gpt-4o-mini。
     */
    private String model = "gpt-4o-mini";
}
