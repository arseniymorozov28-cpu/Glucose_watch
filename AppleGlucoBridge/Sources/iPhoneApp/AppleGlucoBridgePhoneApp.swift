import SwiftUI

@main
struct AppleGlucoBridgePhoneApp: App {
    init() {
        _ = BluetoothGlucoseBridge.shared
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
