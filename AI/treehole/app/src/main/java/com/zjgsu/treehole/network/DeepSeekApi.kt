package com.zjgsu.treehole.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * SiliconFlow API 服务（调用 DeepSeek 模型）
 *
 * 使用方法：
 * 1. 访问 https://siliconflow.cn/ 注册账号（支持微信充值）
 * 2. 获取 API Key
 * 3. 将下面的 API_KEY 替换为你的实际 API Key
 */
object DeepSeekApi {

    // ==========================================
    // ⚠️ 在这里填入你的 SiliconFlow API Key ⚠️
    // ==========================================
    private const val API_KEY = "sk-xeubtyphhsggtsmnktsgutbbimmcbiajvzsxnvaabiogohmb"

    // SiliconFlow API 地址
    private const val BASE_URL = "https://api.siliconflow.cn/v1"

    // DeepSeek V2.5 模型，性价比最高
    private const val MODEL = "deepseek-ai/DeepSeek-V2.5"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 调用 SiliconFlow API 生成推荐回复
     *
     * @param chatHistory 最近的多轮对话历史，用于生成更精准的推荐回复
     * @param latestMessage 最新收到的对方消息
     * @param soulMood 灵魂伴侣的心情/背景描述，用于生成更个性化的回复
     * @param callback 回调函数，返回两条推荐回复
     */
    fun generateReplySuggestions(
        chatHistory: List<Pair<String, String>>, // Pair<sender, message>
        latestMessage: String,
        soulMood: String,
        callback: (primary: String, secondary: String) -> Unit
    ) {
        if (API_KEY == "YOUR_SILICONFLOW_API_KEY" || API_KEY.isBlank()) {
            callback(
                "请先配置 SiliconFlow API Key 才能使用 AI 生成功能",
                "AI 推荐功能需要有效的 API Key"
            )
            return
        }

        // 构建灵魂伴侣的背景描述
        val soulContext = if (soulMood.isNotBlank()) {
            "\n\n【灵魂伴侣的背景心情】$soulMood"
        } else {
            ""
        }

        val systemPrompt = """你是一个贴心的朋友，正在帮用户想一句温暖的话来回复心情不好的朋友。
请根据对方的处境和心情，生成2条简短自然的推荐回复（每条15-25字，像真人说话）。
要求：
1. 像朋友间聊天那样自然、接地气，不要书面语
2. 一条偏倾听陪伴（共情），一条偏温暖鼓励（引导）
3. 不要用表情符号、不要用"你/我"之外的称呼
4. 直接输出两条回复，每条一行，不要其他说明"""

        val userContent = buildString {
            appendLine("对方的心情背景：")
            if (soulMood.isNotBlank()) {
                appendLine(soulMood)
            } else {
                appendLine("暂无背景信息")
            }
            appendLine()
            appendLine("最近聊天：")
            if (chatHistory.isEmpty()) {
                appendLine("（刚匹配上，还没有聊天）")
            } else {
                chatHistory.takeLast(4).forEach { (sender, msg) ->
                    appendLine(if (sender == "me") "我：$msg" else "对方：$msg")
                }
            }
            appendLine()
            appendLine("请生成2条自然的回复：")
        }

        val jsonBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 200)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("${BASE_URL}/chat/completions")
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val json = JSONObject(responseBody ?: "{}")
                        val choices = json.optJSONArray("choices")
                        val content = choices?.optJSONObject(0)
                            ?.optJSONObject("message")
                            ?.optString("content")
                            ?: ""

                        val lines = content.trim().split("\n").filter { it.isNotBlank() }
                        val primary = lines.getOrElse(0) { "谢谢你的分享，我在认真听" }
                        val secondary = lines.getOrElse(1) { "你要不要再多说一点，我在陪伴你" }

                        callback(primary.trim(), secondary.trim())
                    } else {
                        callback(
                            "__ERROR__AI 服务暂时不可用，请稍后重试",
                            "__ERROR__网络请求失败: ${response.code}"
                        )
                    }
                }
            } catch (e: Exception) {
                callback(
                    "__ERROR__AI 生成失败",
                    "__ERROR__${e.message}"
                )
            }
        }.start()
    }
}
