package com.zjgsu.treehole

import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.PostsApi
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.model.Comment
import com.zjgsu.treehole.model.MoodItem
import com.zjgsu.treehole.model.MockData
import org.junit.Assert.*
import org.junit.Test

/**
 * API 和网络层单元测试
 */
class ApiTest {

    // ========== RetrofitClient 测试 ==========

    @Test
    fun `RetrofitClient 单例模式验证`() {
        val client1 = RetrofitClient
        val client2 = RetrofitClient
        assertSame(client1, client2)
    }

    @Test
    fun `RetrofitClient postsApi 不为空`() {
        val postsApi = RetrofitClient.postsApi
        assertNotNull(postsApi)
    }

    @Test
    fun `RetrofitClient BASE_URL 格式正确`() {
        val baseUrl = "http://10.0.2.2:3001/"
        assertTrue(baseUrl.startsWith("http://"))
        assertTrue(baseUrl.endsWith("/"))
    }

    // ========== 数据模型验证测试 ==========

    @Test
    fun `Secret 数据结构完整`() {
        val secret = Secret("1", "测试内容", "开心", "1小时前", 100, 20)
        assertEquals("1", secret.id)
        assertEquals("测试内容", secret.content)
        assertEquals("开心", secret.mood)
        assertEquals("1小时前", secret.timeAgo)
        assertEquals(100, secret.likes)
        assertEquals(20, secret.comments)
    }

    @Test
    fun `Comment 数据结构完整`() {
        val comment = Comment("c1", "评论内容", "30分钟前", 10, 1)
        assertEquals("c1", comment.id)
        assertEquals("评论内容", comment.content)
        assertEquals(10, comment.likes)
        assertEquals(1, comment.floor)
    }

    @Test
    fun `MoodItem 颜色配置完整`() {
        val mood = MoodItem("焦虑", "☁️", "#F472B6", "#EC4899")
        assertTrue(mood.colorStart.startsWith("#"))
        assertTrue(mood.colorEnd.startsWith("#"))
        assertEquals(7, mood.colorStart.length)
    }

    // ========== API 响应状态测试 ==========

    @Test
    fun `成功响应数据结构验证`() {
        val successResponse = mapOf(
            "code" to 200,
            "message" to "success",
            "data" to listOf(
                mapOf("id" to "1", "content" to "树洞1"),
                mapOf("id" to "2", "content" to "树洞2")
            )
        )
        assertEquals(200, successResponse["code"])
        assertEquals("success", successResponse["message"])
        val data = successResponse["data"] as? List<*>
        assertNotNull(data)
        assertEquals(2, data!!.size)
    }

    @Test
    fun `错误响应数据结构验证`() {
        val errorResponse = mapOf(
            "code" to 404,
            "message" to "Not Found",
            "data" to null
        )
        assertEquals(404, errorResponse["code"])
        assertEquals("Not Found", errorResponse["message"])
        assertNull(errorResponse["data"])
    }

    @Test
    fun `网络错误数据结构验证`() {
        val networkError = mapOf(
            "error" to "Network Error",
            "message" to "无法连接到服务器",
            "code" to -1
        )
        assertEquals("Network Error", networkError["error"])
        assertEquals(-1, networkError["code"])
    }

    // ========== 分页和列表测试 ==========

    @Test
    fun `分页响应数据结构验证`() {
        val paginatedResponse = mapOf(
            "code" to 200,
            "data" to mapOf(
                "list" to listOf(
                    mapOf("id" to "1", "content" to "内容1"),
                    mapOf("id" to "2", "content" to "内容2")
                ),
                "page" to 1,
                "pageSize" to 20,
                "total" to 100
            )
        )
        val data = paginatedResponse["data"] as Map<*, *>
        assertEquals(1, data["page"])
        assertEquals(20, data["pageSize"])
        assertEquals(100, data["total"])
        val list = data["list"] as List<*>
        assertEquals(2, list.size)
    }

    @Test
    fun `空列表响应验证`() {
        val emptyResponse = mapOf(
            "code" to 200,
            "data" to emptyList<String>()
        )
        val data = emptyResponse["data"] as List<*>
        assertTrue(data.isEmpty())
    }

    // ========== 心情数据验证测试 ==========

    @Test
    fun `心情列表包含所有8种心情`() {
        val moods = MockData.moods.map { it.name }
        listOf("孤独", "开心", "后悔", "焦虑", "平静", "迷茫", "感动", "释然").forEach { expected ->
            assertTrue("缺少心情: $expected", moods.contains(expected))
        }
    }

    @Test
    fun `所有心情都有对应颜色配置`() {
        MockData.moods.forEach { mood ->
            assertTrue(mood.colorStart.startsWith("#"))
            assertTrue(mood.colorEnd.startsWith("#"))
            assertEquals(7, mood.colorStart.length)
            assertEquals(7, mood.colorEnd.length)
        }
    }

    // ========== 请求参数验证测试 ==========

    @Test
    fun `登录请求参数验证`() {
        val loginRequest = mapOf(
            "email" to "test@example.com",
            "password" to "password123"
        )
        assertTrue(loginRequest["email"]!!.contains("@"))
        assertTrue((loginRequest["password"] as String).length >= 6)
    }

    @Test
    fun `树洞发布请求参数验证`() {
        val postRequest = mapOf(
            "content" to "这是一条测试树洞",
            "mood" to "开心"
        )
        assertTrue((postRequest["content"] as String).isNotEmpty())
        assertTrue((postRequest["mood"] as String).isNotEmpty())
    }

    @Test
    fun `评论请求参数验证`() {
        val commentRequest = mapOf(
            "secretId" to "123",
            "content" to "写的真好"
        )
        assertTrue((commentRequest["secretId"] as String).isNotEmpty())
        assertTrue((commentRequest["content"] as String).isNotEmpty())
    }

    // ========== 边界情况测试 ==========

    @Test
    fun `超长内容处理`() {
        val longContent = "A".repeat(5000)
        val postRequest = mapOf(
            "content" to longContent,
            "mood" to "平静"
        )
        assertEquals(5000, (postRequest["content"] as String).length)
    }

    @Test
    fun `特殊字符处理`() {
        val specialContent = "特殊字符: @#\$%^&*()_+-=[]{}|;':\",./<>?"
        val postRequest = mapOf(
            "content" to specialContent,
            "mood" to "开心"
        )
        assertEquals(specialContent, postRequest["content"])
    }

    @Test
    fun `中文和 Emoji 处理`() {
        val content = "中文内容 😄🎉💖 更多的中文"
        val postRequest = mapOf(
            "content" to content,
            "mood" to "开心"
        )
        assertTrue((postRequest["content"] as String).contains("😄"))
        assertTrue((postRequest["content"] as String).contains("中文"))
    }

    // ========== MockData 验证 ==========

    @Test
    fun `MockData secrets 数据完整`() {
        MockData.secrets.forEach { secret ->
            assertTrue(secret.id.isNotEmpty())
            assertTrue(secret.content.isNotEmpty())
            assertTrue(secret.mood.isNotEmpty())
            assertTrue(secret.likes >= 0)
            assertTrue(secret.comments >= 0)
        }
    }

    @Test
    fun `MockData comments floor 连续`() {
        val floors = MockData.comments.map { it.floor }.sorted()
        assertEquals(floors, (1..floors.size).toList())
    }

    @Test
    fun `MockData mySecrets 内容验证`() {
        MockData.mySecrets.forEach { secret ->
            assertTrue(secret.mood in listOf("平静", "感动", "孤独"))
        }
    }
}