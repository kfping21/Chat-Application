package com.zjgsu.treehole.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// ========== Post DTOs ==========
data class PostDto(
    val id: String,
    val content: String,
    val mood: String,
    val likes: Int,
    val commentCount: Int = 0,
    val createdAt: String,
    val user: PostUserDto?,
    val isLiked: Boolean = false
)

data class PostUserDto(
    val id: String,
    val nickname: String,
    val avatar: String
)

data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
    val hasMore: Boolean
)

data class FeedResponse(
    val posts: List<PostDto>,
    val pagination: PaginationInfo?
)

data class MyPostsResponse(
    val posts: List<PostDto>
)

data class PostResponse(
    val post: PostDto,
    val comments: List<CommentDto>
)

data class CreatePostRequest(
    val content: String,
    val mood: String = "平静"
)

// ========== Comment DTOs ==========
data class CommentDto(
    val id: String,
    val content: String,
    val createdAt: String,
    val user: PostUserDto?
)

data class CommentResponse(
    val comment: CommentDto
)

// ========== Like Response ==========
data class LikeResponse(
    val likes: Int,
    val isLiked: Boolean
)

data class AddCommentRequest(
    val content: String
)

data class CreatePostResponse(
    val message: String,
    val post: PostDto
)

interface PostsApi {
    @GET("api/posts/feed")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<FeedResponse>

    @GET("api/posts/my")
    suspend fun getMyPosts(): Response<MyPostsResponse>

    @GET("api/posts/user/{userId}")
    suspend fun getUserPosts(@Path("userId") userId: String): Response<MyPostsResponse>

    @GET("api/posts/{id}")
    suspend fun getPost(@Path("id") id: String): Response<PostResponse>

    @POST("api/posts")
    suspend fun createPost(@Body request: CreatePostRequest): Response<CreatePostResponse>

    @POST("api/posts/{id}/like")
    suspend fun likePost(@Path("id") id: String): Response<LikeResponse>

    @POST("api/posts/{id}/comment")
    suspend fun addComment(@Path("id") id: String, @Body request: AddCommentRequest): Response<CommentResponse>
}