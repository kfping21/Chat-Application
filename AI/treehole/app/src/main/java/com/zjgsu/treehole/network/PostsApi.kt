package com.zjgsu.treehole.network

import retrofit2.Response
import retrofit2.http.GET

// 对应 db.json 中的帖子结构
data class PostDto(
    val id: String,
    val content: String,
    val mood: String,
    val likes: Int
)

interface PostsApi {
    @GET("posts")
    suspend fun getFeed(): Response<List<PostDto>>
}
