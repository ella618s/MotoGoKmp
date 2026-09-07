package com.example.motogokmp

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
        var currentLatLng by remember { mutableStateOf<LatLng?>(null) } // <--- 1. 記錄目前的經緯度
        val locationService = remember { LocationService() }

        // 載入 API 資料
        LaunchedEffect(Unit) {
            try {
                val api = ParkingApi()
                val result = api.fetchTaipeiParking()
                parkingList = result
                println("API 資料裡面的內容 ${parkingList.first()}")
                println("API 成功抓到資料，數量：${result.size}")
            } catch (e: Exception) {
                e.printStackTrace()
                println("API 抓取失敗：${e.message}")
            } finally {
                isLoading = false
            }
        }

        // 根據目前 GPS 排序停車場清單（如果有取得定位的話）
        val sortedParkingList = remember(parkingList, currentLatLng) {
            if (currentLatLng != null) {
                parkingList.sortedBy { parking ->
                    calculateDistanceKm(
                        currentLatLng!!.latitude,
                        currentLatLng!!.longitude,
                        parking.lat,
                        parking.lng
                    )
                }
            } else {
                parkingList
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("MotoGo - 台北市即時停車位") })
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 定位按鈕區塊
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                                                currentLatLng = latLng // <--- 3. 儲存經緯度觸發畫面重新排序
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

                        // 使用排序後的清單，並把當前座標傳進去計算距離
                        items(sortedParkingList) { item ->
                            ParkingItemCard(
                                item = item,
                                currentLatLng = currentLatLng
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParkingItemCard(item: ParkingSpace, currentLatLng: LatLng?) {
    // 計算與使用者的距離
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 第一行：停車場名稱
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            // 地址與右下角距離的橫向排版
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom // 讓右側文字靠底對齊
            ) {
                // 左側：地址（給定適當的 weight 避免擠壓）
                Text(
                    text = item.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // 右下角：距離
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

            // 剩餘車位
            Text(
                text = "剩餘車位：${item.availableSpaces}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.availableSpaces > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}