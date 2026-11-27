import SwiftUI

struct QuoteView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var m: QuoteModel
    @State private var showDate = false
    @State private var limitMessage = false

    var body: some View {
        VStack(spacing: 0) {
            ScreenHeader(title: "Request a Quote", subtitle: "Event catering enquiry", onAccount: { router.push(.account) })
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        Text(SiteContent.formIntro).font(.ftcBody).foregroundStyle(Color.ftcMuted)
                        if let b = m.banner { Banner(message: b.message, offline: b.offline) }

                        section("Select Food Truck Cuisines", icon: "fork.knife") {
                            Text("tick 3 max. · \(m.cuisines.count) of \(SiteContent.maxCuisines) cuisines selected").font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                            VStack(spacing: 8) {
                                ForEach(SiteContent.Cuisine.allCases) { c in cuisineRow(c) }
                            }
                            if limitMessage { FieldError(text: "You can select a maximum of three cuisines.") }
                            if let e = m.errors["cuisines"] { FieldError(text: e) }
                        }
                        .id("cuisines")

                        section("Your details", icon: "person.fill") {
                            FtcTextField(label: "Your Full Name", text: $m.name, error: m.errors["name"], contentType: .name, capitalization: .words).id("name")
                            FtcTextField(label: "Your Email Address", text: $m.email, error: m.errors["email"], keyboard: .emailAddress, contentType: .emailAddress, capitalization: .never).id("email")
                            FtcTextField(label: "Phone Number", text: $m.phone, error: m.errors["phone"], keyboard: .phonePad, contentType: .telephoneNumber).id("phone")
                        }

                        section("Event details", icon: "calendar") {
                            dateField.id("event_date")
                            FtcTextField(label: "What time is food to be served", text: $m.foodTime, error: m.errors["food_time"], placeholder: "eg. 12:00pm", required: false).id("food_time")
                            FtcTextField(label: "Address of the event", text: $m.address, error: m.errors["event_address"], placeholder: "Full address of the event", contentType: .fullStreetAddress, capitalization: .words, required: false).id("event_address")
                            FtcTextField(label: "Postcode", text: $m.postcode, error: m.errors["postcode"], keyboard: .numberPad, contentType: .postalCode).id("postcode")
                            ChoiceChips(label: "What is the event", options: SiteContent.eventTypes, selection: $m.eventType, error: m.errors["event_type"]).id("event_type")
                            FtcTextField(label: "How many meals to be served (minimum \(SiteContent.minMeals))", text: $m.meals, error: m.errors["meals"], placeholder: "Minimum \(SiteContent.minMeals)", keyboard: .numberPad, identifier: "Number of meals").id("meals")
                            ChoiceChips(label: "Who will be paying for food", options: SiteContent.payers, selection: $m.paying, error: m.errors["paying"]).id("paying")
                        }

                        section("Event requirements", icon: "bolt.fill") {
                            ChoiceChips(label: "Is there power available at the address", options: SiteContent.yesNo, selection: $m.power, error: m.errors["power"]).id("power")
                            ChoiceChips(label: "Will the truck be parked on private land", options: SiteContent.yesNo, selection: $m.privateLand, error: m.errors["private_land"]).id("private_land")
                            ChoiceChips(label: "Is the entrance wider than 3m and at least 5m in length without overhanging obstacles", options: SiteContent.yesNo, selection: $m.entrance, error: m.errors["entrance"]).id("entrance")
                            FtcTextField(label: "Additional information", text: $m.info, error: m.errors["additional_info"], placeholder: "Any other details you would like to share", multiline: true, required: false).id("additional_info")
                        }

                        PrimaryButton(title: "Send Quote Request", busy: m.submitting, systemImage: "paperplane.fill") {
                            hideKeyboard()
                            Task { await m.submit() }
                        }
                        .accessibilityIdentifier("submitQuote")
                        Text("Share your event details to receive quotes and menus from food trucks. Sending an enquiry doesn't confirm a booking, and no payment is taken in this app.")
                            .font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                    }
                    .padding(20)
                    .padding(.bottom, 20)
                }
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: m.scrollTarget) { t in
                    guard let t else { return }
                    withAnimation { proxy.scrollTo(t, anchor: .top) }
                    m.scrollTarget = nil
                }
            }
        }
        .background(Color.ftcCream)
        .onAppear { m.touch() }
        .onChange(of: m.cuisineLimitHit) { _ in
            limitMessage = true
            Task { try? await Task.sleep(nanoseconds: 3_000_000_000); limitMessage = false }
        }
        .sheet(isPresented: $showDate) { datePicker }
        .keyboardDoneButton()
    }

    private func section<C: View>(_ title: String, icon: String, @ViewBuilder _ content: () -> C) -> some View {
        Card {
            HStack(spacing: 10) {
                Image(systemName: icon).foregroundStyle(Color.ftcRed).frame(width: 34, height: 34).background(Circle().fill(Color.ftcRedSoft))
                Text(title).font(.ftcHeadline).foregroundStyle(Color.ftcNavy).accessibilityAddTraits(.isHeader)
            }
            content()
        }
    }

    private func cuisineRow(_ c: SiteContent.Cuisine) -> some View {
        let on = m.cuisines.contains(c)
        return Button { m.toggle(c) } label: {
            HStack(spacing: 12) {
                Image(systemName: on ? "checkmark.square.fill" : "square").font(.system(size: 20)).foregroundStyle(on ? Color.ftcRed : Color.ftcMuted)
                VStack(alignment: .leading, spacing: 1) {
                    Text(c.label).font(.ftcBodyMedium).foregroundStyle(Color.ftcNavy)
                    Text(c.description).font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 12).padding(.vertical, 10)
            .background(RoundedRectangle(cornerRadius: 12).fill(on ? Color.ftcRedSoft : Color.ftcCream))
        }
        .accessibilityLabel(c.label)
        .accessibilityValue(on ? "Selected" : "Not selected")
        .accessibilityAddTraits(on ? .isSelected : [])
    }

    private var dateField: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 2) {
                Text("When is the event").font(.ftcLabel).foregroundStyle(Color.ftcNavy)
                Text("*").font(.ftcLabel).foregroundStyle(Color.ftcRed).accessibilityHidden(true)
            }
            Button { hideKeyboard(); showDate = true } label: {
                HStack {
                    Text(m.eventDate.map { QuoteModel.displayDate.string(from: $0) } ?? "Choose a date")
                        .font(.ftcBody).foregroundStyle(m.eventDate == nil ? Color.ftcMuted : Color.ftcInk)
                    Spacer()
                    Image(systemName: "calendar").foregroundStyle(Color.ftcRed)
                }
                .padding(14)
                .background(RoundedRectangle(cornerRadius: 14).fill(.white))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(m.errors["event_date"] == nil ? Color.ftcLine : Color.ftcError, lineWidth: 1))
            }
            .accessibilityLabel("Event date")
            .accessibilityValue(m.eventDate.map { QuoteModel.displayDate.string(from: $0) } ?? "Not chosen")
            .accessibilityIdentifier("eventDate")
            if let e = m.errors["event_date"] { FieldError(text: e) }
        }
    }

    private var datePicker: some View {
        let today = Calendar.current.startOfDay(for: Date())
        let max = Calendar.current.date(byAdding: .year, value: 3, to: today)!
        return NavigationStack {
            DatePicker("Event date", selection: Binding(get: { m.eventDate ?? today }, set: { m.eventDate = $0; m.errors["event_date"] = nil }),
                       in: today...max, displayedComponents: .date)
                .datePickerStyle(.graphical)
                .tint(.ftcRed)
                .padding()
                .navigationTitle("Event date")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") { if m.eventDate == nil { m.eventDate = today }; showDate = false }.accessibilityIdentifier("dateDone")
                    }
                }
        }
        .presentationDetents([.medium, .large])
    }
}

