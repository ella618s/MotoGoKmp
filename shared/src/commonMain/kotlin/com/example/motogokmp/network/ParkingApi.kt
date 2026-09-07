package com.example.motogokmp.network

import com.example.motogokmp.models.ParkingSpace
import com.example.motogokmp.models.UnifiedParkingResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ParkingApi {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    suspend fun fetchTaipeiParking(): List<ParkingSpace> {
        val url = "https://motogo-backend-df5x.onrender.com/api/v1/parking/taipei"
        val response: UnifiedParkingResponse = httpClient.get(url).body()
        return response.data
    }
}