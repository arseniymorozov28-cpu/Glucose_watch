import SwiftUI

struct ContentView: View {
    @StateObject private var model = PhoneAppModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    credentialsCard
                    snapshotCard
                    statusCard
                }
                .padding(20)
            }
            .navigationTitle("AppleGlucoBridge")
        }
    }

    private var credentialsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("LibreLinkUp")
                .font(.headline)

            TextField("Email", text: $model.email)
                .textInputAutocapitalization(.never)
                .keyboardType(.emailAddress)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)

            SecureField("Password", text: $model.password)
                .textFieldStyle(.roundedBorder)

            Picker("Region", selection: $model.region) {
                ForEach(LibreLinkUpRegion.allCases) { region in
                    Text(region.rawValue.uppercased()).tag(region)
                }
            }
            .pickerStyle(.segmented)

            HStack {
                Button("Connect and Sync") {
                    model.connectAndSync()
                }
                .buttonStyle(.borderedProminent)
                .disabled(model.isLoading)

                Button("Refresh") {
                    model.refreshNow()
                }
                .buttonStyle(.bordered)
                .disabled(model.isLoading)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var snapshotCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Latest Glucose")
                .font(.headline)

            if let snapshot = model.latestSnapshot {
                Text(String(format: "%.1f mmol/L", snapshot.valueMmolL))
                    .font(.system(size: 34, weight: .bold, design: .rounded))

                Text(String(format: "%.0f mg/dL", snapshot.valueMgDl))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                Text(snapshot.trendText)
                    .font(.body.weight(.medium))

                Text("Patient: \(snapshot.patientName)")
                    .foregroundStyle(.secondary)

                Text("Measured at: \(snapshot.timestamp)")
                    .foregroundStyle(.secondary)

                Text(
                    String(
                        format: "Target: %.1f - %.1f mmol/L",
                        snapshot.targetLowMmolL,
                        snapshot.targetHighMmolL
                    )
                )
                .foregroundStyle(.secondary)
            } else {
                Text("No glucose value has been synced yet.")
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var statusCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Bridge Status")
                .font(.headline)

            if model.isLoading {
                ProgressView()
            }

            Text(model.statusMessage)
                .foregroundStyle(.secondary)

            Text(model.bluetoothMessage)
                .foregroundStyle(.secondary)

            Text("Stage 1 covers iPhone login, glucose fetch, and BLE broadcasting for the Galaxy Watch receiver.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

#Preview {
    ContentView()
}
