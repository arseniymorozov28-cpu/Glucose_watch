import Foundation

struct GlucoseSnapshot: Codable, Equatable {
    let valueMgDl: Double
    let valueMmolL: Double
    let trendArrow: Int
    let trendMessage: String?
    let timestamp: String
    let isHigh: Bool
    let isLow: Bool
    let patientName: String
    let targetLowMmolL: Double
    let targetHighMmolL: Double

    var trendText: String {
        switch trendArrow {
        case 1: return "Rapidly falling"
        case 2: return "Falling"
        case 3: return "Stable"
        case 4: return "Rising"
        case 5: return "Rapidly rising"
        default: return "Unknown"
        }
    }

    var blePayloadData: Data {
        let payload = BLEPayload(
            valueMgDl: valueMgDl,
            valueMmolL: valueMmolL,
            trendArrow: trendArrow,
            trendText: trendText,
            trendMessage: trendMessage ?? "",
            timestamp: timestamp,
            isHigh: isHigh,
            isLow: isLow,
            patientName: patientName,
            targetLowMmolL: targetLowMmolL,
            targetHighMmolL: targetHighMmolL
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        return (try? encoder.encode(payload)) ?? Data()
    }
}

private struct BLEPayload: Codable {
    let valueMgDl: Double
    let valueMmolL: Double
    let trendArrow: Int
    let trendText: String
    let trendMessage: String
    let timestamp: String
    let isHigh: Bool
    let isLow: Bool
    let patientName: String
    let targetLowMmolL: Double
    let targetHighMmolL: Double
}
