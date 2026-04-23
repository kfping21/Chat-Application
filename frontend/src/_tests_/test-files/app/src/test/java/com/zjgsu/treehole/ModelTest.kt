package com.zjgsu.treehole

import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.model.Comment
import com.zjgsu.treehole.model.ChatMessage
import com.zjgsu.treehole.model.ChatPreview
import com.zjgsu.treehole.model.MoodItem
import com.zjgsu.treehole.model.MockData
import org.junit.Assert.*
import org.junit.Test

/**
 * 模型类单元测试 - 完整覆盖
 */
class ModelTest {

    // ========== Secret 模型测试 ==========

    @Test
    fun `Secret 创建正常情况`() {
        val secret = Secret("1", "测试内容", "开心", "1小时前", 100, 20)
        assertEquals("1", secret.id)
        assertEquals("测试内容", secret.content)
        assertEquals("开心", secret.mood)
        assertEquals("1小时前", secret.timeAgo)
        assertEquals(100, secret.likes)
        assertEquals(20, secret.comments)
    }

    @Test
    fun `Secret 所有字段都可以正确访问`() {
        val secret = Secret(
            id = "test-id-123",
            content = "这是一条非常长的树洞内容，用来测试边界情况",
            mood = "焦虑",
            timeAgo = "刚刚",
            likes = 0,
            comments = 0
        )
        assertEquals("test-id-123", secret.id)
        assertEquals("焦虑", secret.mood)
        assertEquals(0, secret.likes)
    }

    @Test
    fun `Secret likes 和 comments 可以为0`() {
        val secret = Secret("1", "内容", "平静", "刚刚", 0, 0)
        assertEquals(0, secret.likes)
        assertEquals(0, secret.comments)
    }

