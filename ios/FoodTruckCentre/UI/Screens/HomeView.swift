import SwiftUI

struct HomeView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var quote: QuoteModel
    @EnvironmentObject var content: ContentModel
    @EnvironmentObject var sessions: SessionStore

    var body: some View {
        VStack(spacing: 0) {
            ScreenHeader(title: "Food Truck Centre", subtitle: greeting, onAccount: { router.push(.account) })
            ScrollView {
                VStack(alignment: .leading, spacing: 26) {
                    hero
                    VStack(alignment: .leading, spacing: 10) {
                        Text(SiteContent.intro).font(.ftcBody).foregroundStyle(Color.ftcInk)
                        Text(SiteContent.intro2).font(.ftcBody).foregroundStyle(Color.ftcMuted)
                    }
                    .padding(.horizontal, 20)

                    VStack(spacing: 12) {
                        ForEach(SiteContent.highlights) { h in
                            Card {
                                HStack(alignment: .top, spacing: 14) {
                                    Image(systemName: h.symbol).font(.system(size: 20, weight: .semibold)).foregroundStyle(Color.ftcRed)
                                        .frame(width: 46, height: 46).background(Circle().fill(Color.ftcRedSoft))
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(h.title).font(.ftcHeadline).foregroundStyle(Color.ftcNavy)
                                        Text(h.body).font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                                    }
                                }
                            }
                            .accessibilityElement(children: .combine)
                        }
                    }
                    .padding(.horizontal, 20)

                    howItWorks

                    VStack(alignment: .leading, spacing: 14) {
                        SectionTitle(title: "Popular cuisines", subtitle: "Tap a cuisine to start your quote")
                        LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                            ForEach(SiteContent.Cuisine.allCases) { c in
                                Button {
                                    quote.prefill(cuisine: c); router.open(.quote)
                                } label: {
                                    VStack(alignment: .leading, spacing: 8) {
                                        Image(systemName: c.symbol).font(.system(size: 18, weight: .semibold)).foregroundStyle(Color.ftcRed)
                                        Text(c.label).font(.ftcLabel).foregroundStyle(Color.ftcNavy).multilineTextAlignment(.leading)
                                        Text(c.description).font(FtcFont.poppins(11, .regular, relativeTo: .caption2)).foregroundStyle(Color.ftcMuted)
                                            .lineLimit(2).multilineTextAlignment(.leading)
                                    }
                                    .padding(14)
                                    .frame(maxWidth: .infinity, minHeight: 118, alignment: .topLeading)
                                    .background(RoundedRectangle(cornerRadius: 18).fill(.white))
                                    .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.ftcLine))
                                }
                                .accessibilityLabel("\(c.label). \(c.description). Get a quote")
                            }
                        }
                    }
                    .padding(.horizontal, 20)

                    featuredTeaser.padding(.horizontal, 20)

                    VStack(alignment: .leading, spacing: 12) {
                        SectionTitle(title: "Catering for every event")
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(SiteContent.services) { s in
                                    Button { router.open(.services) } label: {
                                        VStack(alignment: .leading, spacing: 8) {
                                            Image(s.image).resizable().scaledToFill().frame(width: 200, height: 120).clipped()
                                                .clipShape(RoundedRectangle(cornerRadius: 16))
                                            Text(s.title).font(.ftcLabel).foregroundStyle(Color.ftcNavy).lineLimit(1)
                                        }
                                        .frame(width: 200)
                                    }
                                    .accessibilityLabel(s.title)
                                }
                            }
                            .padding(.horizontal, 20)
                        }
                    }

                    FooterLinks().padding(.horizontal, 20)
                }
                .padding(.bottom, 28)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .background(Color.ftcCream)
    }

    private var greeting: String {
        if let n = sessions.session?.profile.name.split(separator: " ").first, !n.isEmpty { return "Hi \(n)!" }
        return "Australia’s food truck experts"
    }

    private var hero: some View {
        ZStack(alignment: .bottomLeading) {
            Image("photo_hero").resizable().scaledToFill().frame(height: 330).frame(maxWidth: .infinity).clipped()
                .accessibilityLabel("Food trucks at an event")
            LinearGradient(colors: [.clear, Color.ftcNavyDeep.opacity(0.92)], startPoint: .top, endPoint: .bottom)
            VStack(alignment: .leading, spacing: 14) {
                Text("FOOD TRUCK CATERING").font(FtcFont.poppins(12, .bold, relativeTo: .caption)).tracking(1.4)
                    .foregroundStyle(Color.ftcNavyDeep).padding(.horizontal, 10).padding(.vertical, 5)
                    .background(Capsule().fill(Color.ftcMustard))
                Text(SiteContent.heroTitle).font(.ftcDisplay).foregroundStyle(.white).accessibilityAddTraits(.isHeader)
                Text(SiteContent.intro3).font(.ftcCaption).foregroundStyle(.white.opacity(0.9))
                HStack(spacing: 10) {
                    Button { quote.touch(); router.open(.quote) } label: { Text("Get Free Quotes") }
                        .buttonStyle(PrimaryButtonStyle())
                        .accessibilityIdentifier("hero.getQuote")
                    Button { router.open(.services) } label: { Text("Our Services") }
                        .buttonStyle(SecondaryButtonStyle())
                }
            }
            .padding(20)
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .padding(.horizontal, 16)
        .padding(.top, 14)
    }

    private var howItWorks: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionTitle(title: "How it works")
            Image("photo_howto").resizable().scaledToFill().frame(height: 170).frame(maxWidth: .infinity).clipped()
                .clipShape(RoundedRectangle(cornerRadius: 20)).accessibilityHidden(true)
            ProcessSteps()
        }
        .padding(.horizontal, 20)
    }

    @ViewBuilder private var featuredTeaser: some View {
        if case .ready(let f?) = content.featured {
            Button { router.open(.featuring) } label: {
                HStack(spacing: 14) {
                    Image(systemName: "star.fill").foregroundStyle(Color.ftcNavyDeep).frame(width: 46, height: 46).background(Circle().fill(Color.ftcMustard))
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Featured truck").font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                        Text(f.headline ?? "See who’s featuring").font(.ftcHeadline).foregroundStyle(Color.ftcNavy).lineLimit(2).multilineTextAlignment(.leading)
                    }
                    Spacer()
                    Image(systemName: "chevron.right").foregroundStyle(Color.ftcNavy)
                }
                .padding(16)
                .background(RoundedRectangle(cornerRadius: 20).fill(Color.ftcMustardSoft))
            }
        }
    }
}

