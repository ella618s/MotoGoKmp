package com.example.motogokmp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.motogokmp.models.ParkingSpace
import com.example.motogokmp.network.ParkingApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var parkingList by remember { mutableStateOf<List<ParkingSpace>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        // 定位相關狀態
        var locationInfo by remember { mutableStateOf("尚未取得定位") }
        var currentLatLng by remember { mutableStateOf<LatLng?>(null) }
        val locationService = remember { LocationService() }
        // 檢視模式狀態：false 為清單模式，true 為地圖模式
        var isMapMode by remember { mutableStateOf(false) }
        var selectedParking by remember { mutableStateOf<ParkingSpace?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var showOnlyAvailable by remember { mutableStateOf(false) } // 是否只顯示有剩餘車位的開關

        // 載入 API 資料
        LaunchedEffect(Unit) {
            try {
                val api = ParkingApi()
                val result = api.fetchTaipeiParking()
                parkingList = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }

        // 🎯 結合關鍵字搜尋、車位快篩與 GPS 距離排序的過濾清單
        val filteredParkingList = remember(parkingList, searchQuery, showOnlyAvailable, currentLatLng) {
            val list = parkingList.filter { parking ->
                // 關鍵字過濾
                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    parking.name.contains(searchQuery, ignoreCase = true) ||
                            parking.address.contains(searchQuery, ignoreCase = true)
                }

                // 車位快篩過濾（如果有打勾，就只留車位 > 0 的）
                val matchesAvailable = if (showOnlyAvailable) {
                    parking.availableSpaces > 0
                } else {
                    true
                }

                matchesSearch && matchesAvailable
            }

            // 距離排序
            if (currentLatLng != null) {
                list.sortedBy { parking ->
                    calculateDistanceKm(
                        currentLatLng!!.latitude,
                        currentLatLng!!.longitude,
                        parking.lat,
                        parking.lng
                    )
                }
            } else {
                list
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isMapMode) "MotoGo - 地圖模式" else "MotoGo - 台北市即時停車位") },
                    actions = {
                        TextButton(onClick = { isMapMode = !isMapMode }) {
                            Text(
                                text = if (isMapMode) "切換清單" else "切換地圖",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            // 🎯 根據不同模式使用不同的排版容器
            if (isMapMode) {
                // 地圖模式：使用 Box 讓搜尋與快篩面板懸浮在地圖上方
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    MapView(
                        modifier = Modifier.fillMaxSize(),
                        parkingList = filteredParkingList,
                        currentLatLng = currentLatLng,
                        onMarkerClick = { parking ->
                            selectedParking = parking
                        }
                    )

                    // 🎯 懸浮控制面板（把搜尋框與快篩按鈕整齊包在 Surface 裡，才不會互相遮擋）
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("搜尋停車場名稱或地址...") },
                                singleLine = true
                            )

                            FilterChip(
                                selected = showOnlyAvailable,
                                onClick = { showOnlyAvailable = !showOnlyAvailable },
                                label = { Text("僅顯示有剩餘車位") }
                            )
                        }
                    }
                }
            } else {
                // 清單模式：使用 Column 讓搜尋框與清單上下排列
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜尋停車場名稱或地址...") },
                        singleLine = true
                    )

                    // 🎯 車位快篩按鈕
                    FilterChip(
                        selected = showOnlyAvailable,
                        onClick = { showOnlyAvailable = !showOnlyAvailable },
                        label = { Text("僅顯示有剩餘車位") }
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = locationInfo, style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = {
                                        locationService.getCurrentLocation { latLng ->
                                            if (latLng != null) {
                                                currentLatLng = latLng
                                                locationInfo = "緯度: ${latLng.latitude}\n經度: ${latLng.longitude}"
                                            } else {
                                                locationInfo = "取得定位失敗或無權限"
                                            }
                                        }
                                    }) {
                                        Text("取得目前 GPS 座標")
                                    }
                                }
                            }
                        }

                        items(filteredParkingList) { item ->
                            ParkingItemCard(
                                item = item,
                                currentLatLng = currentLatLng,
                                onClick = {
                                    selectedParking = item
                                }
                            )
                        }
                    }
                }
            }

            // 底部詳情卡片 (ModalBottomSheet) 保持在最外層共用
            if (selectedParking != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedParking = null }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = selectedParking!!.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "地址：${selectedParking!!.address}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "剩餘車位：${selectedParking!!.availableSpaces}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedParking!!.availableSpaces > 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                openMapNavigation(
                                    lat = selectedParking!!.lat,
                                    lng = selectedParking!!.lng,
                                    name = selectedParking!!.name
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("開啟導航前往")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParkingItemCard(
    item: ParkingSpace,
    currentLatLng: LatLng?,
    onClick: () -> Unit
) {
    val distanceText = remember(currentLatLng, item) {
        if (currentLatLng != null) {
            val dist = calculateDistanceKm(
                currentLatLng.latitude,
                currentLatLng.longitude,
                item.lat,
                item.lng
            )
            if (dist < 1.0) {
                "${(dist * 1000).toInt()} 公尺"
            } else {
                val formatted = ((dist * 10).toInt() / 10.0)
                "$formatted 公里"
            }
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // 👈 讓整張卡片可被點擊
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = item.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (distanceText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "剩餘車位：${item.availableSpaces}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.availableSpaces > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}