    @Test
    fun `Secret 可以处理大数值`() {
        val secret = Secret("1", "热门内容", "开心", "1分钟前", Int.MAX_VALUE, Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, secret.likes)
        assertEquals(Int.MAX_VALUE, secret.comments)
    }

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
        val secrets = listOf(
            Secret("1", "内容1", "开心", "1小时前", 100, 20),
            Secret("2", "内容2", "孤独", "2小时前", 50, 10),
            Secret("3", "内容3", "平静", "3小时前", 200, 50)
        )
        val ids = secrets.map { it.id }.toSet()
        assertEquals(secrets.size, ids.size)
    }

    @Test
    fun `Secret mood 可以是所有8种心情`() {
        val moods = listOf("孤独", "开心", "后悔", "焦虑", "平静", "迷茫", "感动", "释然")
        moods.forEach { mood ->
            val secret = Secret("1", "测试", mood, "刚刚", 0, 0)
            assertEquals(mood, secret.mood)
        }
    }

    // ========== Comment 模型测试 ==========

    @Test
    fun `Comment 创建正常情况`() {
        val comment = Comment("c1", "评论内容", "2小时前", 50, 1)
        assertEquals("c1", comment.id)
        assertEquals("评论内容", comment.content)
        assertEquals("2小时前", comment.timeAgo)
        assertEquals(50, comment.likes)
        assertEquals(1, comment.floor)
    }

    @Test
    fun `Comment floor 从1开始`() {
        val comment = Comment("c1", "首层评论", "刚刚", 0, 1)
        assertEquals(1, comment.floor)
    }

    @Test
    fun `Comment floor 楼号递进`() {
        val comments = listOf(
            Comment("c1", "一楼", "刚刚", 0, 1),
            Comment("c2", "二楼", "刚刚", 0, 2),
            Comment("c3", "三楼", "刚刚", 0, 3)
        )
        assertEquals(1, comments[0].floor)
        assertEquals(2, comments[1].floor)
        assertEquals(3, comments[2].floor)
    }

    // ========== ChatMessage 模型测试 ==========

    @Test
    fun `ChatMessage sender 可以是 me`() {
        val message = ChatMessage("1", "你好", "me", "10:30")
        assertEquals("me", message.sender)
    }

    @Test
    fun `ChatMessage sender 可以是 them`() {
        val message = ChatMessage("2", "你好啊", "them", "10:31")
        assertEquals("them", message.sender)
    }

    @Test
    fun `ChatMessage 时间戳格式正确`() {
        val message = ChatMessage("3", "消息内容", "me", "22:23")
        assertEquals("22:23", message.timestamp)
    }

    @Test
    fun `ChatMessage 消息内容可为空`() {
        val message = ChatMessage("1", "", "me", "10:30")
        assertEquals("", message.text)
    }

    @Test
    fun `ChatMessage 消息内容超长`() {
        val longText = "A".repeat(1000)
        val message = ChatMessage("1", longText, "me", "10:30")
        assertEquals(1000, message.text.length)
    }

    // ========== ChatPreview 模型测试 ==========

    @Test
    fun `ChatPreview 创建正常情况`() {
        val preview = ChatPreview("1", "用户名", "最后一条消息", "刚刚", 5)
        assertEquals("1", preview.id)
        assertEquals("用户名", preview.pseudonym)
        assertEquals("最后一条消息", preview.lastMessage)
        assertEquals("刚刚", preview.timeAgo)
        assertEquals(5, preview.unread)
    }

    @Test
    fun `ChatPreview unread 可以为0`() {
        val preview = ChatPreview("1", "已读用户", "消息", "1天前", 0)
        assertEquals(0, preview.unread)
    }

    // ========== MoodItem 模型测试 ==========

    @Test
    fun `MoodItem 创建正常情况`() {
        val mood = MoodItem("开心", "🌿", "#F5C024", "#FFA726")
        assertEquals("开心", mood.name)
        assertEquals("🌿", mood.emoji)
        assertEquals("#F5C024", mood.colorStart)
        assertEquals("#FFA726", mood.colorEnd)
    }

    @Test
    fun `MoodItem 颜色格式验证`() {
        val mood = MoodItem("测试", "😀", "#FF5733", "#33FF57")
        assertTrue(mood.colorStart.startsWith("#"))
        assertTrue(mood.colorEnd.startsWith("#"))
        assertEquals(7, mood.colorStart.length)
        assertEquals(7, mood.colorEnd.length)
    }

    // ========== MockData 测试 ==========

    @Test
    fun `MockData secrets 列表不为空`() {
        assertTrue(MockData.secrets.isNotEmpty())
    }

    @Test
    fun `MockData secrets 每个元素都有效`() {
        MockData.secrets.forEach { secret ->
            assertTrue(secret.id.isNotEmpty())
            assertTrue(secret.content.isNotEmpty())
            assertTrue(secret.mood.isNotEmpty())
            assertTrue(secret.timeAgo.isNotEmpty())
            assertTrue(secret.likes >= 0)
            assertTrue(secret.comments >= 0)
        }
    }

    @Test
    fun `MockData comments 列表不为空`() {
        assertTrue(MockData.comments.isNotEmpty())
    }

    @Test
    fun `MockData mockMessages 发送者和接收者交替`() {
        val messages = MockData.mockMessages
        assertTrue(messages.size >= 2)
        assertEquals("them", messages[0].sender)
        assertEquals("me", messages[1].sender)
    }

    @Test
    fun `MockData chats 列表不为空`() {
        assertTrue(MockData.chats.isNotEmpty())
    }

    @Test
    fun `MockData notifications 包含未读和已读`() {
        val notifications = MockData.notifications
        assertTrue(notifications.any { !it.read })
        assertTrue(notifications.any { it.read })
    }

    @Test
    fun `MockData moods 包含所有8种心情`() {
        val moodNames = MockData.moods.map { it.name }
        listOf("孤独", "开心", "后悔", "焦虑", "平静", "迷茫", "感动", "释然").forEach { expected ->
            assertTrue("缺少心情: $expected", moodNames.contains(expected))
        }
    }

    @Test
    fun `MockData mySecrets 列表不为空`() {
        assertTrue(MockData.mySecrets.isNotEmpty())
    }

    @Test
    fun `MockData 所有心情都有唯一 emoji`() {
        val emojis = MockData.moods.map { it.emoji }
        assertEquals(emojis.size, emojis.toSet().size)
    }

    @Test
    fun `MockData notifications 类型验证`() {
        MockData.notifications.forEach { notification ->
            assertTrue(notification.type in listOf("like", "comment"))
        }
    }

    @Test
    fun `MockData ChatPreview 未读数可以是0`() {
        val preview = MockData.chats.first()
        assertTrue(preview.unread >= 0)
    }

    @Test
    fun `MockData ChatPreview 支持显示未读消息数`() {
        val unreadPreview = MockData.chats.find { it.unread > 0 }
        assertNotNull(unreadPreview)
    }
}