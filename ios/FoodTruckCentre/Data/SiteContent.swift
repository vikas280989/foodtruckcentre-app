import Foundation

/// Content taken from https://foodtruckcentre.com.au/ (audited 3 Nov 2026) – identical to the Android app.
/// Featured truck and legal pages are loaded live from the website.
enum SiteContent {
    static let heroTitle = "Book Australia’s\nBest Food Trucks"
    static let intro = "FoodTruckCentre.com.au helps you create amazing events with Australia’s best food trucks by providing delicious and flexible catering options for corporate parties, weddings and other events."
    static let intro2 = "From tacos, souvlakis, wood-fired pizzas to Asian food and more, food trucks make every event deliciously memorable."
    static let intro3 = "Share your event details with us to receive quotes and menus from a number of sensational food trucks."
    static let formIntro = "Complete the form below to receive quotes from Australia's best food trucks"
    static let contactIntro = "If you have comments or questions please let us know"
    static let instagramURL = URL(string: "https://www.instagram.com/foodtruckcentre")!
    static let websiteURL = URL(string: "https://foodtruckcentre.com.au/")!
    static let copyright = "©2026 Food Truck Centre All Rights Reserved."

    struct Highlight: Identifiable { let id: Int; let title: String; let body: String; let symbol: String }
    static let highlights: [Highlight] = [
        .init(id: 0, title: "Wide selection of menus", body: "Choose from a diverse range of menu options crafted to suit every taste, occasion, and dietary preference.", symbol: "menucard.fill"),
        .init(id: 1, title: "500+ Events booked", body: "Trusted by hundreds of clients with a proven track record of successfully managing and delivering memorable events.", symbol: "calendar.badge.checkmark"),
        .init(id: 2, title: "6+ Years of experience", body: "Over six years of industry experience providing reliable service, professional expertise, and consistent quality.", symbol: "rosette"),
    ]

    enum Cuisine: String, CaseIterable, Identifiable {
        case burgers = "Burgers / American", mexican = "Mexican / Latin", asian = "Asian Fusion", vietnamese = "Vietnamese",
             mediterranean = "Mediterranean / Middle Eastern", bbq = "BBQ / Smoked Meats", pizza = "Pizza / Italian",
             seafood = "Seafood", vegan = "Vegan / Healthy", desserts = "Desserts / Coffee / Sweets"
        var id: String { rawValue }
        var label: String { rawValue }
        var description: String {
            switch self {
            case .burgers: return "Burgers, loaded fries"
            case .mexican: return "Tacos, burritos, quesadillas"
            case .asian: return "Bao buns, dumplings, Korean chicken"
            case .vietnamese: return "Banh mi, rice bowls"
            case .mediterranean: return "Souvlaki, falafel, shawarma, kebabs"
            case .bbq: return "Brisket, ribs, pulled pork"
            case .pizza: return "Wood-fired pizza, pizza slices"
            case .seafood: return "Fish tacos, calamari, lobster rolls"
            case .vegan: return "Vegan burgers, wraps"
            case .desserts: return "Ice cream, churros, waffles, espresso"
            }
        }
        var symbol: String {
            switch self {
            case .burgers: return "takeoutbag.and.cup.and.straw.fill"
            case .mexican: return "flame.fill"
            case .asian: return "fork.knife"
            case .vietnamese: return "leaf.fill"
            case .mediterranean: return "sun.max.fill"
            case .bbq: return "flame.circle.fill"
            case .pizza: return "circle.grid.cross.fill"
            case .seafood: return "fish.fill"
            case .vegan: return "carrot.fill"
            case .desserts: return "cup.and.saucer.fill"
            }
        }
        /// Finds a cuisine mentioned in text, e.g. the Featuring page ("Pizza / Italian").
        static func find(in text: String) -> Cuisine? {
            let t = text.replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression).lowercased()
            return allCases.first { t.contains($0.label.lowercased()) || t.contains($0.label.replacingOccurrences(of: " / ", with: "/").lowercased()) }
        }
    }

    struct Step: Identifiable { let id: Int; let title: String; let body: String }
    static let process: [Step] = [
        .init(id: 1, title: "Choose up to three cuisines", body: "From Burgers / American to Desserts / Coffee / Sweets – tick 3 max."),
        .init(id: 2, title: "Share your event details", body: "Your date, serving time, address and postcode, type of event, number of meals (minimum 50) and site details like power and truck access."),
        .init(id: 3, title: "Receive quotes and menus", body: "Your enquiry is shared so you can receive quotes and menus from a number of sensational food trucks."),
        .init(id: 4, title: "Confirm directly with the vendor", body: "All bookings are subject to vendor acceptance. Once you accept a vendor's quote, the booking becomes an agreement between you and that vendor."),
    ]
    static let bookingNote = "Sending an enquiry is not a confirmed booking. Pricing, deposits and payment are agreed directly with the food truck vendor – no payment is taken in this app."

    static let maxCuisines = 3
    static let minMeals = 50
    static let eventTypes = ["Office Celebration", "Birthday", "Other"]
    static let payers = ["Organiser", "Guests"]
    static let yesNo = ["Yes", "No"]
    static let contactTopics = ["General enquiry", "Corporate catering", "Feedback", "App support"]

    struct Service: Identifiable { let id: String; let title: String; let description: String; let image: String; let presetEventType: String? }
    static let services: [Service] = [
        .init(id: "corporate", title: "Corporate Catering", description: "Professional catering solutions tailored for business meetings, office lunches, and corporate functions.", image: "svc_corporate", presetEventType: "Office Celebration"),
        .init(id: "conferences", title: "Conferences & Conventions", description: "Reliable food services designed for conferences, seminars, and large professional gatherings.", image: "svc_conferences", presetEventType: nil),
        .init(id: "university", title: "University & School Events", description: "Fresh and quality catering for educational institutions, campus events, and academic celebrations.", image: "svc_university", presetEventType: nil),
        .init(id: "production", title: "Production Catering", description: "Efficient catering support for film crews, media teams, and production environments.", image: "svc_production", presetEventType: nil),
        .init(id: "private", title: "Private Parties", description: "Delicious menus crafted for birthdays, anniversaries, and personal celebrations.", image: "svc_private", presetEventType: nil),
        .init(id: "largescale", title: "Large-Scale Events", description: "Expert catering management for weddings, festivals, and high-capacity events.", image: "svc_largescale", presetEventType: nil),
    ]

    enum LegalPage: String, CaseIterable, Hashable {
        case privacy = "privacy-policy", terms = "terms-and-conditions", vendor = "vendor-terms"
        var title: String {
            switch self { case .privacy: return "Privacy Policy"; case .terms: return "Terms and Conditions"; case .vendor: return "Vendor Terms" }
        }
    }
}
