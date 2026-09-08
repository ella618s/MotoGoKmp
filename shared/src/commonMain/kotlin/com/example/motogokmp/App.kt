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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var parkingList by remember { mutableStateOf<List<ParkingSpace>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        // 目前選擇的城市代碼，預設為 Taipei
        var selectedCity by remember { mutableStateOf("Taipei") }
        // 定位相關狀態
        var locationInfo by remember { mutableStateOf("尚未取得定位") }
        var currentLatLng by remember { mutableStateOf<LatLng?>(null) }
        val locationService = remember { LocationService() }
        // 檢視模式狀態：false 為清單模式，true 為地圖模式
        var isMapMode by remember { mutableStateOf(false) }
        var selectedParking by remember { mutableStateOf<ParkingSpace?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var showOnlyAvailable by remember { mutableStateOf(false) } // 是否只顯示有剩餘車位的開關
        // 記錄已被加入最愛的停車場名稱集合
        var favoriteNames by remember { mutableStateOf(emptySet<String>()) }
        var showOnlyFavorites by remember { mutableStateOf(false) } // 👈 是否只顯示最愛


        // 載入 API 資料
        // 🎯 監聽 selectedCity，只要城市改變就重新抓取對應 API 資料
        LaunchedEffect(selectedCity) {
            isLoading = true
            try {
                val api = ParkingApi()
                // 假設你在 ParkingApi 裡實作了可以傳入 city 的方法，例如 fetchParking(selectedCity)
                val result = api.fetchParking(selectedCity)
                parkingList = result
            } catch (e: Exception) {
                e.printStackTrace()
                parkingList = emptyList()
            } finally {
                isLoading = false
            }
        }

        // 🎯 結合關鍵字搜尋、車位快篩、最愛快篩與 GPS 距離排序的過濾清單
        val filteredParkingList = remember(parkingList, searchQuery, showOnlyAvailable, showOnlyFavorites, favoriteNames, currentLatLng) {
            val list = parkingList.filter { parking ->
                // 關鍵字過濾
                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    parking.name.contains(searchQuery, ignoreCase = true) ||
                            parking.address.contains(searchQuery, ignoreCase = true)
                }

                // 車位快篩過濾
                val matchesAvailable = if (showOnlyAvailable) {
                    parking.availableSpaces > 0
                } else {
                    true
                }

                // 收藏快篩過濾
                val matchesFavorite = if (showOnlyFavorites) {
                    favoriteNames.contains(parking.name)
                } else {
                    true
                }

                // 🎯 必須把三個條件全部結合起來！
                matchesSearch && matchesAvailable && matchesFavorite
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
                    title = {
                        // 顯示目前選擇的城市名稱
                        val cityName = when(selectedCity) {
                            "taipei" -> "台北市"
                            "NewTaipei" -> "新北市"
                            "Taichung" -> "台中市"
                            else -> selectedCity
                        }
                        Text(if (isMapMode) "MotoGo - $cityName 地圖" else "MotoGo - $cityName 即時停車位")
                    },
                    actions = {
                        // 🎯 城市快速切換按鈕範例 (你也可以改成 DropdownMenu 讓選擇更多元)
                        TextButton(onClick = {
                            // 🎯 決定下一個要切換的城市代碼
                            val nextCity = when(selectedCity) {
                                "Taipei" -> "NewTaipei"
                                "NewTaipei" -> "Taichung"
                                else -> "Taipei"
                            }

                            // 🎯 防呆檢查：如果目標城市跟現在一樣，就直接跳過
                            if (selectedCity == nextCity) {
                                return@TextButton // 這裡直接 return@TextButton 即可
                            }

                            selectedCity = nextCity
                        }) {
                            val nextCityName = when(selectedCity) {
                                "Taipei" -> "切換新北"
                                "NewTaipei" -> "切換台中"
                                else -> "切換台北"
                            }
                            Text(text = nextCityName, color = MaterialTheme.colorScheme.secondary)
                        }

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

                            // 🎯 把兩個 FilterChip 並排放在 Row 裡面
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = showOnlyAvailable,
                                    onClick = { showOnlyAvailable = !showOnlyAvailable },
                                    label = { Text("僅有車位") }
                                )

                                FilterChip(
                                    selected = showOnlyFavorites,
                                    onClick = { showOnlyFavorites = !showOnlyFavorites },
                                    label = { Text("我的最愛 ❤️") }
                                )
                            }
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


                    // 🎯 把兩個 FilterChip 並排放在 Row 裡面
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 🎯 車位快篩按鈕
                        FilterChip(
                            selected = showOnlyAvailable,
                            onClick = { showOnlyAvailable = !showOnlyAvailable },
                            label = { Text("僅有車位") }
                        )

                        FilterChip(
                            selected = showOnlyFavorites,
                            onClick = { showOnlyFavorites = !showOnlyFavorites },
                            label = { Text("我的最愛 ❤️") }
                        )
                    }

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
                                isFavorite = favoriteNames.contains(item.name), // 👈 傳入是否已收藏
                                onFavoriteClick = {                            // 👈 傳入點擊切換邏輯
                                    favoriteNames = if (favoriteNames.contains(item.name)) {
                                        favoriteNames - item.name
                                    } else {
                                        favoriteNames + item.name
                                    }
                                },
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedParking!!.name,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )

                            // 🎯 底部詳情卡片的愛心按鈕
                            IconButton(onClick = {
                                val name = selectedParking!!.name
                                favoriteNames = if (favoriteNames.contains(name)) {
                                    favoriteNames - name
                                } else {
                                    favoriteNames + name
                                }
                            }) {
                                val isFav = favoriteNames.contains(selectedParking!!.name)
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "收藏",
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
    isFavorite: Boolean,          // 👈 參數
    onFavoriteClick: () -> Unit,   // 👈 回呼
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // 🎯 愛心收藏按鈕
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "收藏",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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