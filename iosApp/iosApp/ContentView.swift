import SwiftUI
import CoreLocation
import shared

struct ContentView: View {
    // 建立一個常駐的 LocationManager 來確保權限請求順利送出
    private let locationManager = CLLocationManager()

    var body: some View {
        // 透過 UIViewControllerRepresentable 載入 Compose 畫面
        ComposeView()
            .ignoresSafeArea(.all)
            .onAppear {
                // 當 iOS App 一啟動，立刻在主執行緒向系統要求定位授權
                locationManager.requestWhenInUseAuthorization()
            }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // 呼叫 Kotlin 端的主畫面入口
        MainViewControllerKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
