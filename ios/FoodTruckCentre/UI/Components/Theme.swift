import SwiftUI

/// Brand colours taken from the website and logo (same values as the Android app).
extension Color {
    init(hex: UInt32) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255, opacity: 1)
    }
    static let ftcRed = Color(hex: 0xD62828)
    static let ftcRedDeep = Color(hex: 0xA61B1B)
    static let ftcRedSoft = Color(hex: 0xFDE8E6)
    static let ftcNavy = Color(hex: 0x0A1F5C)
    static let ftcNavyDeep = Color(hex: 0x061340)
    static let ftcNavySoft = Color(hex: 0xE7EBF6)
    static let ftcBlue = Color(hex: 0x046BD2)
    static let ftcMustard = Color(hex: 0xF4B400)
    static let ftcMustardSoft = Color(hex: 0xFFF3CF)
    static let ftcCream = Color(hex: 0xFFF8F1)
    static let ftcCreamDeep = Color(hex: 0xF7EBDD)
    static let ftcInk = Color(hex: 0x152238)
    static let ftcMuted = Color(hex: 0x5D6676)
    static let ftcLine = Color(hex: 0xEADFD1)
    static let ftcSuccess = Color(hex: 0x1E8E5A)
    static let ftcError = Color(hex: 0xC62828)
}

/// Poppins (the website's font), scaled with Dynamic Type.
enum FtcFont {
    enum Weight: String { case regular = "Regular", medium = "Medium", semibold = "SemiBold", bold = "Bold", extrabold = "ExtraBold" }
    static func poppins(_ size: CGFloat, _ weight: Weight = .regular, relativeTo style: Font.TextStyle = .body) -> Font {
        .custom("Poppins-\(weight.rawValue)", size: size, relativeTo: style)
    }
}

extension Font {
    static let ftcDisplay = FtcFont.poppins(30, .extrabold, relativeTo: .largeTitle)
    static let ftcTitle = FtcFont.poppins(22, .bold, relativeTo: .title)
    static let ftcHeadline = FtcFont.poppins(17, .semibold, relativeTo: .headline)
    static let ftcBody = FtcFont.poppins(15, .regular, relativeTo: .body)
    static let ftcBodyMedium = FtcFont.poppins(15, .medium, relativeTo: .body)
    static let ftcCaption = FtcFont.poppins(13, .regular, relativeTo: .caption)
    static let ftcLabel = FtcFont.poppins(13, .semibold, relativeTo: .caption)
    static let ftcButton = FtcFont.poppins(16, .semibold, relativeTo: .headline)
}

// MARK: - Buttons

struct PrimaryButtonStyle: ButtonStyle {
    var color: Color = .ftcRed
    @Environment(\.isEnabled) private var enabled
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.ftcButton)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, minHeight: 54)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(enabled ? color : color.opacity(0.5)))
            .shadow(color: color.opacity(configuration.isPressed ? 0.1 : 0.28), radius: 10, y: 5)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.ftcButton)
            .foregroundStyle(Color.ftcNavy)
            .frame(maxWidth: .infinity, minHeight: 52)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Color.ftcNavy.opacity(0.25), lineWidth: 1.5))
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(configuration.isPressed ? Color.ftcNavySoft : .white))
    }
}

/// Red button with a spinner while busy (also blocks double taps).
struct PrimaryButton: View {
    let title: String
    var busy = false
    var systemImage: String? = nil
    let action: () -> Void
    var body: some View {
        Button(action: { if !busy { action() } }) {
            HStack(spacing: 10) {
                if busy { ProgressView().tint(.white) }
                else if let systemImage { Image(systemName: systemImage) }
                Text(busy ? "Sending…" : title)
            }
        }
        .buttonStyle(PrimaryButtonStyle())
        .disabled(busy)
        .accessibilityLabel(title)
    }
}

// MARK: - Cards and layout

struct Card<Content: View>: View {
    var padding: CGFloat = 18
    @ViewBuilder var content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 12) { content }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(.white))
            .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(Color.ftcLine, lineWidth: 1))
            .shadow(color: Color.ftcNavy.opacity(0.06), radius: 12, y: 6)
    }
}