struct ProcessSteps: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ForEach(SiteContent.process) { step in
                HStack(alignment: .top, spacing: 14) {
                    Text("\(step.id)").font(.ftcHeadline).foregroundStyle(.white).frame(width: 36, height: 36).background(Circle().fill(Color.ftcNavy))
                    VStack(alignment: .leading, spacing: 3) {
                        Text(step.title).font(.ftcBodyMedium).foregroundStyle(Color.ftcNavy)
                        Text(step.body).font(.ftcCaption).foregroundStyle(Color.ftcMuted)
                    }
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Step \(step.id). \(step.title). \(step.body)")
            }
        }
    }
}

struct FooterLinks: View {
    @EnvironmentObject var router: Router
    @Environment(\.openURL) private var openURL
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Divider().padding(.bottom, 8)
            link("About Food Truck Centre", "info.circle") { router.push(.about) }
            ForEach(SiteContent.LegalPage.allCases, id: \.self) { p in
                link(p.title, "doc.text") { router.push(.legal(p)) }
            }
            link("Instagram", "camera") { openURL(SiteContent.instagramURL) }
            link("Visit our website", "safari") { openURL(SiteContent.websiteURL) }
            Text(SiteContent.copyright).font(.ftcCaption).foregroundStyle(Color.ftcMuted).padding(.top, 10)
        }
    }
    private func link(_ title: String, _ icon: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon).frame(width: 24).foregroundStyle(Color.ftcRed)
                Text(title).font(.ftcBodyMedium).foregroundStyle(Color.ftcNavy)
                Spacer()
                Image(systemName: "chevron.right").font(.system(size: 13, weight: .semibold)).foregroundStyle(Color.ftcMuted)
            }
            .frame(minHeight: 44)
        }
    }
}
