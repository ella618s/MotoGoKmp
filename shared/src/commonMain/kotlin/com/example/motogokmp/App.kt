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
import com.example.motogokmp.models.Coordinate
import com.example.motogokmp.network.RouteApiService
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var parkingList by remember { mutableStateOf<List<ParkingSpace>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        // 目前選擇的城市代碼，預設為 Taipei
        var selectedCity by remember { mutableStateOf("Taipei") }
        // 定位相關狀態
        var locationInfo by remember { mutableStateOf("正在取得定位...") }
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
        // 宣告儲存導航路線點位的變數
        var currentRoutePoints by remember { mutableStateOf<List<Coordinate>>(emptyList()) }
        val coroutineScope = rememberCoroutineScope()
        var isCalculatingRoute by remember { mutableStateOf(false) }

        // 🎯 1. App 一開機立刻自動請求權限並取得定位，解決第一次導航延遲的問題
        LaunchedEffect(Unit) {
            while (currentLatLng == null) {
                locationService.getCurrentLocation { latLng ->
                    if (latLng != null) {
                        currentLatLng = latLng
                    }
                }
                delay(500) // 每 0.5 秒自動重試一次，直到使用者按允許並成功取得座標
            }
        }

        // 載入 API 資料
        // 🎯 監聽 selectedCity，只要城市改變就重新抓取對應 API 資料
        LaunchedEffect(selectedCity) {
            isLoading = true
            try {
                val api = ParkingApi()
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
                        val cityName = when(selectedCity) {
                            "taipei" -> "台北市"
                            "NewTaipei" -> "新北市"
                            "Taichung" -> "台中市"
                            else -> selectedCity
                        }
                        Text(if (isMapMode) "MotoGo - $cityName 地圖" else "MotoGo - $cityName 即時停車位")
                    },
                    actions = {
                        TextButton(onClick = {
                            val nextCity = when(selectedCity) {
                                "Taipei" -> "NewTaipei"
                                "NewTaipei" -> "Taichung"
                                else -> "Taipei"
                            }

                            if (selectedCity == nextCity) {
                                return@TextButton
                            }

                            selectedCity = nextCity
                            currentRoutePoints = emptyList() // 切換城市時立刻清空舊導航線！
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
            if (isMapMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    MapView(
                        modifier = Modifier.fillMaxSize(),
                        parkingList = filteredParkingList,
                        currentLatLng = currentLatLng,
                        routePoints = currentRoutePoints,
                        onMarkerClick = { parking ->
                            selectedParking = parking
                        }
                    )
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

                    // 🎯 這裡已經把原本佔空間又醜的「取得目前 GPS 座標」手動按鈕卡片整個移除！

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredParkingList) { item ->
                            ParkingItemCard(
                                item = item,
                                currentLatLng = currentLatLng,
                                isFavorite = favoriteNames.contains(item.name),
                                onFavoriteClick = {
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

            // 底部詳情卡片 (ModalBottomSheet)
            if (selectedParking != null) {
                val currentParking = selectedParking!!

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
                                text = currentParking.name,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(onClick = {
                                val name = currentParking.name
                                favoriteNames = if (favoriteNames.contains(name)) {
                                    favoriteNames - name
                                } else {
                                    favoriteNames + name
                                }
                            }) {
                                val isFav = favoriteNames.contains(currentParking.name)
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "收藏",
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "地址：${currentParking.address}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "剩餘車位：${currentParking.availableSpaces}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentParking.availableSpaces > 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val destLat = currentParking.lat
                        val destLng = currentParking.lng

                        Button(
                            onClick = {
                                if (isCalculatingRoute) return@Button
                                isCalculatingRoute = true

                                isMapMode = true
                                selectedParking = null

                                // 🎯 因為一開機已經抓過位置，此時直接使用現有的 currentLatLng（若還沒抓到則給預設值）
                                val originLat = currentLatLng?.latitude ?: 25.0330
                                val originLng = currentLatLng?.longitude ?: 121.5654

                                coroutineScope.launch {
                                    try {
                                        val client = io.ktor.client.HttpClient {
                                            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                                                json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
                                            }
                                        }
                                        val routeApi = RouteApiService(client)

                                        val routePoints = routeApi.getScooterRoute(
                                            originLat = originLat,
                                            originLng = originLng,
                                            destLat = destLat,
                                            destLng = destLng
                                        )

                                        println("🧭 成功取得導航點數量: ${routePoints.size}")
                                        currentRoutePoints = routePoints
                                        client.close()
                                    } catch (e: Exception) {
                                        println("❌ 導航請求失敗: ${e.message}")
                                        e.printStackTrace()
                                    } finally {
                                        isCalculatingRoute = false
                                    }
                                }
                            },
                            enabled = !isCalculatingRoute,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isCalculatingRoute) "正在規劃導航..." else "規劃機車導航路線")
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
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
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
            .clickable(onClick = onClick),
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