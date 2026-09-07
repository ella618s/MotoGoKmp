package com.example.motogokmp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual class LocationService : NSObject(), CLLocationManagerDelegateProtocol {
    private val locationManager = CLLocationManager()
    private var locationCallback: ((LatLng?) -> Unit)? = null

    init {
        locationManager.delegate = this
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    actual fun getCurrentLocation(onLocationReceived: (LatLng?) -> Unit) {
        locationCallback = onLocationReceived

        dispatch_async(dispatch_get_main_queue()) {
            val status = locationManager.authorizationStatus
            when (status) {
                kCLAuthorizationStatusNotDetermined -> {
                    locationManager.requestWhenInUseAuthorization()
                    // 第一次點擊要求權限後，可以先回傳 null 或等待狀態改變
                }
                kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> {
                    locationManager.startUpdatingLocation()
                }
                else -> {
                    locationCallback?.invoke(null)
                    locationCallback = null
                }
            }
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        locationManager.stopUpdatingLocation()
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        if (location != null) {
            @OptIn(ExperimentalForeignApi::class)
            val coordinate = location.coordinate.useContents {
                LatLng(latitude, longitude)
            }
            locationCallback?.invoke(coordinate)
        } else {
            locationCallback?.invoke(null)
        }
        locationCallback = null
    }

    override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
        if (didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
            didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedAlways
        ) {
            locationManager.startUpdatingLocation()
        } else if (didChangeAuthorizationStatus != kCLAuthorizationStatusNotDetermined) {
            locationCallback?.invoke(null)
            locationCallback = null
        }
    }
}