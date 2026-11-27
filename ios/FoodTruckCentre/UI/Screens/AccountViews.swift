import SwiftUI

// MARK: - Log in / Create account (required before using the app)

struct AuthView: View {
    @EnvironmentObject var m: AuthModel
    @State private var showForgot = false
    @State private var legal: SiteContent.LegalPage? = nil

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                VStack(spacing: 10) {
                    Image("logo_badge").resizable().scaledToFit().frame(width: 120, height: 120).accessibilityLabel("Food Truck Centre logo")
                    Text(m.mode == .register ? "Create your account" : "Welcome back")
                        .font(.ftcTitle).foregroundStyle(Color.ftcNavy).accessibilityAddTraits(.isHeader).accessibilityIdentifier("authTitle")
                    Text("Log in to request quotes from Australia’s best food trucks and follow your enquiries.")
                        .font(.ftcBody).foregroundStyle(Color.ftcMuted).multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 12)

                Picker("Mode", selection: $m.mode) {
                    Text("Log in").tag(AuthModel.Mode.login)
                    Text("Create account").tag(AuthModel.Mode.register)
                }
                .pickerStyle(.segmented)
                .onChange(of: m.mode) { _ in m.errors = [:]; m.banner = nil }

                if let b = m.banner { Banner(message: b.message, offline: b.offline) }

                Card {
                    if m.mode == .register {
                        FtcTextField(label: "Full name", text: $m.name, error: m.errors["name"], contentType: .name, capitalization: .words)
                    }
                    FtcTextField(label: "Email", text: $m.email, error: m.errors["email"], keyboard: .emailAddress, contentType: .username, capitalization: .never)
                    if m.mode == .register {
                        FtcTextField(label: "Phone number", text: $m.phone, error: m.errors["phone"], keyboard: .phonePad, contentType: .telephoneNumber)
                    }
                    FtcTextField(label: "Password", text: $m.password, error: m.errors["password"],
                                 placeholder: m.mode == .register ? "8+ characters, a letter and a number" : "",
                                 contentType: m.mode == .register ? .newPassword : .password, isPassword: true)
                        .submitLabel(m.mode == .register ? .done : .go)
                        .onSubmit { if m.mode == .login { hideKeyboard(); Task { await m.submit() } } }
                    if m.mode == .register {
                        Button { m.acceptedTerms.toggle(); m.errors["terms"] = nil } label: {
                            HStack(alignment: .top, spacing: 10) {
                                Image(systemName: m.acceptedTerms ? "checkmark.square.fill" : "square").font(.system(size: 22))
                                    .foregroundStyle(m.acceptedTerms ? Color.ftcRed : Color.ftcMuted)
                                Text("I accept the Terms and Privacy Policy").font(.ftcBody).foregroundStyle(Color.ftcInk).multilineTextAlignment(.leading)
                            }
                        }
                        .accessibilityLabel("Accept terms")
                        .accessibilityValue(m.acceptedTerms ? "Terms accepted" : "Not accepted")
                        .accessibilityIdentifier("acceptTerms")
                        HStack(spacing: 16) {
                            Button("Read Terms") { legal = .terms }.font(.ftcLabel)
                            Button("Read Privacy Policy") { legal = .privacy }.font(.ftcLabel)
                        }
                        if let e = m.errors["terms"] { FieldError(text: e) }
                    }
                    PrimaryButton(title: m.mode == .register ? "Create my account" : "Log in", busy: m.busy) {
                        hideKeyboard(); Task { await m.submit() }
                    }
                    .accessibilityIdentifier(m.mode == .register ? "createAccount" : "logIn")
                    if m.mode == .login {
                        Button("Forgot password?") { m.forgotEmail = m.email; m.forgotSent = nil; showForgot = true }
                            .font(.ftcLabel).frame(maxWidth: .infinity, minHeight: 44)
                    }
                }

