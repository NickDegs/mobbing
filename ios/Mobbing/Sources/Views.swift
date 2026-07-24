import SwiftUI

// ── Ana menü ───────────────────────────────────────────────────────────────
struct MenuView: View {
    let onStart: () -> Void
    let onInfo: () -> Void
    let onLang: () -> Void

    var body: some View {
        ZStack {
            Image("menu_bg").resizable().scaledToFill().ignoresSafeArea()
            VStack {
                HStack {
                    Spacer()
                    Button(action: onLang) { Text("🌐").font(.system(size: 26)) }
                        .padding(.trailing, 16).padding(.top, 8)
                }
                Spacer()
            }
            VStack(spacing: 0) {
                Spacer()
                Text("MOBBING")
                    .font(.system(size: 46, weight: .black))
                    .tracking(10).foregroundStyle(Color.ink)
                Text(L("tagline"))
                    .font(.system(size: 13)).tracking(3)
                    .foregroundStyle(Color.dim).padding(.top, 4)
                if UserDefaults.standard.integer(forKey: "best") > 0 {
                    Text("🏆 " + String(format: L("record_fmt").replacingOccurrences(of: "%1$d", with: "%d"),
                                        UserDefaults.standard.integer(forKey: "best")))
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color.iceSoft).padding(.top, 10)
                }
                Spacer().frame(height: 48)
                GlassButton(label: L("start_shift"), action: onStart)
                GlassButton(label: L("info_corner"), action: onInfo, subtle: true)
                    .padding(.top, 14)
                Spacer()
                Text(L("menu_note"))
                    .font(.system(size: 11)).multilineTextAlignment(.center)
                    .foregroundStyle(Color.dim).padding(.bottom, 26)
            }
            .padding(.horizontal, 32)
        }
    }
}

struct GlassButton: View {
    let label: String
    let action: () -> Void
    var subtle = false

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 16, weight: .bold)).tracking(1)
                .foregroundStyle(subtle ? Color.iceSoft : Color.navy)
                .frame(maxWidth: .infinity).frame(height: 54)
        }
        .background(subtle ? AnyShapeStyle(Color.navyPanel.opacity(0.8)) : AnyShapeStyle(Color.ice))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .glassy(16)
        .frame(maxWidth: 300)
    }
}

// ── Oyun ekranı ────────────────────────────────────────────────────────────
struct GameView: View {
    @ObservedObject var engine: GameEngine
    let onEnd: () -> Void
    @State private var dragX: CGFloat = 0

    var body: some View {
        VStack(spacing: 0) {
            // Sağlık şeridi (tam genişlik) + kanıt rozeti
            HealthBar(value: engine.meters.h, evidence: engine.evidence)
                .padding(.horizontal, 26).padding(.top, 6)

            // Göstergeler
            HStack {
                MeterView(icon: "m_baski", label: "meter_b", value: engine.meters.b, fx: activeFx(0))
                Spacer()
                MeterView(icon: "m_vicdan", label: "meter_v", value: engine.meters.v, fx: activeFx(1))
                Spacer()
                MeterView(icon: "m_ekip", label: "meter_e", value: engine.meters.e, fx: activeFx(2))
                Spacer()
                MeterView(icon: "m_kariyer", label: "meter_k", value: engine.meters.k, fx: activeFx(3))
            }
            .padding(.horizontal, 26).padding(.top, 6)

            Text(String(format: L("day_fmt"), engine.day))
                .font(.system(size: 12)).tracking(3)
                .foregroundStyle(Color.dim).padding(.top, 6)

            GeometryReader { geo in
                if let card = engine.current {
                    CardView(engine: engine, card: card, dragX: $dragX,
                             lawsuit: engine.showLawsuitOffer,
                             cardW: geo.size.width * 0.94,
                             cardH: geo.size.height * 0.97) { left in
                        engine.choose(left: left)
                        if engine.ended != nil { onEnd() }
                    }
                    .frame(width: geo.size.width, height: geo.size.height)
                    .id(engine.showLawsuitOffer ? "lawsuit" : card.id)
                }
            }
            .padding(.top, 8)

            Text(L("swipe_hint"))
                .font(.system(size: 11)).tracking(1)
                .foregroundStyle(Color.dim).padding(.bottom, 6)
        }
    }

    private func activeFx(_ i: Int) -> Int? {
        guard let c = engine.current else { return nil }
        if dragX < -20 { return c.l.fx[i] }
        if dragX > 20 { return c.r.fx[i] }
        return nil
    }
}

struct MeterView: View {
    let icon: String
    let label: String
    let value: Int
    let fx: Int?

    var danger: Bool { value <= 20 || value >= 80 }
    var dangerColor: Color { Color(red: 1, green: 0.30, blue: 0.37) }

