package com.zjgsu.treehole.network

import retrofit2.Response
import retrofit2.http.*

// Notification DTOs
data class NotificationDto(
    val id: String,
    val type: String,
    val message: String,
    val read: Boolean,
    val createdAt: String,
    val sender: NotificationSenderDto?,
    val postId: String?,
    val postContent: String?
)

data class NotificationSenderDto(
    val id: String,
    val nickname: String,
    val avatar: String
)

data class NotificationsResponse(
    val notifications: List<NotificationDto>
)

data class UnreadCountResponse(
    val count: Int
)

data class CleanupResponse(
    val success: Boolean,
    val message: String
)

interface NotificationsApi {
    @GET("api/notifications")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @PUT("api/notifications/{id}/read")
    suspend fun markAsRead(@Path("id") notificationId: String): Response<Unit>

    @PUT("api/notifications/read-all")
    suspend fun markAllAsRead(): Response<Unit>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    @POST("api/notifications/cleanup")
    suspend fun cleanupDuplicates(): Response<CleanupResponse>

    @POST("api/notifications/fix-comment-counts")
    suspend fun fixCommentCounts(): Response<CleanupResponse>

    @DELETE("api/notifications/clear-all")
    suspend fun clearAllNotifications(): Response<CleanupResponse>
}