import Foundation

struct LoginRequest: Codable {
    let email: String
    let password: String
}

struct LoginResponse: Codable {
    let status: Int
    let data: LoginData?
    let error: ErrorInfo?
}

struct ErrorInfo: Codable {
    let message: String?
}

struct LoginData: Codable {
    let user: UserInfo
    let authTicket: AuthTicket
}

struct UserInfo: Codable {
    let id: String
    let firstName: String
    let lastName: String
    let email: String
    let country: String
}

struct AuthTicket: Codable {
    let token: String
    let expires: Int64
    let duration: Int64
}

struct ConnectionsResponse: Codable {
    let status: Int
    let data: [ConnectionData]
}

struct ConnectionData: Codable {
    let id: String
    let patientId: String
    let country: String
    let status: Int
    let firstName: String
    let lastName: String
    let targetLow: Int
    let targetHigh: Int
    let uom: Int
    let glucoseMeasurement: GlucoseMeasurement?
}

struct GraphResponse: Codable {
    let status: Int
    let data: GraphData
}

struct GraphData: Codable {
    let connection: ConnectionData
    let graphData: [GlucoseMeasurement]
}

struct GlucoseMeasurement: Codable {
    let factoryTimestamp: String
    let timestamp: String
    let type: Int
    let valueInMgPerDl: Double
    let trendArrow: Int?
    let trendMessage: String?
    let measurementColor: Int
    let glucoseUnits: Int
    let value: Double
    let isHigh: Bool
    let isLow: Bool

    enum CodingKeys: String, CodingKey {
        case factoryTimestamp = "FactoryTimestamp"
        case timestamp = "Timestamp"
        case type
        case valueInMgPerDl = "ValueInMgPerDl"
        case trendArrow = "TrendArrow"
        case trendMessage = "TrendMessage"
        case measurementColor = "MeasurementColor"
        case glucoseUnits = "GlucoseUnits"
        case value = "Value"
        case isHigh
        case isLow
    }
}
