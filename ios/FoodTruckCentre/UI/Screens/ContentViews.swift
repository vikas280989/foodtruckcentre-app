import SwiftUI

// MARK: - Intro

struct IntroView: View {
    let onDone: () -> Void
    @State private var logoScale: CGFloat = 0.4
    @State private var truckOffset: CGFloat = -300
    @State private var textVisible = false

    var body: some View {
        ZStack {
            LinearGradient(colors: [.ftcCream, .white, .ftcCream], startPoint: .top, endPoint: .bottom).ignoresSafeArea()
            VStack(spacing: 18) {
                Image("logo_badge").resizable().scaledToFit().frame(width: 210, height: 210)
                    .scaleEffect(logoScale).offset(x: truckOffset)
                    .accessibilityLabel("Food Truck Centre")
                VStack(spacing: 4) {
                    Text("Food Truck Centre").font(FtcFont.poppins(26, .bold, relativeTo: .title)).foregroundStyle(Color.ftcNavy)
                    Text("Book Australia’s best food trucks").font(.ftcBody).foregroundStyle(Color.ftcMuted)
                }
                .opacity(textVisible ? 1 : 0)
                .offset(y: textVisible ? 0 : 20)
            }
        }
        .task {
            withAnimation(.spring(response: 0.7, dampingFraction: 0.55)) { logoScale = 1 }
            withAnimation(.easeOut(duration: 0.9)) { truckOffset = 0 }
            try? await Task.sleep(nanoseconds: 350_000_000)
            withAnimation(.easeOut(duration: 0.6)) { textVisible = true }
            try? await Task.sleep(nanoseconds: 1_100_000_000)
            onDone()
        }
    }
}

// MARK: - Services

struct ServicesView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var quote: QuoteModel
    var body: some View {
        VStack(spacing: 0) {
            ScreenHeader(title: "Our Services", subtitle: "Food truck catering for every occasion", onAccount: { router.push(.account) })
            ScrollView {
                VStack(spacing: 16) {
                    ForEach(SiteContent.services) { s in
                        VStack(alignment: .leading, spacing: 0) {
                            FillImage(name: s.image, height: 170)
                                .accessibilityHidden(true)
                            VStack(alignment: .leading, spacing: 8) {
                                Text(s.title).font(.ftcHeadline).foregroundStyle(Color.ftcNavy).accessibilityAddTraits(.isHeader)
                                Text(s.description).font(.ftcBody).foregroundStyle(Color.ftcMuted)
                                Button {
                                    quote.prefill(eventType: s.presetEventType); router.open(.quote)
                                } label: {
                                    Label("Get a quote", systemImage: "arrow.right").font(.ftcLabel).foregroundStyle(Color.ftcRed)
                                        .frame(minHeight: 44)
                                }
                                .accessibilityLabel("Get a quote for \(s.title)")
                            }
                            .padding(16)
                        }
                        .background(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(Color.ftcLine))
                        .shadow(color: Color.ftcNavy.opacity(0.06), radius: 12, y: 6)
                    }
                    Button("About Food Truck Centre") { router.push(.about) }.buttonStyle(SecondaryButtonStyle())
                }
                .padding(20)
            }
        }
        .background(Color.ftcCream)
    }
}

// MARK: - Featuring (live from the website)

struct FeaturingView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var quote: QuoteModel
    @EnvironmentObject var content: ContentModel

    var body: some View {
        VStack(spacing: 0) {
            ScreenHeader(title: "Featuring", subtitle: "Live from foodtruckcentre.com.au", onAccount: { router.push(.account) })
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    switch content.featured {
                    case .loading:
                        LoadingCard()
                    case .error(let msg, let offline):
                        ErrorCard(message: msg, offline: offline) { Task { await content.loadFeatured() } }
                    case .ready(.none):
                        Card {
                            Text("No truck is being featured right now").font(.ftcHeadline).foregroundStyle(Color.ftcNavy)
                            Text("Check back soon, or request quotes from a number of sensational food trucks.").font(.ftcBody).foregroundStyle(Color.ftcMuted)
                        }
                    case .ready(let f?):
                        if let img = f.image {
                            Color.ftcCreamDeep.frame(height: 230).frame(maxWidth: .infinity)
                            .overlay(AsyncImage(url: img) { phase in
                                if let image = phase.image { image.resizable().scaledToFill() } else { ProgressView() }
                            })
                            .clipped()
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .accessibilityLabel(f.headline ?? "Featured food truck")
                        }
                        if let h = f.headline {
                            Text(h).font(.ftcTitle).foregroundStyle(Color.ftcNavy).accessibilityAddTraits(.isHeader).accessibilityIdentifier("featuredHeadline")
                        }
                        if let c = f.cuisine {
                            Label(c.label, systemImage: c.symbol).font(.ftcLabel).foregroundStyle(Color.ftcNavyDeep)
                                .padding(.horizontal, 12).padding(.vertical, 6).background(Capsule().fill(Color.ftcMustardSoft))
                        }
                        if !f.blocks.isEmpty { Card { RichText(blocks: f.blocks, onQuoteLink: { openQuote(f) }) } }
                        PrimaryButton(title: "Request a Quote", systemImage: "doc.text.fill") { openQuote(f) }
                    }
                }
                .padding(20)
            }
            .refreshable { await content.loadFeatured() }
        }
        .background(Color.ftcCream)
    }

    private func openQuote(_ f: FeaturedContent) {
        quote.prefill(cuisine: f.cuisine); router.open(.quote)
    }
}

