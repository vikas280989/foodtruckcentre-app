import XCTest
@testable import FoodTruckCentre

final class ValidationTests: XCTestCase {
    func testEmail() {
        XCTAssertNil(Validation.email("jo.smith+events@example.com.au"))
        XCTAssertNotNil(Validation.email(""))
        XCTAssertNotNil(Validation.email("jo@"))
        XCTAssertNotNil(Validation.email("jo..x@example.com"))
        XCTAssertNotNil(Validation.email("not an email"))
    }

    func testPhone() {
        XCTAssertNil(Validation.phone("0412 345 678"))
        XCTAssertNil(Validation.phone("+61 (2) 9876 5432"))
        XCTAssertNotNil(Validation.phone("1234"))
        XCTAssertNotNil(Validation.phone("call me"))
        XCTAssertNotNil(Validation.phone("1234567890123456"))
    }

    func testPostcodeAndMeals() {
        XCTAssertNil(Validation.postcode("2000"))
        XCTAssertNotNil(Validation.postcode("200"))
        XCTAssertNotNil(Validation.postcode("20000"))
        XCTAssertNil(Validation.meals("50"))
        XCTAssertNotNil(Validation.meals("49"))
        XCTAssertNotNil(Validation.meals("12.5"))
        XCTAssertNotNil(Validation.meals("100001"))
    }

    func testDates() {
        let today = Date()
        XCTAssertNil(Validation.eventDate(today, today: today))
        XCTAssertNotNil(Validation.eventDate(nil, today: today))
        XCTAssertNotNil(Validation.eventDate(Calendar.current.date(byAdding: .day, value: -1, to: today), today: today))
        XCTAssertNotNil(Validation.eventDate(Calendar.current.date(byAdding: .year, value: 4, to: today), today: today))
    }

    func testPasswordAndLinks() {
        XCTAssertNil(Validation.password("truck2026"))
        XCTAssertNotNil(Validation.password("short1"))
        XCTAssertNotNil(Validation.password("onlyletters"))
        XCTAssertNotNil(Validation.password("12345678"))
        XCTAssertNil(Validation.links("see https://a.com and www.b.com"))
        XCTAssertNotNil(Validation.links("http://a http://b http://c http://d"))
    }

    func testCuisineLimit() {
        let three: Set<SiteContent.Cuisine> = [.burgers, .pizza, .vegan]
        XCTAssertNil(Validation.cuisines(three))
        XCTAssertNotNil(Validation.cuisines(three.union([.seafood])))
    }
}

final class SiteContentTests: XCTestCase {
    func testCuisineLabelsMatchServer() {
        XCTAssertEqual(SiteContent.Cuisine.allCases.map(\.label), [
            "Burgers / American", "Mexican / Latin", "Asian Fusion", "Vietnamese", "Mediterranean / Middle Eastern",
            "BBQ / Smoked Meats", "Pizza / Italian", "Seafood", "Vegan / Healthy", "Desserts / Coffee / Sweets",
        ])
        XCTAssertEqual(SiteContent.eventTypes, ["Office Celebration", "Birthday", "Other"])
        XCTAssertEqual(SiteContent.contactTopics, ["General enquiry", "Corporate catering", "Feedback", "App support"])
    }

    func testFindCuisine() {
        XCTAssertEqual(SiteContent.Cuisine.find(in: "This month: Pizza /  Italian from Napoli Wheels"), .pizza)
        XCTAssertEqual(SiteContent.Cuisine.find(in: "BBQ/Smoked Meats"), .bbq)
        XCTAssertNil(SiteContent.Cuisine.find(in: "Nothing here"))
    }
}

final class HtmlParserTests: XCTestCase {
    func testNumberedSectionHeadings() {
        let blocks = HtmlParser.parse("<p><strong>Privacy Policy</strong></p><ol><li><strong> Overview</strong></li></ol><p>FTC is committed &amp; more.</p><ul><li>Name</li></ul>")
        XCTAssertEqual(blocks.count, 4)
        XCTAssertEqual(blocks[0], .heading(text: "Privacy Policy", number: nil, level: 3))
        XCTAssertEqual(blocks[1], .heading(text: "Overview", number: 1, level: 2))
        XCTAssertEqual(blocks[2], .paragraph([HtmlSpan(text: "FTC is committed & more.")]))
        XCTAssertEqual(blocks[3], .bullet([HtmlSpan(text: "Name")]))
    }

    func testScriptsAndUnsafeImagesDropped() {
        let blocks = HtmlParser.parse("<script>alert(1)</script><p>Hi <a href=\"https://x.test/#form\">book</a></p><img src=\"http://insecure/x.jpg\"><img src=\"https://ok/x.jpg\">")
        XCTAssertEqual(blocks.count, 2)
        guard case .paragraph(let spans) = blocks[0] else { return XCTFail() }
        XCTAssertEqual(spans.last?.link, "https://x.test/#form")
        XCTAssertEqual(blocks[1], .image(URL(string: "https://ok/x.jpg")!))
    }

