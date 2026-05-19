package com.zjgsu.treehole.network

import retrofit2.Response
import retrofit2.http.*

// Chat Room DTOs
data class ChatRoomDto(
    val id: String,
    val participantId: String,
    val nickname: String,
    val avatar: String,
    val lastMessage: String,
    val lastMessageAt: String?,
    val unreadCount: Int,
    val messageLimitReached: Boolean
)

data class ChatRoomsResponse(val rooms: List<ChatRoomDto>)

data class ChatMessageDto(
    val id: String,
    val chatId: String,
    val from: String,
    val fromNickname: String,
    val content: String,
    val timestamp: String  // ISO 8601 format: "2026-05-08T10:36:35.174Z"
)

data class ChatHistoryResponse(val messages: List<ChatMessageDto>)

data class SendMessageResponse(
    val message: ChatMessageDto,
    val messageLimitReached: Boolean
)

data class UserInfoDto(
    val id: String,
    val nickname: String,
    val avatar: String,
    val isFollowing: Boolean
)

data class UserInfoResponse(val user: UserInfoDto)

data class FollowResponse(
    val isFollowing: Boolean,
    val message: String
)

data class StartChatResponse(
    val roomId: String,
    val participantId: String,
    val nickname: String,
    val avatar: String
)

interface WhisperApi {
    // Get all chat rooms
    @GET("api/whisper/rooms")
    suspend fun getChatRooms(): Response<ChatRoomsResponse>

    // Get chat history
    @GET("api/whisper/history/{roomId}")
    suspend fun getChatHistory(
        @Path("roomId") roomId: String,
        @Query("limit") limit: Int = 50
    ): Response<ChatHistoryResponse>

    // Send message
    @POST("api/whisper/message/{roomId}")
    suspend fun sendMessage(
        @Path("roomId") roomId: String,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    // Follow/unfollow user
    @POST("api/whisper/follow/{userId}")
    suspend fun followUser(
        @Path("userId") userId: String
    ): Response<FollowResponse>

    // Get user info
    @GET("api/whisper/user/{userId}")
    suspend fun getUserInfo(
        @Path("userId") userId: String
    ): Response<UserInfoResponse>

    // Start chat with user
    @POST("api/whisper/start/{userId}")
    suspend fun startChat(
        @Path("userId") userId: String
    ): Response<StartChatResponse>

    // Mark messages as read
    @POST("api/whisper/read/{roomId}")
    suspend fun markAsRead(
        @Path("roomId") roomId: String
    ): Response<Unit>
}

data class SendMessageRequest(val content: String)