// MARK: - Contact

struct ContactView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var m: ContactModel
    @EnvironmentObject var content: ContentModel
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(spacing: 0) {
            ScreenHeader(title: "Contact Us", subtitle: SiteContent.contactIntro, onAccount: { router.push(.account) })
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    FillImage(name: "photo_contact", height: 160)
                        .clipShape(RoundedRectangle(cornerRadius: 22)).accessibilityHidden(true)
                    if let id = m.sentId {
                        Card {
                            Label("Message sent", systemImage: "checkmark.circle.fill").font(.ftcHeadline).foregroundStyle(Color.ftcSuccess)
                            Text("Thanks for getting in touch – we’ll reply by email soon.").font(.ftcBody).foregroundStyle(Color.ftcInk)
                            Text("Reference: \(id)").font(.ftcLabel).foregroundStyle(Color.ftcNavy).textSelection(.enabled).accessibilityIdentifier("contactRef")
                            Button("Send another message") { m.sentId = nil }.buttonStyle(SecondaryButtonStyle())
                        }
                    } else {
                        if let b = m.banner { Banner(message: b.message, offline: b.offline) }
                        Card {
                            FtcTextField(label: "Your Full Name", text: $m.name, error: m.errors["name"], contentType: .name, capitalization: .words)
                            FtcTextField(label: "Your Email Address", text: $m.email, error: m.errors["email"], keyboard: .emailAddress, contentType: .emailAddress, capitalization: .never)
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Topic").font(.ftcLabel).foregroundStyle(Color.ftcNavy)
                                Picker("Topic", selection: $m.topic) {
                                    ForEach(SiteContent.contactTopics, id: \.self) { Text($0).tag($0) }
                                }
                                .pickerStyle(.menu)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 6).padding(.vertical, 6)
                                .background(RoundedRectangle(cornerRadius: 14).fill(.white))
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.ftcLine))
                            }
                            FtcTextField(label: "Your Message", text: $m.message, error: m.errors["message"], multiline: true)
                            PrimaryButton(title: "Send Message", busy: m.submitting, systemImage: "paperplane.fill") {
                                hideKeyboard(); Task { await m.submit() }
                            }
                            .accessibilityIdentifier("sendMessage")
                        }
                    }
                    Card {
                        Text("Other ways to reach us").font(.ftcHeadline).foregroundStyle(Color.ftcNavy)
                        if let e = content.contactEmail, let url = URL(string: "mailto:\(e)") {
                            Button { openURL(url) } label: { Label(e, systemImage: "envelope.fill").font(.ftcBodyMedium) }.frame(minHeight: 44)
                        }
                        Button { openURL(SiteContent.instagramURL) } label: { Label("@foodtruckcentre on Instagram", systemImage: "camera.fill").font(.ftcBodyMedium) }.frame(minHeight: 44)
                        Button { openURL(SiteContent.websiteURL) } label: { Label("foodtruckcentre.com.au", systemImage: "safari.fill").font(.ftcBodyMedium) }.frame(minHeight: 44)
                    }
                }
                .padding(20)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .background(Color.ftcCream)
        .onAppear { m.touch(); content.refreshContactEmail() }
        .keyboardDoneButton()
    }
}

// MARK: - About

struct AboutView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var quote: QuoteModel
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack(spacing: 0) {
            ScreenHeader(title: "How to Book with Us", subtitle: "Time has changed", onBack: { dismiss() })
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    FillImage(name: "photo_howto", height: 220, label: "Guests at a food truck event")
                        .clipShape(RoundedRectangle(cornerRadius: 28))
                    Card {
                        Text(SiteContent.intro).font(.ftcBody).foregroundStyle(Color.ftcInk)
                        Text(SiteContent.intro2).font(.ftcBody).foregroundStyle(Color.ftcInk)
                        Text(SiteContent.intro3.replacingOccurrences(of: " below", with: "")).font(.ftcBody).foregroundStyle(Color.ftcInk)
                    }
                    SectionTitle(title: "The process")
                    ProcessSteps()
                    Banner(message: SiteContent.bookingNote, isError: false)
                    PrimaryButton(title: "Request a Quote", systemImage: "doc.text.fill") { quote.touch(); router.open(.quote) }
                    Button("Vendor Terms") { router.push(.legal(.vendor)) }.buttonStyle(SecondaryButtonStyle())
                }
                .padding(20)
            }
        }
        .background(Color.ftcCream)
    }
}

