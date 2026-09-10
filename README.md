# MotoGo KMP - 跨平臺機車導航與即時停車位查詢系統

[English Version](./README.md)

MotoGo KMP 是一個現代化的跨平臺機車導航與即時停車輔助應用程式，採用 **Kotlin Multiplatform (KMP)** 與 **Jetpack Compose / Compose Multiplatform** 架構開發，並搭配使用 **Docker** 容器化部署的 **Go 後端服務**。

## 🛠️ 技術亮點
* **KMP 跨平臺架構**：實現 Android 與 iOS 核心商業邏輯、地理座標計算與資料模型高度共用。
* **後端與 DevOps 整合 (Go + Docker)**：
    * 使用高效能的 **Go (Golang)** 開發後端 RESTful API，負責管理即時停車位與導航資料。
    * 透過 **Docker** 將後端服務進行容器化封裝，確保開發與生產環境的一致性與高可擴展性。
* **響應式狀態管理**：結合 `StateFlow` 與宣告式 UI，實現跨平臺狀態的即時同步。
* **網路與序列化層**：透過 Ktor 統一處理跨平臺 RESTful API 請求，並使用 `kotlinx-serialization` 進行高效 JSON 序列化。
* **原生地圖深度整合**：
    * **Android**：整合 OSMDroid 框架，支援高效能地圖渲染與自定義圖層。
    * **iOS**：透過 Kotlin/Native 深度整合原生 `MKMapView`，完美處理自定義標記與 `MKPolyline` 導航路線渲染。
* **機車專屬導航邏輯**：針對臺灣都會區機車通勤與停車需求設計的路線指引與即時資訊介面。

## 🚀 核心技術突破
* **容器化 Go 後端架構**：以 Go 語言構築輕量、高效的後端服務來處理停車場空間與路線運算，並透過 Docker 實現一鍵部署與環境隔離。
* **單一資料源 (Single Source of Truth)**：將狀態管理集中於共用層的 `SharedViewModel`，利用 `StateFlow` 強制驅動 UI 更新，徹底解決跨平臺視圖不同步問題。
* **iOS 跨平臺指標與對接**：
    * 克服 Objective-C/Swift 與 Kotlin/Native 之間的型別與代理人（Delegate）轉型障礙。
    * 運用 `memScoped` 與 `allocArray` 安全處理 C 記憶體指標陣列，確保 iOS 端導航線與地圖標記穩定呈現。
* **即時停車與導航連動**：點擊地圖標記即可聯動後端數據、底部滑動視窗與導航線繪製。

## 📱 主要功能展示
* **即時剩餘車位查詢**：快速搜尋周邊停車場，透過 Go API 即時回傳剩餘車位數與詳細地址。
* **互動式地圖導航**：動態繪製藍色導航路線，並自動調整地圖視角範圍。
* **雙平臺原生體驗**：針對 Android 與 iOS 進行效能與 UI 適配，提供一致的高品質操作感受。

## 🏗️ 目前開發狀態
- [x] Go RESTful API 後端服務開發與資料庫串接
- [x] Docker 容器化建置與後端部署設定
- [x] KMP 跨平臺專案基礎架構與 Ktor 網路層整合
- [x] SharedViewModel 跨平臺狀態同步 (`StateFlow`)
- [x] OSMDroid 離線/線上地圖引擎 (Android)
- [x] 原生 `MKMapView` 整合、自定義標記與 Delegate 委派 (iOS)
- [x] `MKPolyline` 導航路線記憶體安全渲染與指標陣列處理 (iOS)
- [x] 即時停車場資料模型與雙端 UI 綁定

## 📄 授權與聲明 (License & Disclaimer)
* **版權所有**：© 2026 劉淑華 (Liu Shu-Hua). All rights reserved.
* **僅供作品展示**：本專案程式碼與架構僅作為個人技術作品集與展示使用。