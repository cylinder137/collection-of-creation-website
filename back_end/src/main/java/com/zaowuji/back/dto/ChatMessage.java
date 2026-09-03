package com.zaowuji.back.dto;

/**
 * 对话消息（与 OpenAI / DeepSeek 官方接口兼容的 { role, content } 结构）
 */
public record ChatMessage(String role, String content) {
}
