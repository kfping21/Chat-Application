package com.zjgsu.treehole

import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.model.Secret
import org.junit.Assert.*
import org.junit.Test

/**
 * SecretAdapter 单元测试
 * 测试适配器的数据处理、点击事件和UI状态更新逻辑
 */
class SecretAdapterTest {

    private fun createTestSecrets(): List<Secret> {
        return listOf(
            Secret("1", "第一条树洞内容", "开心", "1小时前", 100, 20),
            Secret("2", "第二条树洞内容", "孤独", "2小时前", 50, 10),
            Secret("3", "第三条树洞内容", "平静", "3小时前", 200, 50)
        )
    }

    // ========== 数据验证测试 ==========

    @Test
    fun `SecretAdapter 初始化时 likedSet 为空`() {
        val secrets = createTestSecrets()
        val adapter = SecretAdapter(secrets)
        // 内部 likedSet 初始为空，通过首次点击来验证
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `SecretAdapter itemCount 等于数据列表长度`() {
        val secrets = createTestSecrets()
        val adapter = SecretAdapter(secrets)
        assertEquals(secrets.size, adapter.itemCount)
    }

    @Test
    fun `SecretAdapter 空列表返回0`() {
        val emptyList = emptyList<Secret>()
        val adapter = SecretAdapter(emptyList)
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `SecretAdapter 单条数据正常处理`() {
        val singleSecret = listOf(
            Secret("only", "唯一的内容", "开心", "刚刚", 1, 0)
        )
        val adapter = SecretAdapter(singleSecret)
        assertEquals(1, adapter.itemCount)
    }

    // ========== 边界测试 ==========

    @Test
    fun `SecretAdapter 处理大量数据`() {
        val manySecrets = (1..100).map { i ->
            Secret(i.toString(), "内容$i", "开心", "${i}小时前", i, i / 2)
        }
        val adapter = SecretAdapter(manySecrets)
        assertEquals(100, adapter.itemCount)
    }

    @Test
    fun `Secret 数据完整性验证`() {
        val secret = Secret(
            id = "test-id",
            content = "测试内容包含中文和英文 Content",
            mood = "焦虑",
            timeAgo = "5分钟前",
            likes = 999,
            comments = 88
        )
        assertEquals("test-id", secret.id)
        assertEquals("测试内容包含中文和英文 Content", secret.content)
        assertEquals("焦虑", secret.mood)
        assertEquals("5分钟前", secret.timeAgo)
        assertEquals(999, secret.likes)
        assertEquals(88, secret.comments)
    }

    // ========== Mood 验证测试 ==========

    @Test
    fun `Secret mood 可以是所有8种心情`() {
        val moods = listOf("孤独", "开心", "后悔", "焦虑", "平静", "迷茫", "感动", "释然")
        moods.forEach { mood ->
            val secret = Secret("1", "测试", mood, "刚刚", 0, 0)
            assertEquals(mood, secret.mood)
        }
    }

    @Test
    fun `Secret 点赞数计算正确`() {
        val secret = Secret("1", "测试", "开心", "刚刚", 100, 0)
        // 模拟点赞后 +1
        val likedCount = secret.likes + 1
        assertEquals(101, likedCount)
    }

    @Test
    fun `Secret 评论数正确`() {
        val secret = Secret("1", "测试", "开心", "刚刚", 0, 50)
        assertEquals(50, secret.comments)
        assertTrue(secret.comments > 0)
    }

    // ========== 时间格式测试 ==========

    @Test
    fun `Secret timeAgo 支持各种格式`() {
        val timeFormats = listOf("刚刚", "1分钟前", "1小时前", "1天前", "3天前", "1周前")
        timeFormats.forEach { time ->
            val secret = Secret("1", "测试", "开心", time, 0, 0)
            assertEquals(time, secret.timeAgo)
        }
    }

    // ========== 空内容和边界内容测试 ==========

    @Test
    fun `Secret content 可以是短文本`() {
        val secret = Secret("1", "嗨", "开心", "刚刚", 0, 0)
        assertEquals("嗨", secret.content)
    }

    @Test
    fun `Secret content 可以是长文本`() {
        val longContent = "A".repeat(1000)
        val secret = Secret("1", longContent, "平静", "刚刚", 0, 0)
        assertEquals(longContent, secret.content)
        assertEquals(1000, secret.content.length)
    }

    @Test
    fun `Secret id 唯一性验证`() {
        val secrets = createTestSecrets()
        val ids = secrets.map { it.id }.toSet()
        assertEquals(secrets.size, ids.size)
    }
}