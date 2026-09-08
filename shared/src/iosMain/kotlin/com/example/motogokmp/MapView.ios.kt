package com.example.motogokmp

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.example.motogokmp.models.ParkingSpace
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapView(
    modifier: Modifier,
    parkingList: List<ParkingSpace>,
    currentLatLng: LatLng?,
    onMarkerClick: (ParkingSpace) -> Unit
) {
    // 建立對照表，用來在點擊 Marker 時找回原始的 ParkingSpace 資料
    val annotationMap = remember { mutableMapOf<MKPointAnnotation, ParkingSpace>() }

    // 建立 iOS 代理來攔截點擊事件
    val delegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
                val annotation = didSelectAnnotationView.annotation as? MKPointAnnotation
                if (annotation != null) {
                    annotationMap[annotation]?.let { parking ->
                        onMarkerClick(parking)
                    }
                }
            }
        }
    }

    val mkMapView = remember {
        MKMapView().apply {
            showsUserLocation = true
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            mkMapView.delegate = delegate
            mkMapView
        },
        update = { view ->
            view.removeAnnotations(view.annotations)
            annotationMap.clear()

            val annotations = mutableListOf<MKPointAnnotation>()

            for (parking in parkingList) {
                val annotation = MKPointAnnotation().apply {
                    setCoordinate(CLLocationCoordinate2DMake(parking.lat, parking.lng))
                    setTitle(parking.name)
                    setSubtitle("剩餘車位: ${parking.availableSpaces}")
                }
                annotations.add(annotation)
                annotationMap[annotation] = parking
            }

            view.addAnnotations(annotations)

            if (currentLatLng != null) {
                val coordinate = CLLocationCoordinate2DMake(currentLatLng.latitude, currentLatLng.longitude)
                val span = MKCoordinateSpanMake(0.01, 0.01)
                view.setRegion(MKCoordinateRegionMake(coordinate, span), true)
            } else if (parkingList.isNotEmpty()) {
                val first = parkingList.first()
                val coordinate = CLLocationCoordinate2DMake(first.lat, first.lng)
                val span = MKCoordinateSpanMake(0.05, 0.05)
                view.setRegion(MKCoordinateRegionMake(coordinate, span), true)
            }
        }
    )
}