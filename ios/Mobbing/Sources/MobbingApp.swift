import SwiftUI
import UserNotifications

// ── Palet: mavi kurumsal Liquid Glass ──────────────────────────────────────
extension Color {
    static let navy = Color(red: 0.051, green: 0.129, blue: 0.216)       // #0D2137
    static let navyPanel = Color(red: 0.063, green: 0.157, blue: 0.247)  // #10283F
    static let ice = Color(red: 0.161, green: 0.714, blue: 0.965)        // #29B6F6
    static let iceSoft = Color(red: 0.310, green: 0.765, blue: 0.969)    // #4FC3F7
    static let steel = Color(red: 0.118, green: 0.373, blue: 0.541)      // #1E5F8A
    static let ink = Color(red: 0.918, green: 0.957, blue: 0.984)        // #EAF4FB
    static let dim = Color(red: 0.498, green: 0.631, blue: 0.737)        // #7FA1BC
}

// ── iOS 26 Liquid Glass; eski sürümlerde ultraThinMaterial ────────────────
struct Glassy: ViewModifier {
    var corner: CGFloat = 16
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.glassEffect(.regular, in: .rect(cornerRadius: corner))
        } else {
            content
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: corner))
        }
    }
}
extension View { func glassy(_ corner: CGFloat = 16) -> some View { modifier(Glassy(corner: corner)) } }

@main
struct MobbingApp: App {
    init() { Notif.requestAndSchedule() }
    var body: some Scene {
        WindowGroup { RootView().preferredColorScheme(.dark) }
    }
}

enum Screen { case menu, game, over, info }

struct RootView: View {
    @State private var screen: Screen = .menu
    @State private var showLang = false
    @StateObject private var holder = EngineHolder()
    @StateObject private var loc = Loc.shared

    var body: some View {
        ZStack {
            Color.navy.ignoresSafeArea()
            switch screen {
            case .menu: MenuView(
                onStart: { holder.newGame(); screen = .game },
                onInfo: { screen = .info },
                onLang: { showLang = true })
            case .game:
                if let e = holder.engine {
                    GameView(engine: e) { screen = .over }
                }
            case .over:
                if let e = holder.engine {
                    OverView(engine: e,
                             onRestart: { holder.newGame(); screen = .game },
                             onBribe: { screen = .game })
                }
            case .info: InfoView { screen = .menu }
            }
        }
        .sheet(isPresented: $showLang) {
            LangPicker { code in
                Loc.shared.setOverride(code)
                showLang = false
            }
            .presentationDetents([.medium, .large])
        }
    }
}

// ── Dil seçici ─────────────────────────────────────────────────────────────
struct LangPicker: View {
    let onPick: (String) -> Void

    var body: some View {
        NavigationStack {
            List {
                Button { onPick("auto") } label: {
                    Text(L("lang_auto")).foregroundStyle(Color.iceSoft).bold()
                }
                ForEach(Loc.supported, id: \.self) { code in
                    Button { onPick(code) } label: {
                        Text(Loc.nativeNames[code] ?? code).foregroundStyle(Color.ink)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.navy)
            .navigationTitle("🌐")
        }
    }
}

final class EngineHolder: ObservableObject {
    @Published var engine: GameEngine?
    func newGame() {
        engine = GameEngine(lang: Loc.shared.lang)
    }
}

// ── Yerel bildirimler — "ofis olayları" ────────────────────────────────────
enum Notif {
    static func requestAndSchedule() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            center.removeAllPendingNotificationRequests()
            var bodies = Loc.shared.arr("notif_bodies")
            if bodies.isEmpty { bodies = ["🔔"] }
            for i in 0..<5 {
                var comps = DateComponents()
                comps.hour = [11, 14, 16, 18, 19][i % 5]
                comps.minute = Int.random(in: 0..<60)
                let content = UNMutableNotificationContent()
                content.title = "MOBBING"
                content.body = bodies[i % bodies.count]
                let trig = UNCalendarNotificationTrigger(dateMatching: comps, repeats: true)
                center.add(UNNotificationRequest(identifier: "office_\(i)", content: content, trigger: trig))
            }
        }
    }
}