                Button(m.mode == .register ? "Already have an account? Log in" : "New here? Create an account") {
                    m.mode = m.mode == .register ? .login : .register
                }
                .font(.ftcBodyMedium).foregroundStyle(Color.ftcNavy).frame(maxWidth: .infinity, minHeight: 44)

                Text("No payment is taken in this app.").font(.ftcCaption).foregroundStyle(Color.ftcMuted).frame(maxWidth: .infinity)
            }
            .padding(20)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.ftcCream.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showForgot) { ForgotPasswordView() }
        .keyboardDoneButton()
        .sheet(item: $legal) { p in NavigationStack { LegalView(page: p) } }
    }
}

extension SiteContent.LegalPage: Identifiable { var id: String { rawValue } }

struct ForgotPasswordView: View {
    @EnvironmentObject var m: AuthModel
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 18) {
                Text("Enter the email you registered with and we’ll send you a link to choose a new password.")
                    .font(.ftcBody).foregroundStyle(Color.ftcMuted)
                if let sent = m.forgotSent {
                    Banner(message: sent, isError: false)
                    Button("Back to log in") { dismiss() }.buttonStyle(PrimaryButtonStyle())
                } else {
                    if let b = m.banner { Banner(message: b.message, offline: b.offline) }
                    FtcTextField(label: "Email", text: $m.forgotEmail, error: m.errors["email"], keyboard: .emailAddress, contentType: .username, capitalization: .never)
                    PrimaryButton(title: "Send reset link", busy: m.busy) { hideKeyboard(); Task { await m.sendReset() } }
                }
                Spacer()
            }
            .padding(20)
            .background(Color.ftcCream.ignoresSafeArea())
            .navigationTitle("Reset password")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Close") { dismiss() } } }
        }
    }
}

// MARK: - My account and enquiries

struct AccountView: View {
    @EnvironmentObject var m: AuthModel
    @EnvironmentObject var sessions: SessionStore
    @EnvironmentObject var router: Router
    @Environment(\.dismiss) private var dismiss
    @State private var confirmLogout = false
    @State private var showDelete = false

    var body: some View {
        VStack(spacing: 0) {
            ScreenHeader(title: "My account", subtitle: sessions.session?.profile.email, onBack: { dismiss() })
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    SectionTitle(title: "My enquiries", subtitle: "Track the progress of your quote requests and messages")
                    switch m.enquiries {
                    case .loading: LoadingCard()
                    case .error(let msg, let offline): ErrorCard(message: msg, offline: offline) { Task { await m.loadEnquiries() } }
                    case .ready(let list):
                        if list.isEmpty {
                            Card {
                                Text("No enquiries yet").font(.ftcHeadline).foregroundStyle(Color.ftcNavy)
                                Text("When you request a quote or send us a message it will appear here.").font(.ftcBody).foregroundStyle(Color.ftcMuted)
                                Button("Request a Quote") { router.open(.quote) }.buttonStyle(PrimaryButtonStyle())
                            }
                        } else {
                            ForEach(list) { EnquiryCard(e: $0) }
                        }
                    }

                    SectionTitle(title: "My details")
                    Card {
                        if let b = m.banner { Banner(message: b.message, offline: b.offline) }
                        FtcTextField(label: "Full name", text: $m.profileName, error: m.errors["name"], contentType: .name, capitalization: .words)
                        FtcTextField(label: "Phone number", text: $m.profilePhone, error: m.errors["phone"], keyboard: .phonePad, contentType: .telephoneNumber)
                        if m.profileSaved { Label("Details saved", systemImage: "checkmark.circle.fill").font(.ftcLabel).foregroundStyle(Color.ftcSuccess) }
                        PrimaryButton(title: "Save details", busy: m.busy) { hideKeyboard(); Task { await m.saveProfile() } }
                    }

                    Button { confirmLogout = true } label: { Label("Log out", systemImage: "rectangle.portrait.and.arrow.right") }
                        .buttonStyle(SecondaryButtonStyle())
                        .accessibilityIdentifier("logOut")
                    Button(role: .destructive) { m.errors = [:]; showDelete = true } label: {
                        Text("Delete my account").font(.ftcLabel).foregroundStyle(Color.ftcError).frame(maxWidth: .infinity, minHeight: 44)
                    }
                    Text("Deleting your account removes your login and personal details. Enquiries already sent stay with Food Truck Centre so they can be answered.")
                        .font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                }
                .padding(20)
            }
            .refreshable { await m.loadEnquiries() }
        }
        .background(Color.ftcCream)
        .task { await m.loadAccount(sessions.session?.profile) }
        .confirmationDialog("Log out of Food Truck Centre?", isPresented: $confirmLogout, titleVisibility: .visible) {
            Button("Log out", role: .destructive) { Task { await API.shared.logout() } }
        }
        .sheet(isPresented: $showDelete) { DeleteAccountView() }
        .keyboardDoneButton()
    }
}

