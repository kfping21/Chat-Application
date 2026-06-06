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
    private const val API_KEY = "sk-ssjyovbqzrxwftpbmwcamovccvomwccbektsvbrxqhzomzml"

    // SiliconFlow API 地址
    private const val BASE_URL = "https://api.siliconflow.cn/v1"

    // DeepSeek V3 模型 (纯文本)
    private const val MODEL_TEXT = "deepseek-ai/DeepSeek-V3"
    
    // 视觉模型
    private const val MODEL_VISION = "Qwen/Qwen3-VL-32B-Instruct"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 调用 SiliconFlow API 生成推荐回复
     */
    fun generateReplySuggestions(
        chatHistory: List<Pair<String, String>>, // Pair<sender, message>
        latestMessage: String,
        soulMood: String,
        callback: (primary: String, secondary: String) -> Unit
    ) {
        if (API_KEY.isBlank() || API_KEY == "YOUR_SILICONFLOW_API_KEY") {
            callback("请先配置 API Key", "API Key 未配置")
            return
        }

        val systemPrompt = """你是一个真实的朋友在帮用户想回复。
根据对话情境，生成2条简短自然的回复（每条5-20字，像真人打字那样）。
要求：
1. 像朋友间打字聊天那样自然、随意、口语化
2. 可以是反问、调侃、共情、安慰、转移话题等任何真实聊天方式
3. 不要太正式、不要书面语、不要鸡汤
4. 可以用少量轻量级emoji但不要多用
5. 优先选择最符合此刻聊天氛围的回复风格"""

        val userContent = buildString {
            appendLine("最近聊天记录：")
            if (chatHistory.isEmpty()) {
                appendLine("（刚匹配，还没开始聊）")
            } else {
                chatHistory.takeLast(6).forEach { (sender, msg) ->
                    appendLine(if (sender == "me") "我：$msg" else "对方：$msg")
                }
            }
            appendLine()
            appendLine("请生成2条自然的回复：")
        }

        val jsonBody = JSONObject().apply {
            put("model", MODEL_TEXT)
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
                        val content = json.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("message")
                            ?.optString("content") ?: ""

                        val lines = content.trim().split("\n").filter { it.isNotBlank() }
                        val primary = lines.getOrElse(0) { "谢谢你的分享，我在认真听" }
                        val secondary = lines.getOrElse(1) { "你要不要再多说一点，我在陪伴你" }

                        callback(primary.trim(), secondary.trim())
                    } else {
                        callback("__ERROR__AI 服务暂时不可用", "__ERROR__HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                callback("__ERROR__AI 生成失败", "__ERROR__${e.message}")
            }
        }.start()
    }
    
    /**
     * 调用 SiliconFlow 视觉大模型生成发帖文案
     * 返回 Call 对象以便可以取消请求
     */
    fun generatePostInspiration(
        base64Image: String?,
        callback: (text: String, isError: Boolean) -> Unit
    ): Call? {
        if (API_KEY.isBlank() || API_KEY == "YOUR_SILICONFLOW_API_KEY") {
            callback("请先配置 API Key", true)
            return null
        }

        val jsonBody = JSONObject().apply {
            put("model", MODEL_VISION)
            put("temperature", 0.7)
            put("max_tokens", 200)
            
            val contentArray = JSONArray()
            contentArray.put(JSONObject().apply {
                put("type", "text")
                put("text", "这是一张用户准备发到匿名树洞里的图片，请你帮他写一段吸引人的发帖配图文案（带有情绪价值、稍微带点调皮或文艺，不超过50个字）。注意：直接输出文案内容，**绝对不要带有任何#话题标签**，不需要任何解释。")
            })
            
            if (base64Image != null) {
                contentArray.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
            }
            
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", if (base64Image != null) contentArray else contentArray.optJSONObject(0).optString("text"))
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("${BASE_URL}/chat/completions")
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
            
        val call = client.newCall(request)

        Thread {
            try {
                call.execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val json = JSONObject(responseBody ?: "{}")
                        val content = json.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("message")
                            ?.optString("content") ?: ""
                        // 额外做一个正则替换，防止AI没听话
                        val cleanContent = content.replace(Regex("#\\S+"), "").trim()
                        callback(cleanContent, false)
                    } else {
                        val errorBody = response.body?.string() ?: ""
                        callback("AI服务返回错误: ${response.code}\n$errorBody", true)
                    }
                }
            } catch (e: Exception) {
                if (call.isCanceled()) {
                    // Do nothing if cancelled
                } else {
                    callback("网络或解析错误: ${e.message}", true)
                }
            }
        }.start()
        
        return call
    }
}
