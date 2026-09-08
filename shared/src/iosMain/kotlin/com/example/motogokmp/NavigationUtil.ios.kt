package com.example.motogokmp

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openMapNavigation(lat: Double, lng: Double, name: String) {
    val urlString = "http://maps.apple.com/?daddr=$lat,$lng&dirflg=d"
    val url = NSURL.URLWithString(urlString)
    if (url != null) {
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}