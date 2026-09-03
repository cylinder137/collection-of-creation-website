package com.zaowuji.back.dto;

import java.util.List;

/**
 * AI 客服对话请求：前端携带多轮消息数组（不含 system，系统提示词由服务端统一拼接）
 */
public record ChatRequest(List<ChatMessage> messages) {
}
