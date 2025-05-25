package com.example.taskmanagement.network

import retrofit2.http.GET
import retrofit2.http.Query

interface TaskService {
    @GET("api/v1/tasks/suggestions")
    suspend fun getTaskSuggestions(@Query("category") category: String): List<TaskSuggestion>
}

data class TaskSuggestion(
    val title: String,
    val description: String
)

object TaskApi {
    private const val BASE_URL = "https://api.example.com/"  // Replace with actual API endpoint

    private val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()

    val service: TaskService = retrofit.create(TaskService::class.java)
} 