    func testLineBreakOnlyParagraphsDropped() {
        let blocks = HtmlParser.parse("<p><br><br></p><p>&nbsp;<br>\n</p><p>Hi<br>there<br><br><br><br>again<br></p><p></p>")
        XCTAssertEqual(blocks, [.paragraph([HtmlSpan(text: "Hi\nthere\n\nagain")])])
    }

    func testEntities() {
        XCTAssertEqual(HtmlParser.decode("Australia&#8217;s &#x2013; &rsquo;"), "Australia’s – ’")
    }

    func testBundledLegalPagesParse() {
        for page in SiteContent.LegalPage.allCases {
            guard case .ready(let c)? = ContentModel.bundled(page) else { return XCTFail("missing \(page)") }
            XCTAssertTrue(c.offlineCopy)
            XCTAssertGreaterThan(c.blocks.count, 5, page.rawValue)
            XCTAssertTrue(c.blocks.contains { if case .heading(_, let n?, _) = $0 { return n >= 1 }; return false }, page.rawValue)
        }
    }

    func testQuoteLinks() {
        XCTAssertTrue(RichText.isQuoteLink(URL(string: "https://foodtruckcentre.com.au/#form")!))
        XCTAssertFalse(RichText.isQuoteLink(URL(string: "https://evil.example/#form")!))
        XCTAssertFalse(RichText.isQuoteLink(URL(string: "https://foodtruckcentre.com.au/about")!))
    }

    func testFeaturedParsing() {
        let f = ContentModel.parseFeatured("<h2>Napoli Wheels</h2><img src=\"https://img/x.jpg\"><p>Cuisine: Pizza / Italian</p>")
        XCTAssertEqual(f?.headline, "Napoli Wheels")
        XCTAssertEqual(f?.cuisine, .pizza)
        XCTAssertEqual(f?.image, URL(string: "https://img/x.jpg"))
        XCTAssertEqual(f?.blocks.count, 1)
        XCTAssertNil(ContentModel.parseFeatured("<p> </p>"))
    }
}

@MainActor
final class QuoteModelTests: XCTestCase {
    func testPayloadMatchesServerContract() {
        let m = QuoteModel()
        m.toggle(.pizza); m.toggle(.burgers)
        m.name = " Jo Smith "; m.email = "jo@example.com"; m.phone = "0412345678"
        m.eventDate = QuoteModel.isoDate.date(from: "2030-01-15"); m.postcode = "2000"
        m.eventType = "Birthday"; m.meals = "120"; m.paying = "Organiser"; m.power = "Yes"; m.privateLand = "No"; m.entrance = "Yes"
        XCTAssertTrue(m.validate(today: QuoteModel.isoDate.date(from: "2029-12-01")!).isEmpty)
        let p = m.payload()
        XCTAssertEqual(p["name"] as? String, "Jo Smith")
        XCTAssertEqual(p["event_date"] as? String, "2030-01-15")
        XCTAssertEqual(p["meals"] as? Int, 120)
        XCTAssertEqual(p["cuisines"] as? [String], ["Burgers / American", "Pizza / Italian"])
        XCTAssertEqual(p["website"] as? String, "")
        let id = p["submission_id"] as? String ?? ""
        XCTAssertNotNil(id.range(of: "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", options: .regularExpression))
        for key in ["food_time", "event_address", "private_land", "additional_info", "elapsed_seconds"] { XCTAssertNotNil(p[key], key) }
    }

    func testCuisineLimitAndMissingFields() {
        let m = QuoteModel()
        [.pizza, .burgers, .vegan, .seafood].forEach { m.toggle($0) }
        XCTAssertEqual(m.cuisines.count, 3)
        XCTAssertEqual(m.cuisineLimitHit, 1)
        let e = m.validate()
        for key in ["name", "email", "phone", "event_date", "postcode", "event_type", "meals", "paying", "power", "private_land", "entrance"] {
            XCTAssertNotNil(e[key], key)
        }
        XCTAssertNil(e["cuisines"])
    }

    func testProfilePrefillDoesNotOverwrite() {
        let m = QuoteModel()
        m.name = "Typed Name"
        m.fillFromProfile(Profile(name: "Account Name", email: "a@b.com", phone: "0400000000"))
        XCTAssertEqual(m.name, "Typed Name")
        XCTAssertEqual(m.email, "a@b.com")
        XCTAssertEqual(m.phone, "0400000000")
    }
}
