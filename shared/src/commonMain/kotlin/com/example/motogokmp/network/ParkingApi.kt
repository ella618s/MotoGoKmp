package com.example.motogokmp.network

import com.example.motogokmp.models.ParkingSpace
import com.example.motogokmp.models.UnifiedParkingResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.* // 👈 引入 HttpResponse 以便拿原始字串
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

    suspend fun fetchParking(city: String): List<ParkingSpace> {
        val url = "https://motogo-backend-df5x.onrender.com/api/v1/parking/$city"

        // 把 API 網址印出來
        println("📌 [ParkingApi] Request URL: $url")

        try {
            // 先用 HttpResponse 取得原始回應字串
            val response: HttpResponse = httpClient.get(url)
            val responseBodyString = response.bodyAsText()

            // 把 API 回應的原始 JSON 印出來 (方便檢查格式與內容)
            println("📦 [ParkingApi] Raw JSON Response:\n$responseBodyString")

            // 使用 kotlinx.serialization 將字串解析成你的資料結構
            val parsedResponse = Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            }.decodeFromString<UnifiedParkingResponse>(responseBodyString)

            return parsedResponse.data
        } catch (e: Exception) {
            println("❌ [ParkingApi] Error fetching or parsing parking data: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}