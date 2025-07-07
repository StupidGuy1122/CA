package com.example.ca

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


// 数据模型
data class Ranking(
    val username: String,
    val usertime: String
)
data class ScoreRequest(
    val username: String,
    val usertime: String
)
data class LoginValid(
    val username: String,
    val password: String
)

// Retrofit 接口定义
interface ApiService {
    @POST("Score/AddScore")
    suspend fun submitScore(@Body body: ScoreRequest): Response<Unit>

    @GET("Score/Ranking")
    suspend fun getLeaderboard(): Response<List<Ranking>>

    @POST("User/Login")
    suspend fun login(@Body loginValid: LoginValid): Response<String>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:5114/"

        val instance: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }

        suspend fun submitScore(username: String, usertime: String) {
            instance.submitScore(ScoreRequest(username, usertime))
        }

        suspend fun getLeaderboard(): List<Ranking> {
            val response = instance.getLeaderboard()
            if (response.isSuccessful) {
                return response.body() ?: emptyList()
            } else {
                throw Exception("获取排行榜失败：${response.message()}")
            }
        }

        suspend fun login(username: String, password: String): String {
            val response = instance.login(LoginValid(username, password))
            return if (response.isSuccessful) {
                response.body() ?: "登录失败：服务器未返回信息"
            } else {
                throw Exception("请求失败：${response.message()}")
            }
        }
    }
}

