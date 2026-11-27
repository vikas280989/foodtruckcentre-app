import Foundation
import SwiftUI
import UIKit

struct BannerState: Equatable { let message: String; let offline: Bool }

enum Loadable<T> {
    case loading
    case ready(T)
    case error(String, offline: Bool)
}

// MARK: - Quote form

enum QuoteField: String, CaseIterable {
    case cuisines, name, email, phone, eventDate = "event_date", foodTime = "food_time", address = "event_address", postcode,
         eventType = "event_type", meals, paying, power, privateLand = "private_land", entrance, info = "additional_info"
}

/// Holds the quote form for the life of the app so switching tabs never loses typed details. Nothing is written to disk.
@MainActor
final class QuoteModel: ObservableObject {
    @Published var cuisines: Set<SiteContent.Cuisine> = []
    @Published var name = ""
    @Published var email = ""
    @Published var phone = ""
    @Published var eventDate: Date? = nil
    @Published var foodTime = ""
    @Published var address = ""
    @Published var postcode = ""
    @Published var eventType: String? = nil
    @Published var meals = ""
    @Published var paying: String? = nil
    @Published var power: String? = nil
    @Published var privateLand: String? = nil
    @Published var entrance: String? = nil
    @Published var info = ""

    @Published var errors: [String: String] = [:]
    @Published private(set) var submitting = false
    @Published var banner: BannerState? = nil
    @Published var cuisineLimitHit = 0
    @Published var scrollTarget: String? = nil
    @Published var success: (id: String, firstName: String)? = nil

    private var submissionId = UUID().uuidString.lowercased()
    private var startedAt: Date? = nil

    func touch() { if startedAt == nil { startedAt = Date() } }

    func prefill(cuisine: SiteContent.Cuisine? = nil, eventType: String? = nil) {
        touch()
        if let cuisine, !cuisines.contains(cuisine) {
            if cuisines.count < SiteContent.maxCuisines { cuisines.insert(cuisine) } else { cuisineLimitHit += 1 }
        }
        if let eventType, SiteContent.eventTypes.contains(eventType) { self.eventType = eventType }
        errors["cuisines"] = nil
    }

    func fillFromProfile(_ p: Profile?) {
        guard let p else { return }
        if name.trimmingCharacters(in: .whitespaces).isEmpty { name = p.name }
        if email.trimmingCharacters(in: .whitespaces).isEmpty { email = p.email }
        if phone.trimmingCharacters(in: .whitespaces).isEmpty { phone = p.phone }
    }

    func toggle(_ c: SiteContent.Cuisine) {
        touch()
        if cuisines.contains(c) { cuisines.remove(c) }
        else if cuisines.count >= SiteContent.maxCuisines { cuisineLimitHit += 1; return }
        else { cuisines.insert(c) }
        errors["cuisines"] = nil
    }

    func validate(today: Date = Date()) -> [String: String] {
        var e: [String: String] = [:]
        e["cuisines"] = Validation.cuisines(cuisines)
        e["name"] = Validation.name(name)
        e["email"] = Validation.email(email)
        e["phone"] = Validation.phone(phone)
        e["event_date"] = Validation.eventDate(eventDate, today: today)
        e["postcode"] = Validation.postcode(postcode)
        e["event_type"] = Validation.required(eventType, "Please choose the type of event.")
        e["meals"] = Validation.meals(meals)
        e["paying"] = Validation.required(paying, "Please tell us who will be paying for food.")
        e["power"] = Validation.required(power, "Please answer this question.")
        e["private_land"] = Validation.required(privateLand, "Please answer this question.")
        e["entrance"] = Validation.required(entrance, "Please answer this question.")
        e["additional_info"] = Validation.links(info)
        return e.compactMapValues { $0 }
    }

