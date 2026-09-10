package com.example.motogokmp.models

import kotlinx.serialization.Serializable

@Serializable
data class ScooterRouteRequest(
    val origin_lat: Double,
    val origin_lng: Double,
    val dest_lat: Double,
    val dest_lng: Double
)

@Serializable
data class Coordinate(
    val lat: Double,
    val lng: Double
)

@Serializable
data class ScooterRouteResponse(
    val status: String,
    val points: List<Coordinate>
)