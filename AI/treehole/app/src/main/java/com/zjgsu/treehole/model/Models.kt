package com.zjgsu.treehole.model

data class Secret(
    val id: String,
    val content: String,
    val mood: String,
    val timeAgo: String,
    val likes: Int,
    val comments: Int
)

data class Comment(
    val id: String,
    val content: String,
    val timeAgo: String,
    val likes: Int,
    val floor: Int
)

data class ChatMessage(
    val id: String,
    val text: String,
    val sender: String, // "me" or "them"
    val timestamp: String
)

data class ChatPreview(
    val id: String,
    val pseudonym: String,
    val lastMessage: String,
    val timeAgo: String,
    val unread: Int
)

data class Notification(
    val id: String,
    val type: String, // "like" or "comment"
    val message: String,
    val timeAgo: String,
    val read: Boolean
)

data class MoodItem(
    val name: String,
    val emoji: String,
    val colorStart: String,
    val colorEnd: String
)
