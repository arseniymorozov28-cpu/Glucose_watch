import CryptoKit
import Foundation

enum LibreLinkUpRegion: String, CaseIterable, Identifiable {
    case eu
    case us

    var id: String { rawValue }

    var baseURL: URL {
        switch self {
        case .eu:
            return URL(string: "https://api-eu.libreview.io/")!
        case .us:
            return URL(string: "https://api-us.libreview.io/")!
        }
    }
}

enum LibreLinkUpError: LocalizedError {
    case invalidResponse
    case loginFailed(String)
    case connectionsMissing
    case graphMissing
    case missingCredentials

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Unexpected server response."
        case .loginFailed(let message):
            return message
        case .connectionsMissing:
            return "No LibreLinkUp connections were returned."
        case .graphMissing:
            return "No glucose measurement is available yet."
        case .missingCredentials:
            return "Enter your LibreLinkUp email and password first."
        }
    }
}

struct LibreLinkUpSession {
    let token: String
    let accountId: String
    let patientId: String
    let patientName: String
}

final class LibreLinkUpClient {
    private let session: URLSession
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    init(session: URLSession = .shared) {
        self.session = session
    }

    func login(email: String, password: String, region: LibreLinkUpRegion) async throws -> LibreLinkUpSession {
        let requestBody = LoginRequest(email: email, password: password)
        let response: LoginResponse = try await send(
            path: "llu/auth/login",
            method: "POST",
            region: region,
            token: nil,
            accountId: nil,
            body: requestBody
        )

        guard response.status == 0, let data = response.data else {
            throw LibreLinkUpError.loginFailed(response.error?.message ?? "LibreLinkUp login failed.")
        }

        let accountId = Self.computeAccountId(userId: data.user.id)
        let connections: ConnectionsResponse = try await send(
            path: "llu/connections",
            method: "GET",
            region: region,
            token: data.authTicket.token,
            accountId: accountId,
            body: Optional<String>.none
        )

        guard connections.status == 0, let patient = connections.data.first else {
            throw LibreLinkUpError.connectionsMissing
        }

        return LibreLinkUpSession(
            token: data.authTicket.token,
            accountId: accountId,
            patientId: patient.patientId,
            patientName: "\(patient.firstName) \(patient.lastName)"
        )
    }

    func fetchCurrentGlucose(region: LibreLinkUpRegion, session: LibreLinkUpSession) async throws -> GlucoseSnapshot {
        let graph: GraphResponse = try await send(
            path: "llu/connections/\(session.patientId)/graph",
            method: "GET",
            region: region,
            token: session.token,
            accountId: session.accountId,
            body: Optional<String>.none
        )

        guard graph.status == 0, let latest = graph.data.connection.glucoseMeasurement else {
            throw LibreLinkUpError.graphMissing
        }

        return GlucoseSnapshot(
            valueMgDl: latest.valueInMgPerDl,
            valueMmolL: latest.valueInMgPerDl / 18.0182,
            trendArrow: latest.trendArrow ?? 3,
            trendMessage: latest.trendMessage,
            timestamp: latest.timestamp,
            isHigh: latest.isHigh,
            isLow: latest.isLow,
            patientName: session.patientName,
            targetLowMmolL: Double(graph.data.connection.targetLow) / 18.0182,
            targetHighMmolL: Double(graph.data.connection.targetHigh) / 18.0182
        )
    }

    private func send<T: Decodable, Body: Encodable>(
        path: String,
        method: String,
        region: LibreLinkUpRegion,
        token: String?,
        accountId: String?,
        body: Body?
    ) async throws -> T {
        let url = region.baseURL.appending(path: path)
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("llu.ios", forHTTPHeaderField: "product")
        request.setValue("4.16.0", forHTTPHeaderField: "version")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")

        if let token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let accountId {
            request.setValue(accountId, forHTTPHeaderField: "account-id")
        }

        if let body {
            request.httpBody = try encoder.encode(body)
        }

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw LibreLinkUpError.invalidResponse
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            let serverMessage = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            throw LibreLinkUpError.loginFailed(serverMessage)
        }

        return try decoder.decode(T.self, from: data)
    }

    private static func computeAccountId(userId: String) -> String {
        let digest = SHA256.hash(data: Data(userId.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
