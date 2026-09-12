package com.example.motogokmp.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.example.motogokmp.models.ScooterRouteRequest
import com.example.motogokmp.models.ScooterRouteResponse

class RouteApiService(private val client: HttpClient) {
    private val baseUrl = "https://motogo-backend-df5x.onrender.com/api/v1/route/scooter"

    // 👈 這裡把 preference 參數加進來 (預設傳入 "standard")
    suspend fun getScooterRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        preference: String = "standard"
    ): ScooterRouteResponse {
        try {
            val response: ScooterRouteResponse = client.post(baseUrl) {
                contentType(ContentType.Application.Json)
                setBody(ScooterRouteRequest(
                    origin_lat = originLat,
                    origin_lng = originLng,
                    dest_lat = destLat,
                    dest_lng = destLng,
                    preference = preference
                ))
            }.body()

            return response // 👈 直接回傳整包
        } catch (e: Exception) {
            e.printStackTrace()
            // 發生例外時回傳空的 Response 避免崩潰
            return ScooterRouteResponse(status = "error", points = emptyList(), steps = emptyList())
        }
    }
}