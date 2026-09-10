package com.example.motogokmp

import android.graphics.Color // 確保有匯入 Android Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.motogokmp.models.ParkingSpace
import com.example.motogokmp.models.Coordinate // 👈 匯入 Coordinate 模型
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmMapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline // 👈 匯入 Polyline

@Composable
actual fun MapView(
    modifier: Modifier,
    parkingList: List<ParkingSpace>,
    currentLatLng: LatLng?,
    routePoints: List<Coordinate>, // 👈 1. 接收外部傳入的導航路徑點
    onMarkerClick: (ParkingSpace) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
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

            // 2. 如果有導航路線點，優先把它畫成 Polyline 覆蓋層
            if (routePoints.isNotEmpty()) {
                val polyline = Polyline().apply {
                    setPoints(routePoints.map { GeoPoint(it.lat, it.lng) })
                    color = Color.parseColor("#2196F3") // 藍色機車導航路線
                    width = 12f                        // 線條粗細
                }
                mapView.overlays.add(polyline)

                // 可以選擇將地圖中心移動到路線起點或第一個點
                mapView.controller.setCenter(GeoPoint(routePoints.first().lat, routePoints.first().lng))
            }

            if (currentLatLng != null) {
                val userPoint = GeoPoint(currentLatLng.latitude, currentLatLng.longitude)
                if (routePoints.isEmpty()) {
                    mapView.controller.setCenter(userPoint)
                }

                // 🎯 動態產生一個藍色三角形箭頭的 Bitmap
                val arrowBitmap = createNavigationArrowBitmap(mapView.context)

                val userMarker = Marker(mapView).apply {
                    position = userPoint
                    title = "我的位置"
                    icon = android.graphics.drawable.BitmapDrawable(mapView.context.resources, arrowBitmap) // 👈 換成三角形箭頭
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) // 錨點設在正中心，旋轉時才準確
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
                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(parking)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}

// 動態繪製綠色三角形導航箭頭
private fun createNavigationArrowBitmap(context: android.content.Context): android.graphics.Bitmap {
    val width = 80
    val height = 80
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // 箭頭內部填充（亮綠色）
    val fillPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#4CAF50") // 鮮豔綠色
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
    }

    // 箭頭外框（深綠色/墨綠色邊框，增加立體感）
    val strokePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#1B5E20") // 深綠邊框
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }

    // 描繪飽滿的三角形路徑
    val path = android.graphics.Path().apply {
        moveTo((width / 2).toFloat(), 5f)          // 頂端尖角
        lineTo((width - 10).toFloat(), (height - 15).toFloat()) // 右下角
        lineTo((width / 2).toFloat(), (height - 30).toFloat()) // 底部中央微凹（製造箭尾立體感）
        lineTo(10f, (height - 15).toFloat())         // 左下角
        close()
    }

    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)

    return bitmap
}