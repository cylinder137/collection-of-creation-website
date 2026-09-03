import { request } from './http'

/** 对话消息（与 DeepSeek / OpenAI 官方接口兼容的 { role, content } 结构） */
export interface AiChatMsg {
  role: 'user' | 'assistant'
  content: string
}

/**
 * 发送一轮 AI 客服对话
 *
 * 链路：前端 POST /api/ai/chat（消息数组）→ 后端拼接客服系统提示词后，
 * 按 DeepSeek 官方接口格式（POST https://api.deepseek.com/chat/completions）转发。
 * API Key 只存在于后端环境变量 DEEPSEEK_API_KEY，前端不接触密钥。
 */
export async function sendAiChat(messages: AiChatMsg[]): Promise<string> {
  const data = await request<{ reply: string }>({
    url: '/ai/chat',
    method: 'POST',
    data: { messages },
  })
  return data.reply
}
