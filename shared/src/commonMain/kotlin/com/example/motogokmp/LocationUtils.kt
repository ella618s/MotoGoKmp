package com.example.motogokmp

import kotlin.math.*

fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLon = (lon2 - lon1) * PI / 180.0

    val a = sin(dLat / 2).pow(2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLon / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}