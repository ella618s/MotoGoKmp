package com.example.motogokmp

data class LatLng(val latitude: Double, val longitude: Double)

expect class LocationService() {
    fun getCurrentLocation(onLocationReceived: (LatLng?) -> Unit)
}