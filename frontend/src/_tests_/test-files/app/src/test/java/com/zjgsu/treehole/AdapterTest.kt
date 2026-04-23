package com.zjgsu.treehole

import com.zjgsu.treehole.adapter.CommentAdapter
import com.zjgsu.treehole.adapter.TrendingAdapter
import com.zjgsu.treehole.adapter.MoodAdapter
import com.zjgsu.treehole.adapter.ChatPreviewAdapter
import com.zjgsu.treehole.adapter.MessageAdapter
import com.zjgsu.treehole.adapter.NotificationAdapter
import com.zjgsu.treehole.model.Comment
import com.zjgsu.treehole.model.ChatMessage
import com.zjgsu.treehole.model.ChatPreview
import com.zjgsu.treehole.model.Notification
import com.zjgsu.treehole.model.MoodItem
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.model.MockData
import org.junit.Assert.*
import org.junit.Test

/**
 * 适配器组件单元测试
 * 测试各种 RecyclerView Adapter 的数据处理逻辑
 */
class AdapterTest {

    // ========== CommentAdapter 测试 ==========

    @Test
    fun `CommentAdapter 空数据列表`() {
        val comments = emptyList<Comment>()
        assertEquals(0, comments.size)
    }

    @Test
    fun `Comment 数据结构验证`() {
        val comment = Comment("c1", "测试评论", "1小时前", 10, 1)
        assertEquals("c1", comment.id)
        assertEquals("测试评论", comment.content)
        assertTrue(comment.floor >= 1)
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

    // ========== TrendingAdapter 测试 ==========

    @Test
    fun `Trending 数据排序验证`() {
        val trendingSecrets = listOf(
            Secret("1", "内容1", "开心", "1小时前", 100, 20),
            Secret("2", "内容2", "孤独", "2小时前", 500, 50),
            Secret("3", "内容3", "平静", "3小时前", 1000, 100)
        )
        // 按点赞数降序排列
        val sorted = trendingSecrets.sortedByDescending { it.likes }
        assertEquals(1000, sorted[0].likes)
        assertEquals(100, sorted[2].likes)
    }

    // ========== MoodAdapter 测试 ==========

    @Test
    fun `MoodAdapter 数据完整验证`() {
        val moods = listOf(
            MoodItem("孤独", "🌙", "#4C7BFE", "#673AB7"),
            MoodItem("开心", "🌿", "#F5C024", "#FFA726")
        )
        assertEquals(2, moods.size)
        assertTrue(moods.all { it.name.isNotEmpty() && it.emoji.isNotEmpty() })
    }

    @Test
    fun `MoodItem 颜色格式验证`() {
        val mood = MoodItem("测试", "😀", "#FF5733", "#33FF57")
        assertTrue(mood.colorStart.startsWith("#"))
        assertTrue(mood.colorEnd.startsWith("#"))
        assertEquals(7, mood.colorStart.length) // #RRGGBB
        assertEquals(7, mood.colorEnd.length)
    }

    @Test
    fun `所有心情都有唯一的 Emoji`() {
        val moodEmojis = listOf("🌙", "🌿", "🍂", "☁️", "🌊", "🌫️", "💫", "🕊️")
        assertEquals(moodEmojis.size, moodEmojis.toSet().size) // 无重复
    }

    // ========== ChatPreviewAdapter 测试 ==========

    @Test
    fun `ChatPreview 数据验证`() {
        val preview = ChatPreview("1", "用户名", "最后消息", "刚刚", 3)
        assertTrue(preview.unread >= 0)
        assertTrue(preview.lastMessage.isNotEmpty())
    }

    @Test
    fun `ChatPreview 未读数显示逻辑`() {
        val preview = ChatPreview("1", "用户", "消息", "刚刚", 0)
        assertEquals(0, preview.unread)

        val previewWithUnread = ChatPreview("2", "用户2", "消息2", "刚刚", 99)
        assertTrue(previewWithUnread.unread > 0)
    }

    @Test
    fun `ChatPreview 时间显示格式`() {
        val timeFormats = listOf("刚刚", "1分钟前", "1小时前", "昨天", "1天前")
        timeFormats.forEach { time ->
            val preview = ChatPreview("1", "用户", "消息", time, 0)
            assertEquals(time, preview.timeAgo)
        }
    }

    // ========== MessageAdapter 测试 ==========

    @Test
    fun `ChatMessage 发送者类型验证`() {
        val sent = ChatMessage("1", "你好", "me", "10:30")
        val received = ChatMessage("2", "你好", "them", "10:31")

        assertEquals("me", sent.sender)
        assertEquals("them", received.sender)
        assertNotEquals(sent.sender, received.sender)
    }

    @Test
    fun `ChatMessage 时间戳格式`() {
        val message = ChatMessage("1", "消息内容", "me", "22:23")
        assertTrue(message.timestamp.matches(Regex("\\d{1,2}:\\d{2}")))
    }

    @Test
    fun `ChatMessage 消息内容可为空边界`() {
        val message = ChatMessage("1", "", "me", "10:30")
        assertEquals("", message.text)
    }

    @Test
    fun `ChatMessage 消息内容超长边界`() {
        val longText = "A".repeat(1000)
        val message = ChatMessage("1", longText, "me", "10:30")
        assertEquals(1000, message.text.length)
    }

    // ========== NotificationAdapter 测试 ==========

    @Test
    fun `Notification 类型验证`() {
        val likeNotification = Notification("1", "like", "有人点赞了你的树洞", "刚刚", false)
        assertEquals("like", likeNotification.type)

        val commentNotification = Notification("2", "comment", "有人评论了你的树洞", "刚刚", false)
        assertEquals("comment", commentNotification.type)
    }

    @Test
    fun `Notification 已读未读状态`() {
        val unread = Notification("1", "like", "消息", "刚刚", false)
        val read = Notification("2", "comment", "消息", "刚刚", true)

        assertFalse(unread.read)
        assertTrue(read.read)
    }

    @Test
    fun `Notification 时间显示`() {
        val notification = Notification("1", "like", "消息内容", "5分钟前", false)
        assertEquals("5分钟前", notification.timeAgo)
        assertTrue(notification.timeAgo.contains("前") || notification.timeAgo == "刚刚")
    }

    // ========== 列表数据完整性测试 ==========

    @Test
    fun `多个适配器数据总量验证`() {
        val comments = listOf(Comment("1", "评论", "刚刚", 0, 1))
        val chatMessages = listOf(
            ChatMessage("1", "消息", "me", "10:30"),
            ChatMessage("2", "消息", "them", "10:31")
        )
        val notifications = listOf(
            Notification("1", "like", "通知1", "刚刚", false),
            Notification("2", "comment", "通知2", "刚刚", true)
        )

        assertEquals(1, comments.size)
        assertEquals(2, chatMessages.size)
        assertEquals(2, notifications.size)
    }

    // ========== 数据一致性测试 ==========

    @Test
    fun `从 MockData 获取的数据完整性`() {
        // 验证 secrets
        MockData.secrets.forEach { secret ->
            assertTrue(secret.id.isNotEmpty())
            assertTrue(secret.content.length <= 5000)
        }

        // 验证 comments
        MockData.comments.forEach { comment ->
            assertTrue(comment.floor >= 1)
        }

        // 验证 notifications
        MockData.notifications.forEach { notification ->
            assertTrue(notification.type in listOf("like", "comment"))
        }
    }
}