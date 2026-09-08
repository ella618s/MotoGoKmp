package com.example.motogokmp

import android.content.Intent
import android.net.Uri

actual fun openMapNavigation(lat: Double, lng: Double, name: String) {
    // 直接使用專案中原本就有的 AppContext.instance
    val ctx = AppContext.instance ?: return

    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($name)")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(browserIntent)
    }
}