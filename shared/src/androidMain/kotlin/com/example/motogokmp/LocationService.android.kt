package com.example.motogokmp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

actual class LocationService {
    actual constructor()

    @SuppressLint("MissingPermission")
    actual fun getCurrentLocation(onLocationReceived: (LatLng?) -> Unit) {
        val context = AppContext.instance

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            if (context is Activity) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    1001
                )
            }
            onLocationReceived(null)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) {
                onLocationReceived(LatLng(location.latitude, location.longitude))
            } else {
                onLocationReceived(null)
            }
        }.addOnFailureListener {
            onLocationReceived(null)
        }
    }
}