# MotoGo KMP - Cross-Platform Scooter Navigation & Real-Time Parking System

[繁體中文版說明 (Traditional Chinese README)](./README_zh.md)

MotoGo KMP is a modern cross-platform mobile navigation and parking assistance application built with **Kotlin Multiplatform (KMP)** and **Jetpack Compose / Compose Multiplatform**, powered by a custom **Go backend** deployed via **Docker**.

## 🛠️ Tech Highlights
* **KMP Cross-Platform Architecture**: Shares core business logic, geographic coordinate calculations, and API data models between Android and iOS.
* **Backend & DevOps (Go + Docker)**:
    * Built a high-performance RESTful API service using **Go (Golang)** to manage parking data and navigation routes.
    * Containerized the backend service using **Docker** for consistent, scalable deployment.
* **Reactive State Management**: Combines `StateFlow` and Compose/SwiftUI reactive paradigms to ensure real-time synchronization across platforms.
* **Network & Serialization**: Utilizes Ktor for unified RESTful API requests with `kotlinx-serialization` for robust JSON parsing.
* **Native Map Integration**:
    * **Android**: Powered by OSMDroid for efficient offline mapping and custom overlays.
    * **iOS**: Deeply integrates native `MKMapView` via Kotlin/Native interoperability, featuring custom annotation markers and `MKPolyline` route rendering.
* **Scooter-Centric Routing**: Optimized navigation logic designed around Taiwan's unique white-plate scooter routing rules and parking infrastructure.

## 🚀 Core Technical Achievements
* **Containerized Go Backend Architecture**: Developed a robust backend in Go to handle parking inventory and spatial queries, fully dockerized to ensure seamless environment parity from development to production.
* **Single Source of Truth (SSOT)**: Centralized state management within `SharedViewModel` using `StateFlow` to eliminate UI desynchronization between native views and shared code.
* **Cross-Platform Native Map Interop**:
    * Overcame complex Objective-C/Swift and Kotlin/Native bridging challenges to handle custom `MKMapViewDelegate` and memory-safe C pointer arrays (`memScoped` and `allocArray`) for dynamic route drawing.
* **Real-Time Parking Integration**: Fetches and renders live parking space availability and detailed addresses, presenting data via fluid lists and interactive map markers.

## 📱 Features & UI Showcase
* **Real-Time Availability**: Quick search for nearby parking lots with live available space counters backed by the Go API.
* **Interactive Navigation**: Dynamic polyline route rendering paired with turn-by-turn visual indicators.
* **Bilingual Support & UI Flexibility**: Clean, responsive layout adapted for both Android and iOS design patterns.

## 🏗️ Development Status
- [x] Go RESTful API backend development and database integration
- [x] Docker containerization and deployment configuration for backend services
- [x] KMP cross-platform project structure and Ktor networking setup
- [x] SharedViewModel state synchronization logic (`StateFlow`)
- [x] OSMDroid offline map rendering engine (Android)
- [x] Native `MKMapView` integration, custom annotations, and delegate handling (iOS)
- [x] `MKPolyline` route rendering with safe memory allocation (iOS)
- [x] Real-time parking space data model and UI binding

## 📄 License & Disclaimer
* **Copyright**: © 2026 劉淑華 (Liu Shu-Hua). All rights reserved.
* **Portfolio Notice**: This project is developed for technical demonstration and portfolio purposes.