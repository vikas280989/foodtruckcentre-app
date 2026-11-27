import XCTest

/// Full customer journey against a local test copy of the WordPress backend (started by CI on 127.0.0.1:8800):
/// create account → request a quote → see it in My enquiries → log out → log in again. Takes screenshots on the way.
final class EndToEndUITests: XCTestCase {
    private var app: XCUIApplication!
    private let email = "ios.e2e.\(Int(Date().timeIntervalSince1970))@example.test"
    private let password = "Truck2026ios"

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-ui-testing", "-ui-testing-reset"]
        app.launchEnvironment["FTC_API_BASE"] = ProcessInfo.processInfo.environment["FTC_API_BASE"] ?? "http://127.0.0.1:8800/app-api/v1/"
        app.launch()
    }

    func testCustomerJourney() throws {
        // Login is required before anything else.
        XCTAssertTrue(app.staticTexts["authTitle"].waitForExistence(timeout: 20), "login screen shown first")
        shot("01-login")

        app.buttons["Create account"].firstMatch.tap()
        type("Full name", "iOS Tester")
        type("Email", email)
        type("Phone number", "0412345678")
        type("Password", password, secure: true)
        app.buttons["acceptTerms"].tap()
        shot("02-register")
        dismissKeyboard(); app.buttons["createAccount"].tap()

        // Home
        XCTAssertTrue(app.buttons["tab.home"].waitForExistence(timeout: 30), "account created and home shown")
        XCTAssertTrue(app.staticTexts["Hi iOS!"].waitForExistence(timeout: 5), "greets the customer by name")
        shot("03-home")

        app.buttons["tab.services"].tap(); shot("04-services")
        app.buttons["tab.featuring"].tap(); sleep(3); shot("05-featuring")
        app.buttons["tab.contact"].tap(); shot("06-contact")

        // Quote: empty submit shows errors
        app.buttons["tab.quote"].tap()
        XCTAssertTrue(app.buttons["submitQuote"].waitForExistence(timeout: 5))
        XCTAssertEqual(field("Your Email Address").value as? String, email, "email pre-filled from the account")
        app.buttons["submitQuote"].tap()
        XCTAssertTrue(app.staticTexts["Error: Please choose the event date."].waitForExistence(timeout: 5), "validation errors shown")
        shot("07-quote-errors")

        app.buttons["Pizza / Italian"].tap()
        app.buttons["Desserts / Coffee / Sweets"].tap()
        app.buttons["eventDate"].tap()
        XCTAssertTrue(app.buttons["dateDone"].waitForExistence(timeout: 5))
        app.buttons["dateDone"].tap()
        type("What time is food to be served", "12:00pm")
        type("Address of the event", "1 Test Street, Sydney NSW")
        type("Postcode", "2000")
        app.buttons["What is the event: Birthday"].tap()
        type("Number of meals", "80")
        app.buttons["Who will be paying for food: Organiser"].tap()
        app.buttons["Is there power available at the address: Yes"].tap()
        app.buttons["Will the truck be parked on private land: No"].tap()
        app.buttons["Is the entrance wider than 3m and at least 5m in length without overhanging obstacles: Yes"].tap()
        sleep(4) // the server rejects forms completed implausibly fast
        shot("08-quote-filled")
        app.buttons["submitQuote"].tap()

        let ref = app.staticTexts["enquiryId"]
        XCTAssertTrue(ref.waitForExistence(timeout: 30), "quote sent and reference shown")
        let enquiryId = ref.label
        XCTAssertTrue(enquiryId.hasPrefix("FTC-"), enquiryId)
        shot("09-success")
        let out = XCTAttachment(string: enquiryId); out.name = "enquiry-id"; out.lifetime = .keepAlways; add(out)

        // My enquiries
        app.buttons["View my enquiries"].tap()
        XCTAssertTrue(app.descendants(matching: .any)["enquiry.\(enquiryId)"].waitForExistence(timeout: 20), "enquiry listed in My account")
        shot("10-account")

        // Log out and back in
        app.buttons["logOut"].tap()
        app.sheets.buttons["Log out"].firstMatch.tap()
        XCTAssertTrue(app.staticTexts["authTitle"].waitForExistence(timeout: 15), "back to login after logout")
        type("Email", email)
        type("Password", "wrong-password1", secure: true)
        dismissKeyboard(); app.buttons["logIn"].tap()
        XCTAssertTrue(app.buttons["tab.home"].waitForExistence(timeout: 5) == false, "wrong password refused")
        let refused = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS[c] 'incorrect'")).firstMatch
        if !refused.waitForExistence(timeout: 15) { print("UI TREE:\n" + app.debugDescription); shot("fail-login") }
        XCTAssertTrue(refused.exists, "error shown for the wrong password")
        type("Password", password, secure: true) // the app clears the password after a failed attempt
        dismissKeyboard(); app.buttons["logIn"].tap()
        XCTAssertTrue(app.buttons["tab.home"].waitForExistence(timeout: 30), "logged in again")
        tapVisible("accountButton")
        XCTAssertTrue(app.descendants(matching: .any)["enquiry.\(enquiryId)"].waitForExistence(timeout: 20), "enquiry still there after login")
    }

    func testLegalPagesOpenFromLogin() {
        XCTAssertTrue(app.staticTexts["authTitle"].waitForExistence(timeout: 20))
        app.buttons["Create account"].firstMatch.tap()
        app.buttons["Read Privacy Policy"].tap()
        XCTAssertTrue(app.staticTexts["Privacy Policy"].firstMatch.waitForExistence(timeout: 20))
        shot("11-privacy")
    }

    // MARK: helpers

    private func field(_ id: String) -> XCUIElement {
        let tf = app.textFields[id]
        if tf.exists { return tf }
        let sf = app.secureTextFields[id]
        if sf.exists { return sf }
        return app.textViews[id]
    }

    private func type(_ id: String, _ text: String, secure: Bool = false) {
        dismissKeyboard()
        var el = secure ? app.secureTextFields[id] : field(id)
        if !el.waitForExistence(timeout: 5) { el = field(id) }
        XCTAssertTrue(el.waitForExistence(timeout: 5), "field \(id)")
        el.tap()
        if !secure, let current = el.value as? String, !current.isEmpty, current != el.placeholderValue {
            el.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: current.count))
        }
        el.typeText(text)
        dismissKeyboard()
    }

    private func dismissKeyboard() {
        let keyboard = app.keyboards.firstMatch
        guard keyboard.exists else { return }
        let done = app.buttons.matching(identifier: "keyboardDone").firstMatch
        if done.waitForExistence(timeout: 3) && done.isHittable { done.tap() }
        let gone = NSPredicate(format: "exists == false")
        if XCTWaiter().wait(for: [expectation(for: gone, evaluatedWith: keyboard)], timeout: 4) != .completed {
            let ret = keyboard.buttons["Return"]
            if ret.exists { ret.tap() }
            _ = XCTWaiter().wait(for: [expectation(for: gone, evaluatedWith: keyboard)], timeout: 4)
        }
    }

    /// Taps the on-screen element with this identifier (screen transitions can leave an off-screen copy for a moment).
    private func tapVisible(_ id: String) {
        let deadline = Date().addingTimeInterval(10)
        while Date() < deadline {
            let all = app.buttons.matching(identifier: id).allElementsBoundByIndex
            if let el = all.first(where: { $0.exists && $0.isHittable && app.frame.contains(CGPoint(x: $0.frame.midX, y: $0.frame.midY)) }) {
                el.tap(); return
            }
            usleep(300_000)
        }
        XCTFail("no visible \(id)")
    }

    private func shot(_ name: String) {
        let a = XCTAttachment(screenshot: app.screenshot())
        a.name = name
        a.lifetime = .keepAlways
        add(a)
    }
}
