package com.example.motogokmp.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.example.motogokmp.models.ScooterRouteRequest
import com.example.motogokmp.models.ScooterRouteResponse
import com.example.motogokmp.models.Coordinate

class RouteApiService(private val client: HttpClient) {
    // 假設你的後端網址，如果在模擬器/真機測試，請確保網址正確 (例如 Render 上的 https://motogo-backend-df5x.onrender.com)
    private val baseUrl = "https://motogo-backend-df5x.onrender.com/api/v1/route/scooter"

    suspend fun getScooterRoute(originLat: Double, originLng: Double, destLat: Double, destLng: Double): List<Coordinate> {
        try {
            val response: ScooterRouteResponse = client.post(baseUrl) {
                contentType(ContentType.Application.Json)
                setBody(ScooterRouteRequest(origin_lat = originLat, origin_lng = originLng, dest_lat = destLat, dest_lng = destLng))
            }.body()

            return response.points
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}