    var body: some View {
        VStack(spacing: 4) {
            Circle().fill(fx != nil && fx != 0 ? Color.iceSoft : .clear)
                .frame(width: 9, height: 9)
            Image(icon).resizable().frame(width: 48, height: 48)
            ZStack(alignment: .leading) {
                Capsule().fill(Color.steel.opacity(0.35)).frame(width: 76, height: 10)
                Capsule().fill(danger ? dangerColor : Color.ice)
                    .frame(width: 76 * CGFloat(value) / 100, height: 10)
                    .animation(.easeOut(duration: 0.5), value: value)
            }
            Text(L(label))
                .font(.system(size: 10, weight: .bold)).tracking(1)
                .foregroundStyle(danger ? dangerColor : Color.dim)
        }
    }
}

// ── Sağlık şeridi + kanıt rozeti ─────────────────────────────────────────────
struct HealthBar: View {
    let value: Int
    let evidence: Int
    private var panic: Bool { value < 30 }
    private var barColor: Color {
        if value < 30 { return Color(red: 1, green: 0.30, blue: 0.37) }
        if value < 55 { return Color(red: 1, green: 0.68, blue: 0.26) }
        return Color(red: 0.22, green: 0.85, blue: 0.54)
    }

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: panic ? "heart.slash.fill" : "heart.fill")
                .font(.system(size: 15))
                .foregroundStyle(barColor)
                .symbolEffect(.pulse, options: panic ? .repeating : .nonRepeating, isActive: panic)
            ZStack(alignment: .leading) {
                GeometryReader { g in
                    Capsule().fill(Color.steel.opacity(0.35))
                    Capsule().fill(barColor)
                        .frame(width: max(0, g.size.width * CGFloat(value) / 100))
                        .animation(.easeOut(duration: 0.5), value: value)
                }
            }
            .frame(height: 8)
            if panic {
                Text(L("health_panic"))
                    .font(.system(size: 9, weight: .heavy)).tracking(1)
                    .foregroundStyle(barColor)
            }
            if evidence > 0 {
                // Kanıt biriktikçe iz bırakırsın — rozet risk rengine döner
                let risky = evidence >= 25
                HStack(spacing: 3) {
                    Image(systemName: risky ? "folder.fill.badge.person.crop" : "folder.fill")
                        .font(.system(size: 11))
                    Text("\(evidence)").font(.system(size: 12, weight: .bold))
                }
                .foregroundStyle(risky ? Color(red: 1, green: 0.55, blue: 0.30) : Color.iceSoft)
                .padding(.horizontal, 7).padding(.vertical, 2)
                .background(Color.navyPanel.opacity(0.9), in: Capsule())
                .overlay(Capsule().stroke(risky ? Color(red: 1, green: 0.45, blue: 0.30).opacity(0.7) : .clear, lineWidth: 1))
            }
        }
    }
}

