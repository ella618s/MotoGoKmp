package com.example.motogokmp.models

import kotlinx.serialization.Serializable

@Serializable
data class ScooterRouteRequest(
    val origin_lat: Double,
    val origin_lng: Double,
    val dest_lat: Double,
    val dest_lng: Double,
    val preference: String = "standard" // 機車偏好參數 (對應後端接收的模式字串)
)

@Serializable
data class Coordinate(
    val lat: Double,
    val lng: Double
)

@Serializable
data class ScooterRouteResponse(
    val status: String,
    val points: List<Coordinate>,
    val steps: List<RouteStep> = emptyList() // 步驟清單
)

@Serializable
enum class ScooterRouteMode {
    STANDARD,          // 標準導航
    AVOID_BRIDGES,     // 避開部分機車禁行高架橋
    STRICT_SCOOTER     // 嚴格機車專用道優化
}

@Serializable
data class RoutingPreference(
    val mode: ScooterRouteMode = ScooterRouteMode.STANDARD,
    val avoidTunnels: Boolean = true,
    val preferScooterFriendlyLanes: Boolean = true
)

@Serializable
data class RouteStep(
    val instruction: String, // 例如：「向右轉進入松山路」
    val distance: String,    // 例如：「600 公尺」
    val turnType: String     // 轉彎類型（可搭配圖示：turn_right, straight 等）
)