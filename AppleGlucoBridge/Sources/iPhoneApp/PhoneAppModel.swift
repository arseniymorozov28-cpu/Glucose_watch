import Foundation
import SwiftUI

@MainActor
final class PhoneAppModel: ObservableObject {
    @Published var email: String
    @Published var password: String
    @Published var region: LibreLinkUpRegion
    @Published var latestSnapshot: GlucoseSnapshot?
    @Published var isLoading = false
    @Published var statusMessage = "Enter LibreLinkUp credentials to start syncing."
    @Published var bluetoothMessage = "Bluetooth bridge is preparing."

    private let store: CredentialsStore
    private let client: LibreLinkUpClient
    private var refreshTask: Task<Void, Never>?

    init(store: CredentialsStore = CredentialsStore(), client: LibreLinkUpClient = LibreLinkUpClient()) {
        self.store = store
        self.client = client
        self.email = store.email
        self.password = store.password
        self.region = LibreLinkUpRegion(rawValue: store.region) ?? .eu
        self.latestSnapshot = store.lastSnapshot

        if latestSnapshot != nil {
            statusMessage = "Loaded the last synced glucose value from local storage."
        }

        bluetoothMessage = BluetoothGlucoseBridge.shared.stateDescription

        if !email.isEmpty && !password.isEmpty {
            startRefreshLoop()
        }
    }

    deinit {
        refreshTask?.cancel()
    }

    func connectAndSync() {
        startRefreshLoop(forceImmediateRefresh: true)
    }

    func refreshNow() {
        refreshTask?.cancel()
        startRefreshLoop(forceImmediateRefresh: true)
    }

    private func startRefreshLoop(forceImmediateRefresh: Bool = false) {
        store.email = email.trimmingCharacters(in: .whitespacesAndNewlines)
        store.password = password
        store.region = region.rawValue

        refreshTask?.cancel()
        refreshTask = Task { [weak self] in
            guard let self else { return }

            if forceImmediateRefresh {
                await self.performRefresh(showLoader: true)
            } else {
                await self.performRefresh(showLoader: false)
            }

            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(300))
                if Task.isCancelled {
                    break
                }
                await self.performRefresh(showLoader: false)
            }
        }
    }

    private func performRefresh(showLoader: Bool) async {
        let cleanEmail = store.email.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanPassword = store.password

        guard !cleanEmail.isEmpty, !cleanPassword.isEmpty else {
            statusMessage = LibreLinkUpError.missingCredentials.localizedDescription
            return
        }

        if showLoader {
            isLoading = true
        }

        defer {
            isLoading = false
        }

        do {
            let currentSession = try await client.login(
                email: cleanEmail,
                password: cleanPassword,
                region: region
            )

            store.token = currentSession.token
            store.accountId = currentSession.accountId
            store.patientId = currentSession.patientId
            store.patientName = currentSession.patientName

            let snapshot = try await client.fetchCurrentGlucose(region: region, session: currentSession)
            latestSnapshot = snapshot
            store.lastSnapshot = snapshot
            BluetoothGlucoseBridge.shared.publish(snapshot: snapshot)
            bluetoothMessage = BluetoothGlucoseBridge.shared.stateDescription
            statusMessage = "Synced glucose and published the latest snapshot over BLE."
        } catch {
            statusMessage = error.localizedDescription
            bluetoothMessage = BluetoothGlucoseBridge.shared.stateDescription
        }
    }
}