struct SectionTitle: View {
    let title: String
    var subtitle: String? = nil
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Capsule().fill(Color.ftcMustard).frame(width: 22, height: 5)
                Text(title).font(.ftcTitle).foregroundStyle(Color.ftcNavy).accessibilityAddTraits(.isHeader)
            }
            if let subtitle { Text(subtitle).font(.ftcBody).foregroundStyle(Color.ftcMuted) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Yellow-and-red awning stripe from the logo, used as a header accent.
struct AwningStripe: View {
    var height: CGFloat = 10
    var body: some View {
        GeometryReader { geo in
            let w: CGFloat = 26
            let count = Int(geo.size.width / w) + 2
            HStack(spacing: 0) {
                ForEach(0..<count, id: \.self) { i in
                    Rectangle().fill(i.isMultiple(of: 2) ? Color.ftcMustard : Color.ftcRed).frame(width: w)
                }
            }
        }
        .frame(height: height)
        .clipped()
        .accessibilityHidden(true)
    }
}

/// Screen header with the logo, an optional back button and the account button.
struct ScreenHeader: View {
    let title: String
    var subtitle: String? = nil
    var onBack: (() -> Void)? = nil
    var onAccount: (() -> Void)? = nil
    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                if let onBack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left").font(.system(size: 17, weight: .bold)).foregroundStyle(Color.ftcNavy)
                            .frame(width: 44, height: 44).background(Circle().fill(Color.ftcNavySoft))
                    }
                    .accessibilityLabel("Back")
                } else {
                    Image("logo_badge").resizable().scaledToFit().frame(width: 44, height: 44).accessibilityHidden(true)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.ftcHeadline).foregroundStyle(Color.ftcNavy).lineLimit(1).accessibilityAddTraits(.isHeader)
                    if let subtitle { Text(subtitle).font(.ftcCaption).foregroundStyle(Color.ftcMuted).lineLimit(1) }
                }
                Spacer(minLength: 0)
                if let onAccount {
                    Button(action: onAccount) {
                        Image(systemName: "person.crop.circle.fill").font(.system(size: 26)).foregroundStyle(Color.ftcNavy)
                            .frame(width: 44, height: 44)
                    }
                    .accessibilityLabel("My account")
                    .accessibilityIdentifier("accountButton")
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            AwningStripe(height: 5)
        }
        .background(Color.ftcCream)
    }
}

struct Banner: View {
    let message: String
    var offline = false
    var isError = true
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: offline ? "wifi.slash" : (isError ? "exclamationmark.circle.fill" : "checkmark.circle.fill"))
                .foregroundStyle(isError ? Color.ftcError : Color.ftcSuccess)
            Text(message).font(.ftcBody).foregroundStyle(Color.ftcInk)
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14).fill(isError ? Color.ftcRedSoft : Color.ftcSuccess.opacity(0.12)))
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("banner")
    }
}

// MARK: - Form fields

struct FtcTextField: View {
    let label: String
    @Binding var text: String
    var error: String? = nil
    var placeholder = ""
    var keyboard: UIKeyboardType = .default
    var contentType: UITextContentType? = nil
    var capitalization: TextInputAutocapitalization = .sentences
    var isPassword = false
    var multiline = false
    var required = true
    var identifier: String? = nil
    @State private var revealed = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 2) {
                Text(label).font(.ftcLabel).foregroundStyle(Color.ftcNavy)
                if required { Text("*").font(.ftcLabel).foregroundStyle(Color.ftcRed).accessibilityHidden(true) }
            }
            HStack {
                Group {
                    if isPassword && !revealed {
                        SecureField(placeholder, text: $text)
                    } else if multiline {
                        TextField(placeholder, text: $text, axis: .vertical).lineLimit(4...8)
                    } else {
                        TextField(placeholder, text: $text)
                    }
                }
                .font(.ftcBody)
                .foregroundStyle(Color.ftcInk)
                .keyboardType(keyboard)
                .textContentType(contentType)
                .textInputAutocapitalization(isPassword ? .never : capitalization)
                .autocorrectionDisabled(isPassword || keyboard == .emailAddress)
                .accessibilityLabel(label)
                .accessibilityIdentifier(identifier ?? label)
                if isPassword {
                    Button { revealed.toggle() } label: {
                        Image(systemName: revealed ? "eye.slash" : "eye").foregroundStyle(Color.ftcMuted)
                    }
                    .accessibilityLabel(revealed ? "Hide password" : "Show password")
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(RoundedRectangle(cornerRadius: 14).fill(.white))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(error == nil ? Color.ftcLine : Color.ftcError, lineWidth: error == nil ? 1 : 1.5))
            if let error { FieldError(text: error) }
        }
    }
}

