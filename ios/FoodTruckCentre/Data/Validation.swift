import Foundation

/// Client-side validation, same rules as the Android app and the server (which checks everything again).
enum Validation {
    private static func matches(_ s: String, _ pattern: String) -> Bool {
        s.range(of: pattern, options: .regularExpression) != nil
    }

    static func name(_ v: String) -> String? {
        v.trimmingCharacters(in: .whitespaces).count < 2 ? "Please enter your full name." : nil
    }

    static func email(_ v: String) -> String? {
        let s = v.trimmingCharacters(in: .whitespaces)
        if s.isEmpty { return "Please enter your email address." }
        if s.count > 254 || !matches(s, "^[A-Za-z0-9._%+'-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") || s.contains("..") {
            return "Please enter a valid email address."
        }
        return nil
    }

    static func phone(_ v: String) -> String? {
        let s = v.trimmingCharacters(in: .whitespaces)
        if s.isEmpty { return "Please enter your phone number." }
        let digits = s.filter(\.isNumber).count
        return (!matches(s, "^[0-9+()\\-\\s]{8,20}$") || digits < 8 || digits > 15) ? "Please enter a valid phone number." : nil
    }

    static func eventDate(_ d: Date?, today: Date = Date()) -> String? {
        guard let d else { return "Please choose the event date." }
        let cal = Calendar.current
        let start = cal.startOfDay(for: today)
        if cal.startOfDay(for: d) < start { return "The event date can't be in the past." }
        if let max = cal.date(byAdding: .year, value: 3, to: start), d > max { return "Please choose a date within the next 3 years." }
        return nil
    }

    static func postcode(_ v: String) -> String? {
        matches(v.trimmingCharacters(in: .whitespaces), "^\\d{4}$") ? nil : "Please enter a valid 4-digit Australian postcode."
    }

    static func meals(_ v: String) -> String? {
        let t = v.trimmingCharacters(in: .whitespaces)
        if t.isEmpty { return "Please enter the number of meals." }
        guard let n = Int(t) else { return "Please enter a whole number." }
        if n < SiteContent.minMeals { return "The minimum number of meals is \(SiteContent.minMeals)." }
        if n > 100_000 { return "Please enter a realistic number of meals." }
        return nil
    }

    static func required(_ v: String?, _ message: String = "Please choose an option.") -> String? {
        (v ?? "").isEmpty ? message : nil
    }

    static func cuisines(_ c: Set<SiteContent.Cuisine>) -> String? {
        c.count > SiteContent.maxCuisines ? "Please select a maximum of three cuisines." : nil
    }

    static func message(_ v: String) -> String? {
        v.trimmingCharacters(in: .whitespacesAndNewlines).count < 5 ? "Please write a short message." : nil
    }

    static func links(_ v: String) -> String? {
        let count = (try? NSRegularExpression(pattern: "(https?://|www\\.)", options: .caseInsensitive))?
            .numberOfMatches(in: v, range: NSRange(v.startIndex..., in: v)) ?? 0
        return count > 3 ? "Please remove some of the links from your message." : nil
    }

    /// 8+ characters with at least one letter and one number (same as the server).
    static func password(_ v: String) -> String? {
        if v.count < 8 { return "Use at least 8 characters." }
        if v.count > 200 { return "That password is too long." }
        if !v.contains(where: \.isLetter) || !v.contains(where: \.isNumber) { return "Use at least one letter and one number." }
        return nil
    }
}
