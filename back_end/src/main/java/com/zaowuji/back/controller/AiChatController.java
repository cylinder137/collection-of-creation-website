package com.zaowuji.back.controller;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.dto.ChatRequest;
import com.zaowuji.back.service.AiChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 客服接口（对外公开）
 *
 * 调用链路：官网前端悬浮客服 → POST /api/ai/chat → 后端拼接客服提示词后，
 * 按 DeepSeek 官方接口格式（OpenAI 兼容 /chat/completions）转发 → 返回 { reply }
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /** 发送一轮对话；messages 为 { role, content } 数组（仅 user/assistant，system 由服务端拼接） */
    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(@RequestBody ChatRequest request) {
        String reply = aiChatService.chat(request.messages());
        return ApiResponse.ok(Map.of("reply", reply));
    }
}
