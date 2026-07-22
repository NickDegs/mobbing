import SwiftUI

// ── Ana menü ───────────────────────────────────────────────────────────────
struct MenuView: View {
    let onStart: () -> Void
    let onInfo: () -> Void

    var body: some View {
        ZStack {
            Image("menu_bg").resizable().scaledToFill().ignoresSafeArea()
            VStack(spacing: 0) {
                Spacer()
                Text("MOBBING")
                    .font(.system(size: 46, weight: .black))
                    .tracking(10).foregroundStyle(Color.ink)
                Text(String(localized: "tagline"))
                    .font(.system(size: 13)).tracking(3)
                    .foregroundStyle(Color.dim).padding(.top, 4)
                Spacer().frame(height: 48)
                GlassButton(label: String(localized: "start_shift"), action: onStart)
                GlassButton(label: String(localized: "info_corner"), action: onInfo, subtle: true)
                    .padding(.top, 14)
                Spacer()
                Text(String(localized: "menu_note"))
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
            // Göstergeler
            HStack {
                MeterView(icon: "m_baski", value: engine.meters.b, fx: activeFx(0))
                Spacer()
                MeterView(icon: "m_vicdan", value: engine.meters.v, fx: activeFx(1))
                Spacer()
                MeterView(icon: "m_ekip", value: engine.meters.e, fx: activeFx(2))
                Spacer()
                MeterView(icon: "m_kariyer", value: engine.meters.k, fx: activeFx(3))
            }
            .padding(.horizontal, 26).padding(.top, 8)

            Text(String(format: String(localized: "day_fmt"), engine.day))
                .font(.system(size: 12)).tracking(3)
                .foregroundStyle(Color.dim).padding(.top, 6)

            Spacer()

            if let card = engine.current {
                CardView(engine: engine, card: card, dragX: $dragX) { left in
                    engine.choose(left: left)
                    if engine.ended != nil { onEnd() }
                }
            }

            Spacer()

            Text(String(localized: "swipe_hint"))
                .font(.system(size: 11)).tracking(1)
                .foregroundStyle(Color.dim).padding(.bottom, 12)
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
    let value: Int
    let fx: Int?

    var danger: Bool { value <= 20 || value >= 80 }

    var body: some View {
        VStack(spacing: 4) {
            Circle().fill(fx != nil && fx != 0 ? Color.iceSoft : .clear)
                .frame(width: 8, height: 8)
            Image(icon).resizable().frame(width: 30, height: 30)
            ZStack(alignment: .leading) {
                Capsule().fill(Color.steel.opacity(0.35)).frame(width: 46, height: 5)
                Capsule().fill(danger ? Color(red: 1, green: 0.30, blue: 0.37) : Color.ice)
                    .frame(width: 46 * CGFloat(value) / 100, height: 5)
                    .animation(.easeOut(duration: 0.5), value: value)
            }
        }
    }
}

// ── Kart ───────────────────────────────────────────────────────────────────
struct CardView: View {
    @ObservedObject var engine: GameEngine
    let card: Card
    @Binding var dragX: CGFloat
    let onChoose: (Bool) -> Void

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                ZStack(alignment: .topLeading) {
                    Image("c_\(card.ch)").resizable().scaledToFill()
                        .frame(maxWidth: .infinity).frame(height: 240).clipped()
                    Text(catEmoji(card.cat) + " " + catLabel(card.cat))
                        .font(.system(size: 9, weight: .semibold)).tracking(2)
                        .foregroundStyle(Color.dim)
                        .padding(.horizontal, 8).padding(.vertical, 3)
                        .background(Color.navy.opacity(0.65), in: RoundedRectangle(cornerRadius: 10))
                        .padding(10)
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text(charName(card.ch))
                        .font(.system(size: 12, weight: .bold)).tracking(1.5)
                        .foregroundStyle(Color.iceSoft)
                    Text(engine.cardText(card))
                        .font(.system(size: 15)).lineSpacing(4)
                        .foregroundStyle(Color.ink)
                        .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 0)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(width: 330, height: 470)
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

            // Seçim etiketleri
            if dragX < -20 {
                tag(engine.choiceText(card.l), green: true)
                    .position(x: 80, y: 235)
                    .opacity(min(1, abs(dragX) / 110))
            }
            if dragX > 20 {
                tag(engine.choiceText(card.r), green: false)
                    .position(x: 250, y: 235)
                    .opacity(min(1, dragX / 110))
            }
        }
        .frame(width: 330, height: 470)
    }

    private func tag(_ text: String, green: Bool) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .bold)).lineSpacing(2)
            .foregroundStyle(green ? Color(red: 0.22, green: 0.85, blue: 0.54)
                                   : Color(red: 1, green: 0.30, blue: 0.37))
            .padding(10)
            .frame(maxWidth: 150)
            .background(Color.navyPanel.opacity(0.95), in: RoundedRectangle(cornerRadius: 12))
            .rotationEffect(.degrees(green ? -4 : 4))
    }
}