struct FieldError: View {
    let text: String
    var body: some View {
        Label(text, systemImage: "exclamationmark.circle")
            .font(.ftcCaption)
            .foregroundStyle(Color.ftcError)
            .accessibilityLabel("Error: \(text)")
    }
}

/// Row of pill buttons for a single choice (Yes/No, event type, ...).
struct ChoiceChips: View {
    let label: String
    let options: [String]
    @Binding var selection: String?
    var error: String? = nil
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 2) {
                Text(label).font(.ftcLabel).foregroundStyle(Color.ftcNavy)
                Text("*").font(.ftcLabel).foregroundStyle(Color.ftcRed).accessibilityHidden(true)
            }
            FlowLayout(spacing: 8) {
                ForEach(options, id: \.self) { o in
                    let on = selection == o
                    Button { selection = o } label: {
                        HStack(spacing: 6) {
                            if on { Image(systemName: "checkmark").font(.system(size: 12, weight: .bold)) }
                            Text(o).font(.ftcBodyMedium)
                        }
                        .padding(.horizontal, 16).frame(minHeight: 44)
                        .foregroundStyle(on ? .white : Color.ftcNavy)
                        .background(Capsule().fill(on ? Color.ftcNavy : .white))
                        .overlay(Capsule().stroke(on ? Color.ftcNavy : Color.ftcLine, lineWidth: 1))
                    }
                    .accessibilityLabel("\(label): \(o)")
                    .accessibilityAddTraits(on ? .isSelected : [])
                }
            }
            if let error { FieldError(text: error) }
        }
    }
}

/// Simple wrapping layout for chips.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxW = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowH: CGFloat = 0, widest: CGFloat = 0
        for v in subviews {
            let s = v.sizeThatFits(.unspecified)
            if x + s.width > maxW, x > 0 { x = 0; y += rowH + spacing; rowH = 0 }
            x += s.width + spacing; rowH = max(rowH, s.height); widest = max(widest, x - spacing)
        }
        return CGSize(width: min(widest, maxW), height: y + rowH)
    }
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowH: CGFloat = 0
        for v in subviews {
            let s = v.sizeThatFits(.unspecified)
            if x + s.width > bounds.maxX, x > bounds.minX { x = bounds.minX; y += rowH + spacing; rowH = 0 }
            v.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(s))
            x += s.width + spacing; rowH = max(rowH, s.height)
        }
    }
}

extension View {
    func hideKeyboard() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

/// Adds a "Done" button above the keyboard (number and phone pads have no return key).
struct KeyboardDone: ViewModifier {
    func body(content: Content) -> some View {
        content.toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }
                .font(.ftcButton)
                .accessibilityIdentifier("keyboardDone")
            }
        }
    }
}

extension View {
    func keyboardDoneButton() -> some View { modifier(KeyboardDone()) }
}

/// A photo that fills a fixed-height box at the available width and is cropped to it
/// (a bare scaledToFill image would make its container wider than the screen).
struct FillImage: View {
    let name: String
    let height: CGFloat
    var label: String? = nil
    var body: some View {
        Color.ftcCreamDeep
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .overlay(Image(name).resizable().scaledToFill())
            .clipped()
            .accessibilityElement()
            .accessibilityAddTraits(.isImage)
            .accessibilityLabel(label ?? "")
            .accessibilityHidden(label == nil)
    }
}
