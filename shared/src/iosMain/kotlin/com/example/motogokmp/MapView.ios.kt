package com.example.motogokmp

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.example.motogokmp.models.Coordinate
import com.example.motogokmp.models.ParkingSpace
import kotlinx.cinterop.*
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIColor
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapView(
    modifier: Modifier,
    parkingList: List<ParkingSpace>,
    currentLatLng: LatLng?,
    routePoints: List<Coordinate>,
    onMarkerClick: (ParkingSpace) -> Unit
) {
    val annotationMap = remember { mutableMapOf<MKPointAnnotation, ParkingSpace>() }

    // 建立地圖與代理人
    val mkMapView = remember {
        MKMapView().apply {
            showsUserLocation = true
        }
    }

    // 將 delegate 獨立成物件，利用 @Suppress 封鎖編譯器雜音
    remember(mkMapView) {
        val delegate = object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
                val annotation = didSelectAnnotationView.annotation as? MKPointAnnotation
                if (annotation != null) {
                    annotationMap[annotation]?.let { parking ->
                        onMarkerClick(parking)
                    }
                }
            }

            @Suppress("CONFLICTING_OVERLOADS", "RETURN_TYPE_MISMATCH_ON_OVERRIDE")
            override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
                return if (rendererForOverlay is MKPolyline) {
                    MKPolylineRenderer(polyline = rendererForOverlay).apply {
                        strokeColor = UIColor.blueColor
                        lineWidth = 6.0
                    }
                } else {
                    MKOverlayRenderer(overlay = rendererForOverlay)
                }
            }
        }
        mkMapView.delegate = delegate
        delegate
    }

    UIKitView(
        modifier = modifier,
        factory = { mkMapView },
        update = { view ->
            view.removeAnnotations(view.annotations)
            annotationMap.clear()
            view.removeOverlays(view.overlays)

            // 1. 畫出導航路線（安全的指標陣列寫法）
            if (routePoints.isNotEmpty()) {
                memScoped {
                    val coordsArray = allocArray<CLLocationCoordinate2D>(routePoints.size)
                    routePoints.forEachIndexed { index, pt ->
                        val currentCoord = coordsArray[index]
                        currentCoord.latitude = pt.lat
                        currentCoord.longitude = pt.lng
                    }
                    val polyline = MKPolyline.polylineWithCoordinates(coordsArray, count = routePoints.size.toULong())
                    view.addOverlay(polyline)
                    view.setVisibleMapRect(polyline.boundingMapRect, animated = true)
                }
            } else if (currentLatLng != null) {
                val coordinate = CLLocationCoordinate2DMake(currentLatLng.latitude, currentLatLng.longitude)
                view.setRegion(MKCoordinateRegionMake(coordinate, MKCoordinateSpanMake(0.01, 0.01)), true)
            }

            // 2. 加上停車場 Marker
            val annotations = parkingList.map { parking ->
                MKPointAnnotation().apply {
                    setCoordinate(CLLocationCoordinate2DMake(parking.lat, parking.lng))
                    setTitle(parking.name)
                    setSubtitle("剩餘車位: ${parking.availableSpaces}")
                }.also { annotationMap[it] = parking }
            }
            view.addAnnotations(annotations)
        }
    )
}