// ── Oyun sonu ──────────────────────────────────────────────────────────────
struct OverView: View {
    @ObservedObject var engine: GameEngine
    let onRestart: () -> Void
    let onBribe: () -> Void
    @StateObject private var store = BribeStore()

    var body: some View {
        ZStack {
            Image("gameover_bg").resizable().scaledToFill().ignoresSafeArea()
            Color.navy.opacity(0.55).ignoresSafeArea()
            VStack(spacing: 16) {
                if let end = engine.ended {
                    Text(String(localized: String.LocalizationValue("end_\(end.rawValue)_t")))
                        .font(.system(size: 24, weight: .black)).tracking(2)
                        .foregroundStyle(Color.iceSoft).multilineTextAlignment(.center)
                    Text(String(localized: String.LocalizationValue("end_\(end.rawValue)_b")))
                        .font(.system(size: 15)).lineSpacing(5)
                        .foregroundStyle(Color.ink).multilineTextAlignment(.center)
                }
                Text(String(format: String(localized: "lasted_fmt"), engine.day))
                    .font(.system(size: 13)).foregroundStyle(Color.dim)

                // 💼 Rüşvet — kaldığın yerden devam (consumable IAP)
                if store.product != nil {
                    Button {
                        Task {
                            if await store.buy() {
                                engine.revive()
                                onBribe()
                            }
                        }
                    } label: {
                        VStack(spacing: 2) {
                            Text("💼 " + String(localized: "bribe_btn") + " — " + store.priceLabel)
                                .font(.system(size: 15, weight: .bold))
                            Text(String(localized: "bribe_flavor"))
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

                GlassButton(label: String(localized: "restart"), action: onRestart, subtle: true)
                    .padding(.top, 6)
            }
            .padding(36)
        }
    }
}

// ── Bilgi köşesi ───────────────────────────────────────────────────────────
struct InfoView: View {
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(String(localized: "info_title"))
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(Color.iceSoft)
            ScrollView {
                Text(String(localized: "info_body"))
                    .font(.system(size: 14)).lineSpacing(6)
                    .foregroundStyle(Color.ink)
            }
            GlassButton(label: String(localized: "back"), action: onBack, subtle: true)
                .frame(maxWidth: .infinity)
        }
        .padding(28)
    }
}

// ── Yardımcılar ────────────────────────────────────────────────────────────
func charName(_ ch: String) -> String { String(localized: String.LocalizationValue("ch_\(ch)")) }
func catLabel(_ cat: String) -> String { String(localized: String.LocalizationValue("cat_\(cat.lowercased())")) }
func catEmoji(_ cat: String) -> String {
    switch cat {
    case "ILT": return "🗣️"; case "IZO": return "🚪"; case "ITB": return "🎭"
    case "IS": return "📋"; case "SAG": return "🩺"; case "YOU": return "🎯"
    default: return "👔"
    }
}
