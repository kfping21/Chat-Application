package com.zjgsu.treehole

import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.zjgsu.treehole.model.MockData
import com.zjgsu.treehole.model.Secret
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E 测试 - 数据流程
 * 测试从数据加载到 UI 显示的完整流程
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DataFlowE2ETest {

    @Rule
    fun activityRule() = ActivityScenarioRule(MainActivity::class.java)

    // ========== MockData 数据完整性测试 ==========

    @Test
    fun `MockData 的树洞数据格式正确`() {
        val secrets = MockData.secrets

        secrets.forEach { secret ->
            // 验证每条树洞都有必要的字段
            assertNotNull(secret.id)
            assertTrue(secret.id.isNotEmpty())

            assertNotNull(secret.content)
            assertTrue(secret.content.isNotEmpty())

            assertNotNull(secret.mood)
            assertTrue(secret.mood.isNotEmpty())

            assertNotNull(secret.timeAgo)
            assertTrue(secret.timeAgo.isNotEmpty())

            // 验证数字字段
            assertTrue(secret.likes >= 0)
            assertTrue(secret.comments >= 0)
        }
    }

    @Test
    fun `MockData 包含各种心情类型的树洞`() {
        val secrets = MockData.secrets
        val moodTypes = secrets.map { it.mood }.toSet()

        // 验证包含多种心情
        assertTrue(moodTypes.size >= 5)
    }

    @Test
    fun `MockData 聊天消息格式正确`() {
        val messages = MockData.mockMessages

        messages.forEach { message ->
            assertNotNull(message.id)
            assertNotNull(message.text)
            assertTrue(message.sender in listOf("me", "them"))
            assertNotNull(message.timestamp)
        }
    }

    // ========== Secret 模型边界测试 ==========

    @Test
    fun `Secret 的 content 可以是任何长度`() {
        // 短内容
        val shortSecret = Secret("1", "短", "开心", "刚刚", 0, 0)
        assertEquals("短", shortSecret.content)

        // 长内容（5000字符）
        val longContent = "A".repeat(5000)
        val longSecret = Secret("2", longContent, "平静", "刚刚", 0, 0)
        assertEquals(5000, longSecret.content.length)
    }

    @Test
    fun `Secret 的 likes 可以是0`() {
        val secret = Secret("1", "内容", "开心", "刚刚", 0, 0)
        assertEquals(0, secret.likes)
    }

    @Test
    fun `Secret 的 comments 可以是0`() {
        val secret = Secret("1", "内容", "开心", "刚刚", 0, 0)
        assertEquals(0, secret.comments)
    }

    // ========== 时间显示格式测试 ==========

    @Test
    fun `timeAgo 支持各种时间格式`() {
        val timeFormats = listOf("刚刚", "1分钟前", "30分钟前", "1小时前", "5小时前", "1天前", "3天前", "1周前")

        timeFormats.forEach { time ->
            val secret = Secret("1", "内容", "开心", time, 0, 0)
            assertEquals(time, secret.timeAgo)
        }
    }

    // ========== 心情数据完整性测试 ==========

    @Test
    fun `MoodItem 数据格式正确`() {
        MockData.moods.forEach { mood ->
            assertNotNull(mood.name)
            assertNotNull(mood.emoji)
            assertNotNull(mood.colorStart)
            assertNotNull(mood.colorEnd)

            // 验证颜色格式
            assertTrue(mood.colorStart.startsWith("#"))
            assertTrue(mood.colorEnd.startsWith("#"))
            assertEquals(7, mood.colorStart.length)
            assertEquals(7, mood.colorEnd.length)
        }
    }

    @Test
    fun `所有心情都有唯一 emoji`() {
        val emojis = MockData.moods.map { it.emoji }
        assertEquals(emojis.size, emojis.toSet().size)
    }

    // ========== ChatPreview 数据测试 ==========

    @Test
    fun `ChatPreview 未读数可以是0`() {
        val preview = MockData.chats.first()
        assertTrue(preview.unread >= 0)
    }

    @Test
    fun `ChatPreview 支持显示未读消息数`() {
        val unreadPreview = MockData.chats.find { it.unread > 0 }
        assertNotNull(unreadPreview)
    }

    // ========== Notification 数据测试 ==========

    @Test
    fun `Notification 类型只能是 like 或 comment`() {
        MockData.notifications.forEach { notification ->
            assertTrue(notification.type in listOf("like", "comment"))
        }
    }

    @Test
    fun `Notification 包含已读和未读状态`() {
        val hasUnread = MockData.notifications.any { !it.read }
        val hasRead = MockData.notifications.any { it.read }

        assertTrue(hasUnread)
        assertTrue(hasRead)
    }

    // ========== 数据一致性测试 ==========

    @Test
    fun `多条树洞数据 ID 都是唯一的`() {
        val ids = MockData.secrets.map { it.id }.toSet()
        assertEquals(MockData.secrets.size, ids.size)
    }

    @Test
    fun `评论数据 floor 号连续`() {
        val floors = MockData.comments.map { it.floor }.sorted()
        assertEquals(floors, (1..floors.size).toList())
    }
}