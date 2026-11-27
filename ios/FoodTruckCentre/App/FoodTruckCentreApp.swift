import SwiftUI

@main
struct FoodTruckCentreApp: App {
    init() {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-ui-testing") { UIView.setAnimationsEnabled(false) }
        #endif
        let nav = UINavigationBarAppearance()
        nav.configureWithOpaqueBackground()
        nav.backgroundColor = UIColor(Color.ftcCream)
        nav.shadowColor = .clear
        UINavigationBar.appearance().standardAppearance = nav
        UINavigationBar.appearance().scrollEdgeAppearance = nav
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .preferredColorScheme(.light)
                .tint(.ftcRed)
        }
    }
}

enum Tab: Hashable { case home, services, quote, featuring, contact }

enum Route: Hashable {
    case about, account, legal(SiteContent.LegalPage), success(id: String, name: String)
}

@MainActor
final class Router: ObservableObject {
    @Published var tab: Tab = .home
    @Published var path = NavigationPath()
    func push(_ r: Route) { path.append(r) }
    func popToRoot() { path = NavigationPath() }
    func open(_ t: Tab) { popToRoot(); tab = t }
}

struct RootView: View {
    @State private var seenIntro = false
    @StateObject private var sessions = SessionStore.shared
    @StateObject private var router = Router()
    @StateObject private var quote = QuoteModel()
    @StateObject private var contact = ContactModel()
    @StateObject private var content = ContentModel()
    @StateObject private var auth = AuthModel()

    var body: some View {
        Group {
            if !seenIntro {
                IntroView { withAnimation { seenIntro = true } }
            } else if sessions.session == nil {
                NavigationStack { AuthView() }
            } else {
                MainView()
            }
        }
        .environmentObject(sessions)
        .environmentObject(router)
        .environmentObject(quote)
        .environmentObject(contact)
        .environmentObject(content)
        .environmentObject(auth)
        .onAppear { content.start() }
        .onChange(of: sessions.session) { s in
            if s == nil {
                // Logged out (or session expired): forget anything typed by the previous customer.
                quote.clearForm(); contact.clearForm(); auth.reset(); router.popToRoot(); router.tab = .home
            } else {
                quote.fillFromProfile(s?.profile); contact.fillFromProfile(s?.profile)
            }
        }
    }
}

struct MainView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var quote: QuoteModel
    @EnvironmentObject var sessions: SessionStore

    var body: some View {
        NavigationStack(path: $router.path) {
            VStack(spacing: 0) {
                ZStack {
                    switch router.tab {
                    case .home: HomeView()
                    case .services: ServicesView()
                    case .quote: QuoteView()
                    case .featuring: FeaturingView()
                    case .contact: ContactView()
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                FtcTabBar()
            }
            .background(Color.ftcCream.ignoresSafeArea())
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: Route.self) { r in
                Group {
                    switch r {
                    case .about: AboutView()
                    case .account: AccountView()
                    case .legal(let p): LegalView(page: p)
                    case .success(let id, let name): SuccessView(enquiryId: id, firstName: name)
                    }
                }
                .toolbar(.hidden, for: .navigationBar)
            }
        }
        .onAppear { quote.fillFromProfile(sessions.session?.profile) }
        .onChange(of: quote.success?.id) { id in
            if let id, let s = quote.success { router.push(.success(id: id, name: s.firstName)); quote.success = nil }
        }
    }
}

/// Bottom navigation with a raised centre "Get Quote" button, like the Android app.
struct FtcTabBar: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var quote: QuoteModel

    var body: some View {
        HStack(alignment: .bottom, spacing: 0) {
            item(.home, "Home", "house.fill")
            item(.services, "Services", "square.grid.2x2.fill")
            Button {
                quote.touch(); router.open(.quote)
            } label: {
                VStack(spacing: 4) {
                    ZStack {
                        Circle().fill(Color.ftcRed).frame(width: 58, height: 58)
                            .shadow(color: Color.ftcRed.opacity(0.35), radius: 10, y: 4)
                        Image(systemName: "doc.text.fill").font(.system(size: 22, weight: .semibold)).foregroundStyle(.white)
                    }
                    .offset(y: -14)
                    .padding(.bottom, -14)
                    Text("Get Quote").font(FtcFont.poppins(11, .semibold, relativeTo: .caption2))
                        .foregroundStyle(router.tab == .quote ? Color.ftcRed : Color.ftcNavy)
                }
                .frame(maxWidth: .infinity)
            }
            .accessibilityLabel("Get Quote")
            .accessibilityIdentifier("tab.quote")
            .accessibilityAddTraits(router.tab == .quote ? .isSelected : [])
            item(.featuring, "Featuring", "star.fill")
            item(.contact, "Contact", "envelope.fill")
        }
        .padding(.top, 8)
        .padding(.bottom, 4)
        .background(Color.white.shadow(color: Color.ftcNavy.opacity(0.08), radius: 10, y: -4).ignoresSafeArea(edges: .bottom))
    }

    private func item(_ t: Tab, _ title: String, _ icon: String) -> some View {
        let on = router.tab == t
        return Button { router.open(t) } label: {
            VStack(spacing: 4) {
                Image(systemName: icon).font(.system(size: 20, weight: .semibold))
                Text(title).font(FtcFont.poppins(11, on ? .semibold : .medium, relativeTo: .caption2))
            }
            .foregroundStyle(on ? Color.ftcRed : Color.ftcNavy.opacity(0.75))
            .frame(maxWidth: .infinity, minHeight: 48)
        }
        .accessibilityLabel(title)
        .accessibilityIdentifier("tab.\(title.lowercased())")
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}
