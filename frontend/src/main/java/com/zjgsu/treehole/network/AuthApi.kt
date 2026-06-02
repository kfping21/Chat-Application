package com.zjgsu.treehole.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// Auth DTOs
data class LoginRequest(
    val username: String,
    val password: String,
    val forceLogin: Boolean = false
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String = ""
)

data class AuthResponse(
    val message: String,
    val token: String? = null,
    val user: UserDto? = null,
    val forceLoginAvailable: Boolean = false
)

data class UserDto(
    val id: String,
    val username: String,
    val nickname: String,
    val avatar: String
)

data class UserResponse(
    val user: UserDto
)

data class AvatarUploadResponse(
    val user: UserDto,
    val avatarUrl: String
)

data class UserProfileResponse(
    val id: String,
    val nickname: String,
    val avatar: String,
    val bio: String,
    val createdAt: String,
    val postsCount: Int,
    val commentsCount: Int,
    val chatRoomsCount: Int,
    val isFollowing: Boolean,
    val isOnline: Boolean,
    val followersCount: Int,
    val followingCount: Int
)

data class ExploreUserDto(
    val id: String,
    val nickname: String,
    val avatar: String,
    val bio: String,
    val isOnline: Boolean,
    val lastOnlineAt: String?
)

data class ExploreUsersResponse(
    val users: List<ExploreUserDto>
)

data class FollowUserDto(
    val id: String,
    val nickname: String,
    val avatar: String,
    val bio: String,
    val isOnline: Boolean,
    val isFollowing: Boolean
)

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

data class FollowListResponse(
    val users: List<FollowUserDto>
)

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @Multipart
    @PUT("api/user/profile")
    suspend fun updateProfile(
        @Part("nickname") nickname: RequestBody?,
        @Part("avatar") avatar: RequestBody?,
        @Part("bio") bio: RequestBody?,
        @Part file: MultipartBody.Part?
    ): Response<UserResponse>

    @Multipart
    @POST("api/user/avatar")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part
    ): Response<AvatarUploadResponse>

    @GET("api/user/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): Response<UserProfileResponse>

    @GET("api/user/discover")
    suspend fun getExploreUsers(@Query("limit") limit: Int = 60): Response<ExploreUsersResponse>

    @GET("api/user/{id}/following")
    suspend fun getFollowing(@Path("id") userId: String): Response<FollowListResponse>

    @GET("api/user/{id}/followers")
    suspend fun getFollowers(@Path("id") userId: String): Response<FollowListResponse>

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<AuthResponse>
}
