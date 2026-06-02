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
    // ===== Chat Rooms CRUD =====
    @GET("api/whisper/rooms")
    suspend fun getChatRooms(): Response<ChatRoomsResponse>

    @DELETE("api/whisper/rooms/{roomId}")
    suspend fun deleteChatRoom(@Path("roomId") roomId: String): Response<MessageResponse>

    // ===== Chat Messages =====
    @GET("api/whisper/history/{roomId}")
    suspend fun getChatHistory(
        @Path("roomId") roomId: String,
        @Query("limit") limit: Int = 50
    ): Response<ChatHistoryResponse>

    @POST("api/whisper/message/{roomId}")
    suspend fun sendMessage(
        @Path("roomId") roomId: String,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    @PUT("api/whisper/message/{messageId}")
    suspend fun updateMessage(
        @Path("messageId") messageId: String,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    @DELETE("api/whisper/message/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): Response<MessageResponse>

    // ===== Follow Management =====
    @POST("api/whisper/follow/{userId}")
    suspend fun followUser(@Path("userId") userId: String): Response<FollowResponse>

    @DELETE("api/whisper/follow/{userId}")
    suspend fun unfollowUser(@Path("userId") userId: String): Response<FollowResponse>

    // ===== User Info =====
    @GET("api/whisper/user/{userId}")
    suspend fun getUserInfo(@Path("userId") userId: String): Response<UserInfoResponse>

    // ===== Chat Actions =====
    @POST("api/whisper/start/{userId}")
    suspend fun startChat(@Path("userId") userId: String): Response<StartChatResponse>

    @POST("api/whisper/read/{roomId}")
    suspend fun markAsRead(@Path("roomId") roomId: String): Response<Unit>
}

data class SendMessageRequest(val content: String)
