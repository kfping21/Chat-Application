package com.zjgsu.treehole.model

data class Secret(
    val id: String,
    val content: String,
    val mood: String,
    val timeAgo: String,
    var likes: Int,
    var comments: Int,
    val avatar: String = "",
    val nickname: String = "",
    val isLiked: Boolean = false,
    var userId: String = "" // Author's user ID for chat
)

data class Comment(
    val id: String,
    val content: String,
    val timeAgo: String,
    val likes: Int,
    val floor: Int,
    val avatar: String = "",
    val nickname: String = "",
    val userId: String = "",
    val isAuthor: Boolean = false
)

data class ChatMessage(
    val id: String,
    val text: String,
    val sender: String,
    val timestamp: String
)

data class ChatPreview(
    val id: String,
    val participantId: String,
    val nickname: String,
    val avatar: String,
    val lastMessage: String,
    val timeAgo: String,
    val unread: Int
)

data class Notification(
    val id: String,
    val type: String, // "like", "comment", "follow"
    val message: String,
    val read: Boolean,
    val createdAt: String,
    val sender: NotificationSender?,
    val postId: String?,
    val postContent: String?
)

data class NotificationSender(
    val id: String,
    val nickname: String,
    val avatar: String
)

data class MoodItem(
    val name: String,
    val emoji: String,
    val colorStart: String,
    val colorEnd: String
)

data class MyComment(
    val id: String,
    val content: String,
    val timeAgo: String,
    val postId: String,
    val postContent: String,
    val mood: String
)