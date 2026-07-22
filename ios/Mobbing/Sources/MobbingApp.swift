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
    @StateObject private var holder = EngineHolder()

    var body: some View {
        ZStack {
            Color.navy.ignoresSafeArea()
            switch screen {
            case .menu: MenuView(
                onStart: { holder.newGame(); screen = .game },
                onInfo: { screen = .info })
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
    }
}

final class EngineHolder: ObservableObject {
    @Published var engine: GameEngine?
    func newGame() {
        let supported = ["tr","de","fr","es","it","pt","ru","ja","ko","zh","ar","hi","id","nl","pl"]
        let code = Locale.current.language.languageCode?.identifier ?? "en"
        engine = GameEngine(lang: supported.contains(code) ? code : "en")
    }
}

// ── Yerel bildirimler — "ofis olayları" ────────────────────────────────────
enum Notif {
    static func requestAndSchedule() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            center.removeAllPendingNotificationRequests()
            let bodies = [
                String(localized: "notif_1"), String(localized: "notif_2"),
                String(localized: "notif_3"), String(localized: "notif_4"),
                String(localized: "notif_5")
            ]
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