    static let isoDate: DateFormatter = {
        let f = DateFormatter(); f.calendar = Calendar(identifier: .gregorian); f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"; return f
    }()
    static let displayDate: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_AU"); f.dateFormat = "EEE d MMM yyyy"; return f
    }()

    func payload() -> [String: Any] {
        [
            "submission_id": submissionId,
            "elapsed_seconds": startedAt.map { Int(Date().timeIntervalSince($0)) } ?? 0,
            "website": "",
            "name": name.trimmingCharacters(in: .whitespaces),
            "email": email.trimmingCharacters(in: .whitespaces),
            "phone": phone.trimmingCharacters(in: .whitespaces),
            "event_date": eventDate.map { Self.isoDate.string(from: $0) } ?? "",
            "food_time": foodTime.trimmingCharacters(in: .whitespaces),
            "event_address": address.trimmingCharacters(in: .whitespaces),
            "postcode": postcode.trimmingCharacters(in: .whitespaces),
            "event_type": eventType ?? "",
            "meals": Int(meals.trimmingCharacters(in: .whitespaces)) ?? 0,
            "paying": paying ?? "",
            "power": power ?? "",
            "private_land": privateLand ?? "",
            "entrance": entrance ?? "",
            "cuisines": SiteContent.Cuisine.allCases.filter { cuisines.contains($0) }.map(\.label),
            "additional_info": info.trimmingCharacters(in: .whitespacesAndNewlines),
        ]
    }

    func submit() async {
        guard !submitting else { return }
        banner = nil
        let found = validate()
        errors = found
        if !found.isEmpty {
            scrollTarget = QuoteField.allCases.map(\.rawValue).first { found[$0] != nil }
            banner = BannerState(message: found.count == 1 ? "Please check the highlighted field." : "Please check the \(found.count) highlighted fields.", offline: false)
            return
        }
        submitting = true
        defer { submitting = false }
        switch await API.shared.submit("quote.php", body: payload()) {
        case .success(let id):
            let first = name.trimmingCharacters(in: .whitespaces).components(separatedBy: " ").first ?? ""
            reset()
            success = (id, first)
        case .failure(let e):
            if case .invalid(let m, let fields) = e {
                errors.merge(fields) { $1 }
                scrollTarget = QuoteField.allCases.map(\.rawValue).first { fields[$0] != nil }
                banner = BannerState(message: m, offline: false)
            } else {
                banner = BannerState(message: e.message, offline: e.offline)
            }
        }
    }

    func clearForm() { reset() }

    private func reset() {
        cuisines = []; name = ""; email = ""; phone = ""; eventDate = nil; foodTime = ""; address = ""; postcode = ""
        eventType = nil; meals = ""; paying = nil; power = nil; privateLand = nil; entrance = nil; info = ""
        errors = [:]; banner = nil
        submissionId = UUID().uuidString.lowercased(); startedAt = nil
    }
}

// MARK: - Contact form

@MainActor
final class ContactModel: ObservableObject {
    @Published var name = ""
    @Published var email = ""
    @Published var topic = SiteContent.contactTopics[0]
    @Published var message = ""
    @Published var errors: [String: String] = [:]
    @Published private(set) var submitting = false
    @Published var banner: BannerState? = nil
    @Published var sentId: String? = nil
    private var submissionId = UUID().uuidString.lowercased()
    private var startedAt: Date? = nil

    func touch() { if startedAt == nil { startedAt = Date() } }

    func fillFromProfile(_ p: Profile?) {
        guard let p else { return }
        if name.isEmpty { name = p.name }
        if email.isEmpty { email = p.email }
    }

    func clearForm() {
        name = ""; email = ""; message = ""; topic = SiteContent.contactTopics[0]; errors = [:]; banner = nil; sentId = nil
        submissionId = UUID().uuidString.lowercased(); startedAt = nil
    }