struct EnquiryCard: View {
    let e: MyEnquiry
    var body: some View {
        Card(padding: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(e.title).font(.ftcHeadline).foregroundStyle(Color.ftcNavy)
                    Text(e.enquiryId).font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                }
                Spacer()
                Text(e.stageLabel).font(.ftcLabel).foregroundStyle(stageColor.fg)
                    .padding(.horizontal, 10).padding(.vertical, 5).background(Capsule().fill(stageColor.bg))
                    .accessibilityLabel("Status: \(e.stageLabel)")
            }
            if let s = e.summary, !s.isEmpty { Text(s).font(.ftcBody).foregroundStyle(Color.ftcInk) }
            HStack(spacing: 14) {
                if let d = e.eventDate, let shown = Self.show(d) { Label(shown, systemImage: "calendar") }
                Label("Sent \(Self.sent(e.submittedAt))", systemImage: "paperplane")
            }
            .font(.ftcCaption).foregroundStyle(Color.ftcMuted)
        }
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("enquiry.\(e.enquiryId)")
    }

    private var stageColor: (fg: Color, bg: Color) {
        switch e.stage {
        case "contacted": return (.ftcBlue, Color.ftcBlue.opacity(0.12))
        case "quoted": return (.ftcNavyDeep, .ftcMustardSoft)
        case "confirmed": return (.ftcSuccess, Color.ftcSuccess.opacity(0.12))
        case "closed": return (.ftcMuted, Color.ftcMuted.opacity(0.12))
        default: return (.ftcRed, .ftcRedSoft)
        }
    }

    static func show(_ ymd: String) -> String? {
        QuoteModel.isoDate.date(from: ymd).map { QuoteModel.displayDate.string(from: $0) }
    }

    static func sent(_ iso: String) -> String {
        let f = ISO8601DateFormatter()
        guard let d = f.date(from: iso) else { return iso }
        let o = DateFormatter(); o.locale = Locale(identifier: "en_AU"); o.dateFormat = "d MMM yyyy"
        return o.string(from: d)
    }
}

struct DeleteAccountView: View {
    @EnvironmentObject var m: AuthModel
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 18) {
                Text("This permanently deletes your Food Truck Centre account. Enter your password to confirm.")
                    .font(.ftcBody).foregroundStyle(Color.ftcInk)
                FtcTextField(label: "Password", text: $m.deletePassword, error: m.errors["delete_password"], contentType: .password, isPassword: true)
                Button {
                    hideKeyboard()
                    Task { if await m.deleteAccount() { dismiss() } }
                } label: {
                    HStack { if m.busy { ProgressView().tint(.white) }; Text("Delete my account") }
                }
                .buttonStyle(PrimaryButtonStyle(color: .ftcError))
                .disabled(m.busy)
                Spacer()
            }
            .padding(20)
            .background(Color.ftcCream.ignoresSafeArea())
            .navigationTitle("Delete account")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } } }
        }
        .presentationDetents([.medium])
    }
}