// ── Kart ───────────────────────────────────────────────────────────────────
struct CardView: View {
    @ObservedObject var engine: GameEngine
    let card: Card
    @Binding var dragX: CGFloat
    var lawsuit: Bool = false
    var cardW: CGFloat = 350
    var cardH: CGFloat = 500
    let onChoose: (Bool) -> Void

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                ZStack(alignment: .topLeading) {
                    if lawsuit {
                        ZStack {
                            LinearGradient(colors: [Color(red: 0.10, green: 0.32, blue: 0.22),
                                                    Color(red: 0.05, green: 0.16, blue: 0.13)],
                                           startPoint: .top, endPoint: .bottom)
                            Text("⚖️").font(.system(size: 88))
                        }
                        .frame(maxWidth: .infinity).frame(height: cardH * 0.52).clipped()
                        Text("⚖️ " + L("lawsuit_cat"))
                            .font(.system(size: 9, weight: .semibold)).tracking(2)
                            .foregroundStyle(Color.iceSoft)
                            .padding(.horizontal, 8).padding(.vertical, 3)
                            .background(Color.navy.opacity(0.65), in: RoundedRectangle(cornerRadius: 10))
                            .padding(10)
                    } else {
                        Image("c_\(card.ch)").resizable().scaledToFill()
                            .frame(maxWidth: .infinity).frame(height: cardH * 0.52).clipped()
                        Text(catEmoji(card.cat) + " " + catLabel(card.cat))
                            .font(.system(size: 9, weight: .semibold)).tracking(2)
                            .foregroundStyle(Color.dim)
                            .padding(.horizontal, 8).padding(.vertical, 3)
                            .background(Color.navy.opacity(0.65), in: RoundedRectangle(cornerRadius: 10))
                            .padding(10)
                    }
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text(lawsuit ? L("lawsuit_head") : charName(card.ch))
                        .font(.system(size: 12, weight: .bold)).tracking(1.5)
                        .foregroundStyle(Color.iceSoft)
                    Text(engine.text)
                        .font(.system(size: 17)).lineSpacing(5)
                        .foregroundStyle(Color.ink)
                        .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 0)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(width: cardW, height: cardH)
            .background(LinearGradient(colors: [.navyPanel, .navy], startPoint: .top, endPoint: .bottom))
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.steel.opacity(0.5), lineWidth: 1))
            .glassy(24)
            .shadow(color: .black.opacity(0.5), radius: 24, y: 14)
            .offset(x: dragX)
            .rotationEffect(.degrees(dragX / 30))
            .gesture(
                DragGesture()
                    .onChanged { dragX = $0.translation.width }
                    .onEnded { g in
                        if abs(g.translation.width) > 110 {
                            let left = g.translation.width < 0
                            withAnimation(.easeIn(duration: 0.25)) {
                                dragX = left ? -600 : 600
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.26) {
                                dragX = 0
                                onChoose(left)
                            }
                        } else {
                            withAnimation(.spring(duration: 0.3)) { dragX = 0 }
                        }
                    }
            )

            // Seçim etiketleri — kart ortasında büyük karar bandı
            if dragX < -20 {
                tag(engine.lText, green: true)
                    .position(x: cardW / 2, y: cardH / 2)
                    .opacity(min(1, abs(dragX) / 110))
            }
            if dragX > 20 {
                tag(engine.rText, green: false)
                    .position(x: cardW / 2, y: cardH / 2)
                    .opacity(min(1, dragX / 110))
            }
        }
        .frame(width: cardW, height: cardH)
    }

    private func tag(_ text: String, green: Bool) -> some View {
        let accent = green ? Color(red: 0.22, green: 0.85, blue: 0.54)
                           : Color(red: 1, green: 0.30, blue: 0.37)
        return Text(text)
            .font(.system(size: 19, weight: .black)).lineSpacing(5)
            .multilineTextAlignment(.center)
            .foregroundStyle(accent)
            .padding(.horizontal, 18).padding(.vertical, 20)
            .frame(width: cardW * 0.9)
            .background(Color.navy.opacity(0.95), in: RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(accent, lineWidth: 2.5))
    }
}

// ── Oyun sonu ──────────────────────────────────────────────────────────────
struct OverView: View {
    @ObservedObject var engine: GameEngine
    let onRestart: () -> Void
    let onBribe: () -> Void
    @StateObject private var store = BribeStore()
    @State private var showConfirm = false