    func submit() async {
        guard !submitting else { return }
        banner = nil
        var e: [String: String] = [:]
        e["name"] = Validation.name(name)
        e["email"] = Validation.email(email)
        e["message"] = Validation.message(message) ?? Validation.links(message)
        errors = e.compactMapValues { $0 }
        if !errors.isEmpty { banner = BannerState(message: "Please check the highlighted fields.", offline: false); return }
        submitting = true
        defer { submitting = false }
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
        let device = topic == "App support" ? String("\(UIDevice.current.model) (iOS \(UIDevice.current.systemVersion))".prefix(80)) : ""
        let body: [String: Any] = [
            "submission_id": submissionId,
            "elapsed_seconds": startedAt.map { Int(Date().timeIntervalSince($0)) } ?? 0,
            "website": "",
            "name": name.trimmingCharacters(in: .whitespaces),
            "email": email.trimmingCharacters(in: .whitespaces),
            "topic": topic,
            "message": message.trimmingCharacters(in: .whitespacesAndNewlines),
            "app_version": "iOS \(version)",
            "device": device,
        ]
        switch await API.shared.submit("contact.php", body: body) {
        case .success(let id):
            sentId = id
            name = ""; email = ""; message = ""; topic = SiteContent.contactTopics[0]
            submissionId = UUID().uuidString.lowercased(); startedAt = nil
        case .failure(let err):
            if case .invalid(let m, let f) = err { errors.merge(f) { $1 }; banner = BannerState(message: m, offline: false) }
            else { banner = BannerState(message: err.message, offline: err.offline) }
        }
    }
}

// MARK: - Website content

struct FeaturedContent { let headline: String?; let blocks: [HtmlBlock]; let image: URL?; let cuisine: SiteContent.Cuisine? }
struct LegalContent { let blocks: [HtmlBlock]; let updated: String; let offlineCopy: Bool }

@MainActor
final class ContentModel: ObservableObject {
    @Published var featured: Loadable<FeaturedContent?> = .loading
    @Published var legal: [SiteContent.LegalPage: Loadable<LegalContent>] = [:]
    @Published var contactEmail: String? = nil

    func start() {
        Task { await loadFeatured() }
        Task { contactEmail = await API.shared.contactEmail() }
    }

    func refreshContactEmail() { if contactEmail == nil { Task { contactEmail = await API.shared.contactEmail() } } }

    func loadFeatured() async {
        featured = .loading
        switch await API.shared.page("featuring") {
        case .success(let p): featured = .ready(Self.parseFeatured(p.html))
        case .failure(let e): featured = .error(e.message, offline: e.offline)
        }
    }

    static func parseFeatured(_ html: String) -> FeaturedContent? {
        let blocks = HtmlParser.parse(html)
        let text = HtmlParser.plainText(html)
        let images = blocks.compactMap { if case .image(let u) = $0 { return u }; return nil }
        if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && images.isEmpty { return nil }
        let headline = blocks.lazy.compactMap { if case .heading(let t, _, _) = $0 { return t }; return nil }.first
        let rest = blocks.filter {
            if case .image = $0 { return false }
            if case .heading(let t, _, _) = $0, t == headline { return false }
            return true
        }
        return FeaturedContent(headline: headline, blocks: rest, image: images.first, cuisine: SiteContent.Cuisine.find(in: text))
    }

    func loadLegal(_ page: SiteContent.LegalPage, force: Bool = false) async {
        if !force, case .ready(let c) = legal[page], !c.offlineCopy { return }
        legal[page] = .loading
        switch await API.shared.page(page.rawValue) {
        case .success(let p): legal[page] = .ready(LegalContent(blocks: HtmlParser.parse(p.html), updated: p.modified, offlineCopy: false))
        case .failure(let e): legal[page] = Self.bundled(page) ?? .error(e.message, offline: e.offline)
        }
    }

    /// Copy of the page bundled with the app (taken from the website), shown only when offline.
    static func bundled(_ page: SiteContent.LegalPage) -> Loadable<LegalContent>? {
        guard let url = Bundle.main.url(forResource: page.rawValue, withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let html = obj["html"] as? String else { return nil }
        return .ready(LegalContent(blocks: HtmlParser.parse(html), updated: obj["modified"] as? String ?? "", offlineCopy: true))
    }
}

// MARK: - Accounts

@MainActor
final class AuthModel: ObservableObject {
    enum Mode { case login, register }
    @Published var mode: Mode = .login
    @Published var name = ""
    @Published var email = ""
    @Published var phone = ""
    @Published var password = ""
    @Published var acceptedTerms = false
    @Published var errors: [String: String] = [:]
    @Published var banner: BannerState? = nil
    @Published private(set) var busy = false

    @Published var forgotEmail = ""
    @Published var forgotSent: String? = nil

