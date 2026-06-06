package com.zjgsu.treehole.network

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 本地后端地址（主地址 + 备用地址自动回退）
    private const val PRIMARY_BASE_URL = "http://10.17.27.114:3001/"
    private const val SECONDARY_BASE_URL = "http://10.17.27.114:3001/"
    private val BASE_URL_CANDIDATES = listOf(PRIMARY_BASE_URL, SECONDARY_BASE_URL)

    @Volatile
    private var activeBaseUrl: String = PRIMARY_BASE_URL

    val BASE_URL: String
        get() = activeBaseUrl

    private fun rewriteBaseUrl(original: HttpUrl, targetBaseUrl: String): HttpUrl {
        val target = targetBaseUrl.toHttpUrl()
        return original.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
    }

    // OkHttpClient with optimized configuration
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("Connection", "keep-alive")
                .build()

            val firstBase = activeBaseUrl
            val firstRequest = request.newBuilder()
                .url(rewriteBaseUrl(request.url, firstBase))
                .build()

            try {
                chain.proceed(firstRequest)
            } catch (firstError: IOException) {
                val fallbackBase = BASE_URL_CANDIDATES.firstOrNull { it != firstBase }
                    ?: throw firstError

                val fallbackRequest = request.newBuilder()
                    .url(rewriteBaseUrl(request.url, fallbackBase))
                    .build()

                val fallbackResponse = chain.proceed(fallbackRequest)
                activeBaseUrl = fallbackBase
                fallbackResponse
            }
        }
        .build()

    // Auth interceptor - adds token to requests
    private val authInterceptor = Interceptor { chain ->
        val token = TokenManager.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    // OkHttpClient for posts with auth
    private val postsOkHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .addInterceptor(authInterceptor)
        .build()

    // OkHttpClient for auth
    private val authOkHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .addInterceptor(authInterceptor)
        .build()

    // Retrofit for auth
    private val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Retrofit for posts
    private val postsRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(postsOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy { authRetrofit.create(AuthApi::class.java) }
    val postsApi: PostsApi by lazy { postsRetrofit.create(PostsApi::class.java) }
    val whisperApi: WhisperApi by lazy { postsRetrofit.create(WhisperApi::class.java) }
    val notificationsApi: NotificationsApi by lazy { postsRetrofit.create(NotificationsApi::class.java) }
    val partyApi: PartyApi by lazy { postsRetrofit.create(PartyApi::class.java) }
}
