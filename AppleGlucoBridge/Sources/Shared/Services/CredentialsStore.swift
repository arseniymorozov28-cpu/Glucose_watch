import Foundation

final class CredentialsStore {
    private enum Keys {
        static let email = "applebridge.email"
        static let password = "applebridge.password"
        static let region = "applebridge.region"
        static let token = "applebridge.token"
        static let accountId = "applebridge.accountId"
        static let patientId = "applebridge.patientId"
        static let patientName = "applebridge.patientName"
        static let lastSnapshot = "applebridge.lastSnapshot"
    }

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    var email: String {
        get { defaults.string(forKey: Keys.email) ?? "" }
        set { defaults.set(newValue, forKey: Keys.email) }
    }

    var password: String {
        get { defaults.string(forKey: Keys.password) ?? "" }
        set { defaults.set(newValue, forKey: Keys.password) }
    }

    var region: String {
        get { defaults.string(forKey: Keys.region) ?? "eu" }
        set { defaults.set(newValue, forKey: Keys.region) }
    }

    var token: String {
        get { defaults.string(forKey: Keys.token) ?? "" }
        set { defaults.set(newValue, forKey: Keys.token) }
    }

    var accountId: String {
        get { defaults.string(forKey: Keys.accountId) ?? "" }
        set { defaults.set(newValue, forKey: Keys.accountId) }
    }

    var patientId: String {
        get { defaults.string(forKey: Keys.patientId) ?? "" }
        set { defaults.set(newValue, forKey: Keys.patientId) }
    }

    var patientName: String {
        get { defaults.string(forKey: Keys.patientName) ?? "" }
        set { defaults.set(newValue, forKey: Keys.patientName) }
    }

    var lastSnapshot: GlucoseSnapshot? {
        get {
            guard let data = defaults.data(forKey: Keys.lastSnapshot) else {
                return nil
            }

            return try? JSONDecoder().decode(GlucoseSnapshot.self, from: data)
        }
        set {
            if let newValue, let data = try? JSONEncoder().encode(newValue) {
                defaults.set(data, forKey: Keys.lastSnapshot)
            } else {
                defaults.removeObject(forKey: Keys.lastSnapshot)
            }
        }
    }
}