    @Published var enquiries: Loadable<[MyEnquiry]> = .loading
    @Published var profileName = ""
    @Published var profilePhone = ""
    @Published var profileSaved = false
    @Published var deletePassword = ""

    func submit() async {
        guard !busy else { return }
        banner = nil
        var e: [String: String] = [:]
        if mode == .register {
            e["name"] = Validation.name(name)
            e["phone"] = Validation.phone(phone)
            e["password"] = Validation.password(password)
            if !acceptedTerms { e["terms"] = "Please accept the Terms and Privacy Policy." }
        } else if password.isEmpty {
            e["password"] = "Please enter your password."
        }
        e["email"] = Validation.email(email)
        errors = e.compactMapValues { $0 }
        if !errors.isEmpty { banner = BannerState(message: "Please check the highlighted fields.", offline: false); return }
        busy = true
        defer { busy = false }
        let r = mode == .register
            ? await API.shared.register(name: name.trimmingCharacters(in: .whitespaces), email: email.trimmingCharacters(in: .whitespaces), phone: phone.trimmingCharacters(in: .whitespaces), password: password)
            : await API.shared.login(email: email.trimmingCharacters(in: .whitespaces), password: password)
        switch r {
        case .success:
            password = ""; name = ""; phone = ""; acceptedTerms = false; errors = [:]
        case .failure(let err):
            if case .invalid(let m, let f) = err { errors.merge(f) { $1 }; banner = BannerState(message: m, offline: false) }
            else { banner = BannerState(message: err.message, offline: err.offline) }
            password = ""
        }
    }

    func sendReset() async {
        guard !busy else { return }
        banner = nil
        if let e = Validation.email(forgotEmail) { errors = ["email": e]; return }
        errors = [:]
        busy = true
        defer { busy = false }
        switch await API.shared.forgotPassword(email: forgotEmail.trimmingCharacters(in: .whitespaces)) {
        case .success(let m): forgotSent = m
        case .failure(let e): banner = BannerState(message: e.message, offline: e.offline)
        }
    }

    func loadAccount(_ profile: Profile?) async {
        if let profile { profileName = profile.name; profilePhone = profile.phone }
        await API.shared.refreshProfile()
        if let p = SessionStore.shared.session?.profile { profileName = p.name; profilePhone = p.phone }
        await loadEnquiries()
    }

    func loadEnquiries() async {
        if case .ready = enquiries {} else { enquiries = .loading }
        switch await API.shared.myEnquiries() {
        case .success(let list): enquiries = .ready(list)
        case .failure(let e): enquiries = .error(e.message, offline: e.offline)
        }
    }

    func saveProfile() async {
        guard !busy else { return }
        var e: [String: String] = [:]
        e["name"] = Validation.name(profileName)
        e["phone"] = Validation.phone(profilePhone)
        errors = e.compactMapValues { $0 }
        profileSaved = false
        guard errors.isEmpty else { return }
        busy = true
        defer { busy = false }
        switch await API.shared.updateProfile(name: profileName.trimmingCharacters(in: .whitespaces), phone: profilePhone.trimmingCharacters(in: .whitespaces)) {
        case .success: profileSaved = true; banner = nil
        case .failure(let err):
            if case .invalid(let m, let f) = err { errors.merge(f) { $1 }; banner = BannerState(message: m, offline: false) }
            else { banner = BannerState(message: err.message, offline: err.offline) }
        }
    }

    func deleteAccount() async -> Bool {
        guard !busy else { return false }
        if deletePassword.isEmpty { errors = ["delete_password": "Please enter your password."]; return false }
        busy = true
        defer { busy = false }
        switch await API.shared.deleteAccount(password: deletePassword) {
        case .success: deletePassword = ""; reset(); return true
        case .failure(let e):
            errors = ["delete_password": e.fields["password"] ?? e.message]
            return false
        }
    }

    func reset() {
        name = ""; email = ""; phone = ""; password = ""; acceptedTerms = false; errors = [:]; banner = nil
        enquiries = .loading; profileName = ""; profilePhone = ""; profileSaved = false; deletePassword = ""; forgotSent = nil
    }
}