struct SuccessView: View {
    let enquiryId: String
    let firstName: String
    @EnvironmentObject var router: Router
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                Spacer(minLength: 40)
                Image(systemName: "checkmark.seal.fill").font(.system(size: 76)).foregroundStyle(Color.ftcSuccess).accessibilityHidden(true)
                Text(firstName.isEmpty ? "Thank you!" : "Thank you, \(firstName)!").font(.ftcDisplay).foregroundStyle(Color.ftcNavy)
                    .multilineTextAlignment(.center).accessibilityIdentifier("successTitle")
                Text("Your quote request has been sent. Food trucks will be in touch with quotes and menus soon.")
                    .font(.ftcBody).foregroundStyle(Color.ftcMuted).multilineTextAlignment(.center)
                Card {
                    Text("Your reference").font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                    Text(enquiryId).font(FtcFont.poppins(20, .bold, relativeTo: .title3)).foregroundStyle(Color.ftcNavy).textSelection(.enabled)
                        .accessibilityIdentifier("enquiryId")
                    Text("You can follow its progress any time under My account.").font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                }
                Button("View my enquiries") { router.popToRoot(); router.push(.account) }.buttonStyle(PrimaryButtonStyle())
                Button("Back to home") { router.open(.home) }.buttonStyle(SecondaryButtonStyle())
            }
            .padding(24)
        }
        .background(Color.ftcCream)
    }
}
