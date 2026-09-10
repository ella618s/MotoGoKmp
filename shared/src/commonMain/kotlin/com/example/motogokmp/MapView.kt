package com.example.motogokmp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.motogokmp.models.Coordinate
import com.example.motogokmp.models.ParkingSpace

@Composable
expect fun MapView(
    modifier: Modifier = Modifier,
    parkingList: List<ParkingSpace>,
    currentLatLng: LatLng?,
    routePoints: List<Coordinate>,
    onMarkerClick: (ParkingSpace) -> Unit
)