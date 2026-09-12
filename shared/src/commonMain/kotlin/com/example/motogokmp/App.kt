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
import com.example.motogokmp.models.RouteStep
import com.example.motogokmp.models.RoutingPreference
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
        var selectedCity by remember { mutableStateOf("Taipei") } // 目前選擇的城市代碼，預設為 Taipei
        var locationInfo by remember { mutableStateOf("正在取得定位...") }// 定位相關狀態
        var currentLatLng by remember { mutableStateOf<LatLng?>(null) }
        val locationService = remember { LocationService() }
        var isMapMode by remember { mutableStateOf(false) }// 檢視模式狀態：false 為清單模式，true 為地圖模式
        var selectedParking by remember { mutableStateOf<ParkingSpace?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var showOnlyAvailable by remember { mutableStateOf(false) } // 是否只顯示有剩餘車位的開關
        var favoriteNames by remember { mutableStateOf(emptySet<String>()) }// 記錄已被加入最愛的停車場名稱集合
        var showOnlyFavorites by remember { mutableStateOf(false) } // 是否只顯示最愛
        var currentRoutePoints by remember { mutableStateOf<List<Coordinate>>(emptyList()) }// 宣告儲存導航路線點位的變數
        val coroutineScope = rememberCoroutineScope()
        var isCalculatingRoute by remember { mutableStateOf(false) }
        var routePreference by remember { mutableStateOf(RoutingPreference()) }
        var currentRouteSteps by remember { mutableStateOf<List<RouteStep>>(emptyList()) }

        LaunchedEffect(Unit) {
            while (currentLatLng == null) {
                locationService.getCurrentLocation { latLng ->
                    if (latLng != null) {
                        currentLatLng = latLng
                    }
                }
                delay(500)
            }
        }

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

        val filteredParkingList = remember(
            parkingList,
            searchQuery,
            showOnlyAvailable,
            showOnlyFavorites,
            favoriteNames,
            currentLatLng
        ) {
            val list = parkingList.filter { parking ->
                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    parking.name.contains(searchQuery, ignoreCase = true) ||
                            parking.address.contains(searchQuery, ignoreCase = true)
                }

                val matchesAvailable = if (showOnlyAvailable) {
                    parking.availableSpaces > 0
                } else {
                    true
                }

                val matchesFavorite = if (showOnlyFavorites) {
                    favoriteNames.contains(parking.name)
                } else {
                    true
                }

                matchesSearch && matchesAvailable && matchesFavorite
            }

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
                        val cityName = when (selectedCity) {
                            "taipei" -> "台北市"
                            "NewTaipei" -> "新北市"
                            "Taichung" -> "台中市"
                            else -> selectedCity
                        }
                        Text(if (isMapMode) "MotoGo - \n(cityName 地圖" else "MotoGo -\n)cityName 即時停車位")
                    },
                    actions = {
                        TextButton(onClick = {
                            val nextCity = when (selectedCity) {
                                "Taipei" -> "NewTaipei"
                                "NewTaipei" -> "Taichung"
                                else -> "Taipei"
                            }

                            if (selectedCity == nextCity) {
                                return@TextButton
                            }

                            selectedCity = nextCity
                            currentRoutePoints = emptyList()
                            currentRouteSteps = emptyList()
                        }) {
                            val nextCityName = when (selectedCity) {
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
                            if (selectedParking?.name != parking.name) {
                                selectedParking = parking
                                currentRoutePoints = emptyList() // 🎯 切換新停車場時清空舊路線
                                currentRouteSteps = emptyList()  // 🎯 清空舊步驟
                            }
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
                                    if (selectedParking?.name != item.name) {
                                        selectedParking = item
                                        currentRoutePoints = emptyList() // 🎯 切換新停車場時清空舊路線
                                        currentRouteSteps = emptyList()  // 🎯 清空舊步驟
                                    }
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
                            .padding(24.dp)
                            .windowInsetsPadding(WindowInsets.safeDrawing), // 自動適應 iOS 安全邊距
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
                        Text("導航偏好：", style = MaterialTheme.typography.bodyMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = routePreference.mode == com.example.motogokmp.models.ScooterRouteMode.STANDARD,
                                onClick = {
                                    routePreference =
                                        routePreference.copy(mode = com.example.motogokmp.models.ScooterRouteMode.STANDARD)
                                },
                                label = { Text("標準") }
                            )

                            FilterChip(
                                selected = routePreference.mode == com.example.motogokmp.models.ScooterRouteMode.AVOID_BRIDGES,
                                onClick = {
                                    routePreference =
                                        routePreference.copy(mode = com.example.motogokmp.models.ScooterRouteMode.AVOID_BRIDGES)
                                },
                                label = { Text("避開高架橋") }
                            )
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

                        // 🎯 簡化為直接規劃路線並繪製在地圖上，點擊後即可開始導航
                        Button(
                            onClick = {
                                if (isCalculatingRoute) return@Button
                                isCalculatingRoute = true

                                isMapMode = true

                                val originLat = currentLatLng?.latitude ?: 25.0330
                                val originLng = currentLatLng?.longitude ?: 121.5654

                                coroutineScope.launch {
                                    try {
                                        val client = io.ktor.client.HttpClient {
                                            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                                                json(kotlinx.serialization.json.Json {
                                                    ignoreUnknownKeys = true
                                                })
                                            }
                                        }
                                        val routeApi = RouteApiService(client)

                                        val routeResponse = routeApi.getScooterRoute(
                                            originLat = originLat,
                                            originLng = originLng,
                                            destLat = destLat,
                                            destLng = destLng,
                                            preference = routePreference.mode.name.lowercase()
                                        )

                                        currentRoutePoints = routeResponse.points
                                        println("🧭 成功取得導航點數量: ${routeResponse.points.size}")

                                        client.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isCalculatingRoute = false
                                        selectedParking = null // 計算完成後收起面板，在地圖上檢視路線
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