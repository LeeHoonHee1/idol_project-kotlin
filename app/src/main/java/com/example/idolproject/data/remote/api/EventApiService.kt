package com.example.idolproject.data.remote.api

import com.example.idolproject.data.remote.dto.EventDto
import retrofit2.http.GET

interface EventApiService {

    @GET("idolproject/events.json")
    suspend fun getEvents(): List<EventDto>
}