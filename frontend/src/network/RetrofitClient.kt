package com.zjgsu.treehole.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 是 Android 模拟器访问本机 localhost 的专属 IP
    private const val BASE_URL = "http://10.0.2.2:3001/"

    val postsApi: PostsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PostsApi::class.java)
    }
}
