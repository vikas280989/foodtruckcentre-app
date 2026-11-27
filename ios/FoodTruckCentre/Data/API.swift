import Foundation
import Security

// MARK: - Configuration

enum AppConfig {
    /// Live API (WordPress plugin "FTC App API"). Debug builds can be pointed at a local test server
    /// by the automated UI tests through the FTC_API_BASE environment variable; release builds never can.
    static var apiBase: URL {
        #if DEBUG
        if let s = ProcessInfo.processInfo.environment["FTC_API_BASE"], let u = URL(string: s) { return u }
        #endif
        return URL(string: "https://foodtruckcentre.com.au/app-api/v1/")!
    }
    static let websiteBase = URL(string: "https://foodtruckcentre.com.au/")!
    static var userAgent: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1"
        return "FoodTruckCentreApp/\(v) (iOS)"
    }
}

// MARK: - Models

struct Profile: Codable, Equatable { var name = ""; var email = ""; var phone = "" }
struct Session: Codable, Equatable { var token: String; var profile: Profile }

struct MyEnquiry: Codable, Identifiable, Equatable {
    var id: String { enquiryId }
    let enquiryId: String
    let type: String
    let title: String
    let summary: String?
    let eventDate: String?
    let submittedAt: String
    let stage: String
    let stageLabel: String
    let delivered: Bool?

    enum CodingKeys: String, CodingKey {
        case enquiryId = "enquiry_id", type, title, summary, eventDate = "event_date", submittedAt = "submitted_at", stage, stageLabel = "stage_label", delivered
    }
}

struct WebPage { let title: String; let html: String; let modified: String }

/// Customer-friendly results (never raw technical errors).
enum APIError: Error, Equatable {
    case invalid(message: String, fields: [String: String])
    case failure(message: String, offline: Bool = false, unauthorized: Bool = false)

    var message: String {
        switch self { case .invalid(let m, _): return m; case .failure(let m, _, _): return m }
    }
    var fields: [String: String] { if case .invalid(_, let f) = self { return f }; return [:] }
    var offline: Bool { if case .failure(_, let o, _) = self { return o }; return false }
}

private struct ServerResponse: Decodable {
    var ok: Bool?
    var message: String?
    var fields: [String: String]?
    var token: String?
    var profile: Profile?
    var enquiries: [MyEnquiry]?
    var enquiryId: String?
    var contactEmail: String?
    enum CodingKeys: String, CodingKey { case ok, message, fields, token, profile, enquiries, enquiryId = "enquiry_id", contactEmail = "contact_email" }
}

// MARK: - Keychain-backed session

/// Stores the session token and profile in the iOS Keychain (encrypted by the system,
/// available only on this device after first unlock, never backed up to other devices).
@MainActor
final class SessionStore: ObservableObject {
    static let shared = SessionStore()
    @Published private(set) var session: Session?
    private let service = "au.com.foodtruckcentre.app.session"

    private init() {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-ui-testing-reset") { deleteKeychain() }
        #endif
        session = load()
    }

    var token: String? { session?.token }

    func save(_ s: Session) {
        session = s
        guard let data = try? JSONEncoder().encode(s) else { return }
        deleteKeychain()
        let q: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: "session",
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]
        SecItemAdd(q as CFDictionary, nil)
    }

    func updateProfile(_ p: Profile) {
        guard var s = session else { return }
        s.profile = p
        save(s)
    }

    func clear() {
        session = nil
        deleteKeychain()
    }

    private func load() -> Session? {
        let q: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: "session",
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var out: CFTypeRef?
        guard SecItemCopyMatching(q as CFDictionary, &out) == errSecSuccess, let data = out as? Data else { return nil }
        return try? JSONDecoder().decode(Session.self, from: data)
    }

    private func deleteKeychain() {
        let q: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service]
        SecItemDelete(q as CFDictionary)
    }
}

// MARK: - API client

@MainActor
final class API {
    static let shared = API()
    private let sessions = SessionStore.shared
    private let urlSession: URLSession = {
        let c = URLSessionConfiguration.default
        c.timeoutIntervalForRequest = 30
        c.timeoutIntervalForResource = 45
        c.requestCachePolicy = .reloadIgnoringLocalCacheData
        c.urlCache = nil
        return URLSession(configuration: c)
    }()

    // Submissions -------------------------------------------------------------

    func submit(_ endpoint: String, body: [String: Any]) async -> Result<String, APIError> {
        switch await call(endpoint, body: body) {
        case .success(let r):
            if let id = r.enquiryId, !id.isEmpty { return .success(id) }
            return .failure(.failure(message: "We couldn't send your enquiry right now. Please try again in a moment."))
        case .failure(let e): return .failure(e)
        }
    }

    // Accounts ----------------------------------------------------------------

    func register(name: String, email: String, phone: String, password: String) async -> Result<Session, APIError> {
        toSession(await call("auth-register.php", body: ["name": name, "email": email, "phone": phone, "password": password, "website": ""]))
    }

    func login(email: String, password: String) async -> Result<Session, APIError> {
        toSession(await call("auth-login.php", body: ["email": email, "password": password]))
    }

