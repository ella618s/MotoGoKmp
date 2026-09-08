package com.example.motogokmp

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.motogokmp.models.ParkingSpace
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmMapView
import org.osmdroid.views.overlay.Marker

@Composable
actual fun MapView(
    modifier: Modifier,
    parkingList: List<ParkingSpace>,
    currentLatLng: LatLng?,
    onMarkerClick: (ParkingSpace) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // 在 factory 建立時直接賦予合規的 User-Agent
            Configuration.getInstance().userAgentValue = "MotoGoKmp/1.0 (Android)"

            OsmMapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(25.0478, 121.5170))
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            if (currentLatLng != null) {
                val userPoint = GeoPoint(currentLatLng.latitude, currentLatLng.longitude)
                mapView.controller.setCenter(userPoint)

                val userMarker = Marker(mapView).apply {
                    position = userPoint
                    title = "我的位置"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(userMarker)
            }

            for (parking in parkingList) {
                val point = GeoPoint(parking.lat, parking.lng)
                val marker = Marker(mapView).apply {
                    position = point
                    title = parking.name
                    snippet = "剩餘車位: ${parking.availableSpaces}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    // 🎯 點擊 Android 地圖 Marker 時觸發
                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(parking)
                        true // 回傳 true 代表事件已處理
                    }
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}