// MARK: - Legal (live from the website, bundled copy when offline)

struct LegalView: View {
    let page: SiteContent.LegalPage
    @EnvironmentObject var content: ContentModel
    @EnvironmentObject var router: Router
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            ScreenHeader(title: page.title, subtitle: "Food Truck Centre", onBack: { dismiss() })
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    switch content.legal[page] ?? .loading {
                    case .loading: LoadingCard()
                    case .error(let m, let o): ErrorCard(message: m, offline: o) { Task { await content.loadLegal(page, force: true) } }
                    case .ready(let c):
                        if c.offlineCopy { Banner(message: "You’re offline, so this is the copy saved in the app. Pull down to refresh.", offline: true) }
                        if let d = Self.date(c.updated) { Text("Last updated \(d)").font(.ftcCaption).foregroundStyle(Color.ftcMuted) }
                        Card { RichText(blocks: c.blocks, onQuoteLink: { router.open(.quote) }) }
                    }
                }
                .padding(20)
            }
            .refreshable { await content.loadLegal(page, force: true) }
        }
        .background(Color.ftcCream)
        .task { await content.loadLegal(page) }
    }

    static func date(_ s: String) -> String? {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        guard let d = f.date(from: s) else { return nil }
        let o = DateFormatter(); o.locale = Locale(identifier: "en_AU"); o.dateStyle = .long
        return o.string(from: d)
    }
}

// MARK: - Shared content helpers

struct LoadingCard: View {
    var body: some View {
        Card {
            HStack(spacing: 12) { ProgressView(); Text("Loading…").font(.ftcBody).foregroundStyle(Color.ftcMuted) }
        }
        .accessibilityLabel("Loading")
    }
}

struct ErrorCard: View {
    let message: String
    let offline: Bool
    let retry: () -> Void
    var body: some View {
        Card {
            Banner(message: message, offline: offline)
            Button("Try again", action: retry).buttonStyle(SecondaryButtonStyle())
        }
    }
}

/// Renders parsed website HTML natively. Links open in the browser, except links to the
/// website's quote form, which open the in-app form.
struct RichText: View {
    let blocks: [HtmlBlock]
    var onQuoteLink: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(Array(blocks.enumerated()), id: \.offset) { _, b in
                switch b {
                case .heading(let t, let n, let level):
                    Text(n.map { "\($0). \(t)" } ?? t)
                        .font(level <= 2 ? .ftcHeadline : .ftcBodyMedium)
                        .foregroundStyle(Color.ftcNavy)
                        .padding(.top, level <= 2 ? 6 : 2)
                        .accessibilityAddTraits(.isHeader)
                case .paragraph(let spans):
                    Text(attributed(spans)).font(.ftcBody).foregroundStyle(Color.ftcInk)
                case .bullet(let spans):
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        if !(spans.first?.text.first?.isNumber ?? false) {
                            Circle().fill(Color.ftcRed).frame(width: 6, height: 6).offset(y: -2)
                        }
                        Text(attributed(spans)).font(.ftcBody).foregroundStyle(Color.ftcInk)
                    }
                case .image(let u):
                    AsyncImage(url: u) { $0.resizable().scaledToFit() } placeholder: { Color.ftcCreamDeep.frame(height: 160) }
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .accessibilityHidden(true)
                }
            }
        }
        .environment(\.openURL, OpenURLAction { url in
            if Self.isQuoteLink(url) { onQuoteLink(); return .handled }
            return .systemAction(url)
        })
        .tint(.ftcBlue)
    }

    static func isQuoteLink(_ url: URL) -> Bool {
        url.fragment == "form" && (url.host == nil || url.host?.hasSuffix("foodtruckcentre.com.au") == true)
    }

    private func attributed(_ spans: [HtmlSpan]) -> AttributedString {
        var out = AttributedString()
        for s in spans {
            var a = AttributedString(s.text)
            if s.bold { a.font = FtcFont.poppins(15, .semibold) }
            if s.italic { a.inlinePresentationIntent = .emphasized }
            if let l = s.link, let u = URL(string: l, relativeTo: AppConfig.websiteBase)?.absoluteURL,
               ["https", "http", "mailto", "tel"].contains(u.scheme?.lowercased() ?? "") {
                a.link = u
                a.underlineStyle = .single
            }
            out += a
        }
        return out
    }
}
