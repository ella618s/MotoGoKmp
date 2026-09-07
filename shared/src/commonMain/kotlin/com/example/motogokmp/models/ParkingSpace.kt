package com.example.motogokmp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnifiedParkingResponse(
    val status: String,
    val count: Int,
    val data: List<ParkingSpace>
)

@Serializable
data class ParkingSpace(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    @SerialName("available_spaces")
    val availableSpaces: Int
)