    func forgotPassword(email: String) async -> Result<String, APIError> {
        (await call("auth-forgot.php", body: ["email": email])).map { $0.message ?? "Check your email for a reset link." }
    }

    func logout() async {
        if sessions.token != nil { _ = await call("auth-logout.php", body: [:]) }
        sessions.clear()
    }

    func refreshProfile() async {
        if case .success(let r) = await call("me.php", body: nil), let p = r.profile { sessions.updateProfile(p) }
    }

    func updateProfile(name: String, phone: String) async -> Result<Profile, APIError> {
        switch await call("me.php", body: ["name": name, "phone": phone]) {
        case .success(let r):
            guard let p = r.profile else { return .failure(.failure(message: "Something went wrong. Please try again.")) }
            sessions.updateProfile(p)
            return .success(p)
        case .failure(let e): return .failure(e)
        }
    }

    func deleteAccount(password: String) async -> Result<Void, APIError> {
        switch await call("me-delete.php", body: ["password": password]) {
        case .success: sessions.clear(); return .success(())
        case .failure(let e): return .failure(e)
        }
    }

    func myEnquiries() async -> Result<[MyEnquiry], APIError> {
        (await call("my-enquiries.php", body: nil)).map { $0.enquiries ?? [] }
    }

    func contactEmail() async -> String? {
        if case .success(let r) = await call("app-config.php", body: nil), let e = r.contactEmail, e.contains("@") { return e }
        return nil
    }

    // Website content -----------------------------------------------------------

    func page(_ slug: String) async -> Result<WebPage, APIError> {
        var comps = URLComponents(url: AppConfig.websiteBase.appendingPathComponent("wp-json/wp/v2/pages"), resolvingAgainstBaseURL: false)!
        comps.queryItems = [.init(name: "slug", value: slug), .init(name: "_fields", value: "title,content,modified")]
        var req = URLRequest(url: comps.url!)
        req.setValue(AppConfig.userAgent, forHTTPHeaderField: "User-Agent")
        do {
            let (data, resp) = try await urlSession.data(for: req)
            guard (resp as? HTTPURLResponse)?.statusCode == 200,
                  let arr = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
                return .failure(.failure(message: "We couldn't load this page right now."))
            }
            guard let obj = arr.first else { return .failure(.failure(message: "This page isn't available on the website right now.")) }
            func rendered(_ k: String) -> String { ((obj[k] as? [String: Any])?["rendered"] as? String) ?? "" }
            return .success(WebPage(title: rendered("title"), html: rendered("content"), modified: obj["modified"] as? String ?? ""))
        } catch {
            return .failure(networkFailure(error))
        }
    }

    // Core --------------------------------------------------------------------

    /// GET when body is nil, otherwise POST JSON. Sends the session token when logged in.
    private func call(_ path: String, body: [String: Any]?) async -> Result<ServerResponse, APIError> {
        var req = URLRequest(url: AppConfig.apiBase.appendingPathComponent(path))
        req.setValue(AppConfig.userAgent, forHTTPHeaderField: "User-Agent")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        if let t = sessions.token { req.setValue(t, forHTTPHeaderField: "X-FTC-Token") }
        if let body {
            req.httpMethod = "POST"
            req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
            req.httpBody = try? JSONSerialization.data(withJSONObject: body)
        }
        do {
            let (data, resp) = try await urlSession.data(for: req)
            let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
            let parsed = try? JSONDecoder().decode(ServerResponse.self, from: data)
            if (200..<300).contains(code), parsed?.ok == true, let parsed { return .success(parsed) }
            if code == 401 && path != "auth-login.php" {
                sessions.clear()
                return .failure(.failure(message: parsed?.message ?? "Your session has ended. Please log in again.", unauthorized: true))
            }
            if code == 422, let parsed {
                return .failure(.invalid(message: parsed.message ?? "Please check the highlighted fields.", fields: parsed.fields ?? [:]))
            }
            if let m = parsed?.message, code >= 400 { return .failure(.failure(message: m)) }
            return .failure(.failure(message: "Something went wrong. Please try again in a moment."))
        } catch {
            return .failure(networkFailure(error))
        }
    }

    private func toSession(_ r: Result<ServerResponse, APIError>) -> Result<Session, APIError> {
        switch r {
        case .success(let s):
            guard let t = s.token, let p = s.profile else { return .failure(.failure(message: "Something went wrong. Please try again.")) }
            let session = Session(token: t, profile: p)
            sessions.save(session)
            return .success(session)
        case .failure(let e): return .failure(e)
        }
    }

    private func networkFailure(_ error: Error) -> APIError {
        let code = (error as? URLError)?.code
        switch code {
        case .notConnectedToInternet, .networkConnectionLost, .cannotFindHost, .dataNotAllowed:
            return .failure(message: "You appear to be offline. Check your internet connection and try again.", offline: true)
        case .timedOut:
            return .failure(message: "The connection timed out. Please check your internet and try again.", offline: true)
        case .secureConnectionFailed, .serverCertificateUntrusted, .serverCertificateHasBadDate:
            return .failure(message: "A secure connection couldn't be established. Please try again later.")
        default:
            return .failure(message: "The connection was interrupted. Please check your internet and try again.", offline: true)
        }
    }
}
