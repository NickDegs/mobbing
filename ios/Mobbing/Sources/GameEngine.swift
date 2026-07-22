import Foundation

// ---------------------------------------------------------------------------
// Veri modeli — cards_*.json ile aynı şema
// ---------------------------------------------------------------------------
struct LocText: Codable {
    let en: String
    let tr: String?
    func get(_ lang: String) -> String { lang == "tr" ? (tr ?? en) : en }
}

struct Choice: Codable {
    let t: LocText
    let fx: [Int]
    let next: String?
}

struct Card: Codable, Identifiable {
    let id: String
    let cat: String
    let ch: String
    let minB: Int?
    let t: LocText
    let l: Choice
    let r: Choice
}

struct CardFile: Codable { let cards: [Card] }

struct Meters {
    var b = 50, v = 50, e = 50, k = 50
}

enum Ending: String {
    case b0, b100, v0, v100, e0, e100, k0, k100
}

// ---------------------------------------------------------------------------
// Motor
// ---------------------------------------------------------------------------
final class GameEngine: ObservableObject {
    @Published var meters = Meters()
    @Published var day = 1
    @Published var current: Card?
    @Published var ended: Ending?

    private let lang: String
    private var allCards: [Card] = []
    private var followupIds: Set<String> = []
    private var deck: [Card] = []
    private var queue: [String] = []

    private let projects = ["Atlas", "Phoenix", "Nova", "Titan", "Orion", "Vega", "Zenith", "Delta-9"]
    private let clients = ["GlobalCorp", "Meridian AŞ", "NorthBridge", "Vertex Ltd", "OmniTrade", "BlueRock"]

    // Çeviri katmanı: loc_<lang>.json → id -> {t,l,r}
    struct LocCard: Codable { let t: String; let l: String; let r: String }
    struct LocFile: Codable { let cards: [String: LocCard] }
    private var overlay: [String: LocCard] = [:]

    init(lang: String) {
        self.lang = lang
        for name in ["cards_core", "cards_ext", "cards_ext2"] {
            if let url = Bundle.main.url(forResource: name, withExtension: "json"),
               let data = try? Data(contentsOf: url),
               let file = try? JSONDecoder().decode(CardFile.self, from: data) {
                allCards += file.cards
            }
        }
        if !["en", "tr"].contains(lang),
           let url = Bundle.main.url(forResource: "loc_\(lang)", withExtension: "json"),
           let data = try? Data(contentsOf: url),
           let file = try? JSONDecoder().decode(LocFile.self, from: data) {
            overlay = file.cards
        }
        followupIds = Set(allCards.flatMap { [$0.l.next, $0.r.next].compactMap { $0 } })
        reshuffle()
        drawNext()
    }

    private var mainPool: [Card] { allCards.filter { $0.minB == nil && !followupIds.contains($0.id) } }
    private var pressurePool: [Card] { allCards.filter { ($0.minB ?? 999) <= meters.b } }
    private func reshuffle() { deck = mainPool.shuffled() }

    private func substitute(_ s: String) -> String {
        var out = s
        if out.contains("{P}") { out = out.replacingOccurrences(of: "{P}", with: projects.randomElement()!) }
        if out.contains("{C}") { out = out.replacingOccurrences(of: "{C}", with: clients.randomElement()!) }
        if out.contains("{X}") { out = out.replacingOccurrences(of: "{X}", with: String(Int.random(in: 5...40) * 10)) }
        return out
    }

    // Çözülmüş metinler — kart çekildiğinde BİR KEZ hesaplanır (render'da rastgelelik bug'ı önlenir)
    @Published var text = ""
    @Published var lText = ""
    @Published var rText = ""

    private func resolveTexts() {
        guard let c = current else { return }
        let o = overlay[c.id]
        text = substitute(o?.t ?? c.t.get(lang))
        lText = substitute(o?.l ?? c.l.t.get(lang))
        rText = substitute(o?.r ?? c.r.t.get(lang))
    }

    private func drawNext() {
        pickNext()
        resolveTexts()
    }

    private func pickNext() {
        if !queue.isEmpty && Double.random(in: 0...1) < 0.6 {
            let id = queue.removeFirst()
            if let c = allCards.first(where: { $0.id == id }) { current = c; return }
        }
        if meters.b >= 75 && Double.random(in: 0...1) < 0.35, let c = pressurePool.randomElement() {
            current = c; return
        }
        if deck.isEmpty { reshuffle() }
        current = deck.removeLast()
    }

    func choose(left: Bool) {
        guard let c = current else { return }
        let ch = left ? c.l : c.r
        let fx = ch.fx.map { $0 == 0 ? 0 : $0 + Int.random(in: -2...2) }
        meters.b = clamp(meters.b + fx[0]); meters.v = clamp(meters.v + fx[1])
        meters.e = clamp(meters.e + fx[2]); meters.k = clamp(meters.k + fx[3])
        if let nx = ch.next, !queue.contains(nx) { queue.append(nx) }
        day += 1
        ended = checkEnd()
        if ended == nil { drawNext() }
    }

    private func clamp(_ x: Int) -> Int { max(0, min(100, x)) }

    /// Ekran görüntüsü/test modu: belirli bir sonu zorla
    func forceEnd(_ e: Ending) { ended = e }

    /// Rüşvet: seni bitiren göstergeyi güvenli bölgeye çeker, oyun kaldığı günden sürer.
    func revive() {
        guard let end = ended else { return }
        switch end {
        case .b0: meters.b = 30;  case .b100: meters.b = 70
        case .v0: meters.v = 30;  case .v100: meters.v = 70
        case .e0: meters.e = 30;  case .e100: meters.e = 70
        case .k0: meters.k = 30;  case .k100: meters.k = 70
        }
        ended = nil
        drawNext()
    }

    private func checkEnd() -> Ending? {
        if meters.b <= 0 { return .b0 };   if meters.b >= 100 { return .b100 }
        if meters.v <= 0 { return .v0 };   if meters.v >= 100 { return .v100 }
        if meters.e <= 0 { return .e0 };   if meters.e >= 100 { return .e100 }
        if meters.k <= 0 { return .k0 };   if meters.k >= 100 { return .k100 }
        return nil
    }
}