    var body: some View {
        ZStack {
            Image("gameover_bg").resizable().scaledToFill().ignoresSafeArea()
            Color.navy.opacity(0.55).ignoresSafeArea()
            ScrollView {
            VStack(spacing: 16) {
                if let end = engine.ended {
                    Text(L("end_\(end.rawValue)_t"))
                        .font(.system(size: 24, weight: .black)).tracking(2)
                        .foregroundStyle(Color.iceSoft).multilineTextAlignment(.center)
                    Text(L("end_\(end.rawValue)_b"))
                        .font(.system(size: 15)).lineSpacing(5)
                        .foregroundStyle(Color.ink).multilineTextAlignment(.center)
                }
                Text(String(format: L("lasted_fmt"), engine.day))
                    .font(.system(size: 13)).foregroundStyle(Color.dim)

                // İlişki özeti — kaç kişi seni sevdi / nefret etti
                if engine.lovedCount > 0 || engine.hatedCount > 0 {
                    HStack(spacing: 18) {
                        if engine.lovedCount > 0 {
                            Label(String(format: L("summary_loved"), engine.lovedCount), systemImage: "heart.fill")
                                .foregroundStyle(Color(red: 0.22, green: 0.85, blue: 0.54))
                        }
                        if engine.hatedCount > 0 {
                            Label(String(format: L("summary_hated"), engine.hatedCount), systemImage: "bolt.fill")
                                .foregroundStyle(Color(red: 1, green: 0.30, blue: 0.37))
                        }
                    }
                    .font(.system(size: 12, weight: .semibold))
                }

                // Gerçek karşılık — verdiğin "ezen" kararların iş hukuku karşılığı
                let consequences = engine.legalConsequences()
                if !consequences.isEmpty {
                    VStack(alignment: .leading, spacing: 5) {
                        Text(L("legal_intro"))
                            .font(.system(size: 11, weight: .bold)).tracking(1)
                            .foregroundStyle(Color.iceSoft)
                        ForEach(consequences, id: \.self) { line in
                            Text("• " + line)
                                .font(.system(size: 11)).lineSpacing(2)
                                .foregroundStyle(Color.ink)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        Text(L("legal_outro"))
                            .font(.system(size: 10)).italic()
                            .foregroundStyle(Color.dim).padding(.top, 2)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(12)
                    .frame(maxWidth: 320, alignment: .leading)
                    .background(Color.navyPanel.opacity(0.6), in: RoundedRectangle(cornerRadius: 12))
                }

                // Her oyun sonunda: gerçek hayat için samimi tavsiye notu
                Text(L("end_advice"))
                    .font(.system(size: 12)).italic().lineSpacing(4)
                    .foregroundStyle(Color.iceSoft).multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, 10).padding(.vertical, 12)
                    .frame(maxWidth: 330)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.iceSoft.opacity(0.3), lineWidth: 1))

                // 💼 Rüşvet — kaldığın yerden devam (consumable IAP)
                if store.product != nil || ShotMode.mode != nil {
                    Button {
                        showConfirm = true
                    } label: {
                        VStack(spacing: 2) {
                            Text("💼 " + L("bribe_btn") + " — " + store.priceLabel)
                                .font(.system(size: 15, weight: .bold))
                            Text(L("bribe_flavor"))
                                .font(.system(size: 10)).opacity(0.8)
                        }
                        .foregroundStyle(Color.navy)
                        .frame(maxWidth: .infinity).frame(height: 58)
                    }
                    .background(Color.iceSoft)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .glassy(16)
                    .frame(maxWidth: 300)
                    .disabled(store.purchasing)
                    .padding(.top, 20)
                }

                GlassButton(label: L("restart"), action: onRestart, subtle: true)
                    .padding(.top, 6)

                // IAP yasal linkleri (App Store zorunluluğu)
                HStack(spacing: 14) {
                    Link(L("privacy_link"), destination: URL(string: "https://realvirtuality.app/mobbing/privacy.html")!)
                    Link(L("terms_link"), destination: URL(string: "https://realvirtuality.app/mobbing/terms.html")!)
                }
                .font(.system(size: 11))
                .foregroundStyle(Color.dim)
                .padding(.top, 10)
            }
            .frame(maxWidth: .infinity, minHeight: UIScreen.main.bounds.height - 40)
            .padding(36)
            }
        }
        .onAppear {
            if engine.day > UserDefaults.standard.integer(forKey: "best") {
                UserDefaults.standard.set(engine.day, forKey: "best")
            }
            if ShotMode.mode == "confirm" {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { showConfirm = true }
        } }
        .sheet(isPresented: $showConfirm) {
            VStack(spacing: 18) {
                Text("💼").font(.system(size: 52))
                Text(L("bribe_btn")).font(.system(size: 22, weight: .black))
                    .foregroundStyle(Color.ink)
                Text(L("bribe_flavor")).font(.system(size: 14))
                    .foregroundStyle(Color.dim).multilineTextAlignment(.center)
                Text(store.priceLabel).font(.system(size: 30, weight: .black))
                    .foregroundStyle(Color.iceSoft)
                GlassButton(label: L("bribe_btn") + " — " + store.priceLabel) {
                    showConfirm = false
                    Task {
                        if await store.buy() {
                            engine.revive()
                            onBribe()
                        }
                    }
                }
                // Satın alma öncesi yasal linkler (App Store kuralı)
                HStack(spacing: 16) {
                    Link(L("privacy_link"), destination: URL(string: "https://realvirtuality.app/mobbing/privacy.html")!)
                    Link(L("terms_link"), destination: URL(string: "https://realvirtuality.app/mobbing/terms.html")!)
                }
                .font(.system(size: 12))
                .foregroundStyle(Color.iceSoft)
            }
            .padding(30)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.navy)
            .presentationDetents([.medium])
        }
    }
}

// ── Bilgi köşesi ───────────────────────────────────────────────────────────
struct InfoView: View {
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(L("info_title"))
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(Color.iceSoft)
            ScrollView {
                Text(L("info_body"))
                    .font(.system(size: 14)).lineSpacing(6)
                    .foregroundStyle(Color.ink)
            }
            GlassButton(label: L("back"), action: onBack, subtle: true)
                .frame(maxWidth: .infinity)
        }
        .padding(28)
    }
}

// ── Yardımcılar ────────────────────────────────────────────────────────────
func charName(_ ch: String) -> String { L("ch_\(ch)") }
func catLabel(_ cat: String) -> String { L("cat_\(cat.lowercased())") }
func catEmoji(_ cat: String) -> String {
    switch cat {
    case "ILT": return "🗣️"; case "IZO": return "🚪"; case "ITB": return "🎭"
    case "IS": return "📋"; case "SAG": return "🩺"; case "YOU": return "🎯"
    default: return "👔"
    }
}
