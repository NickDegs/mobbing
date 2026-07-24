import Foundation

// ---------------------------------------------------------------------------
// Veri modeli — cards_*.json ile aynı şema (DEĞİŞMEDİ, geriye uyumlu)
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
    let minD: Int?
    let t: LocText
    let l: Choice
    let r: Choice
}

struct CardFile: Codable { let cards: [Card] }

// 5 gösterge: baskı, vicdan, ekip, kariyer, SAĞLIK (yeni)
struct Meters {
    var b = 50, v = 50, e = 50, k = 50, h = 100
}

enum Ending: String {
    case b0, b100, v0, v100, e0, e100, k0, k100
    case h0        // YENİ: sağlık çöküşü (tükenmişlik)
    case lawsuit   // YENİ: dava zaferi (adalet)
}

// ---------------------------------------------------------------------------
// Motor
// ---------------------------------------------------------------------------
final class GameEngine: ObservableObject {
    @Published var meters = Meters()
    @Published var day = 1
    @Published var current: Card?
    @Published var ended: Ending?

    // v2 sistemleri
    @Published var evidence = 0                 // kanıt sayacı
    @Published var showLawsuitOffer = false     // "dava aç?" özel kartı gösteriliyor mu
    private(set) var relationships: [String: Int] = [:]   // karakter ilişki hafızası
    private(set) var legalTally: [String: Int] = [:]      // kategori bazlı "ezen karar" sayısı
    private var solidarityShield = 0            // sendika kalkanı (tur sayısı)
    private var lawsuitFloor = 0                 // dava teklifi eşik tabanı (reddedilince yükselir)
    private var unionTriggered = false          // sendika olayı bir kez

    private let lang: String
    private var allCards: [Card] = []
    private var followupIds: Set<String> = []
    private var queue: [String] = []

    private let projects = ["Atlas", "Phoenix", "Nova", "Titan", "Orion", "Vega", "Zenith", "Delta-9"]
    private let clients = ["GlobalCorp", "Meridian AŞ", "NorthBridge", "Vertex Ltd", "OmniTrade", "BlueRock"]

    // Çeviri katmanı
    struct LocCard: Codable { let t: String; let l: String; let r: String }
    struct LocFile: Codable { let cards: [String: LocCard] }
    private var overlay: [String: LocCard] = [:]

    // Mağduriyet kategorileri (kanıt/gerçek karşılık için)
    private let victimCats: Set<String> = ["SAG", "IZO", "ITB", "IS", "YOU"]

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
        drawNext()
    }

    // ----- Kart havuzu -----
    private var recent: [String] = []
    private var mainPool: [Card] { allCards.filter {
        $0.minB == nil && !followupIds.contains($0.id) && ($0.minD ?? 0) <= day
    } }
    private var pressurePool: [Card] { allCards.filter { ($0.minB ?? 999) <= meters.b } }

    private func substitute(_ s: String) -> String {
        var out = s
        if out.contains("{P}") { out = out.replacingOccurrences(of: "{P}", with: projects.randomElement()!) }
        if out.contains("{C}") { out = out.replacingOccurrences(of: "{C}", with: clients.randomElement()!) }
        if out.contains("{X}") { out = out.replacingOccurrences(of: "{X}", with: String(Int.random(in: 5...40) * 10)) }
        return out
    }

    @Published var text = ""
    @Published var lText = ""
    @Published var rText = ""

    private func resolveTexts() {
        // Dava teklifi özel kartı — koddan gelir, JSON'da yok
        if showLawsuitOffer {
            text = L("lawsuit_offer_t")
            lText = L("lawsuit_offer_l")
            rText = L("lawsuit_offer_r")
            return
        }
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
        // Sendika olayı: ekip güçlü + oturmuş + bir kez
        if !unionTriggered && meters.e > 65 && day > 25,
           let u = allCards.first(where: { $0.id == "sendika" }) {
            unionTriggered = true; current = u; return
        }
        if !queue.isEmpty && Double.random(in: 0...1) < 0.6 {
            let id = queue.removeFirst()
            if let c = allCards.first(where: { $0.id == id }) { current = c; return }
        }
        if meters.b >= 75 && Double.random(in: 0...1) < 0.35, let c = pressurePool.randomElement() {
            current = c; return
        }
        let pool = mainPool.filter { !recent.contains($0.id) }
        let pick = pool.randomElement() ?? mainPool.randomElement()!
        recent.append(pick.id)
        if recent.count > 12 { recent.removeFirst() }
        current = pick
    }

    // ----- Seçim -----
    func choose(left: Bool) {
        // Dava teklifi ekranındaki seçim
        if showLawsuitOffer {
            showLawsuitOffer = false
            if left { ended = .lawsuit }               // dava aç → adalet sonu (kanıt yeterince güçlü)
            else { lawsuitFloor = evidence + 8; drawNext() }  // devam: dava için daha çok kanıt topla
            return
        }
        guard let c = current else { return }
        let ch = left ? c.l : c.r
        let fx = ch.fx.map { $0 == 0 ? 0 : $0 + Int.random(in: -2...2) }

        meters.b = clamp(meters.b + fx[0]); meters.v = clamp(meters.v + fx[1])
        meters.e = clamp(meters.e + fx[2]); meters.k = clamp(meters.k + fx[3])

        // 2. KANIT: mağduriyet kartında direniş → belge. AĞIRLIKLI: sıradan direniş az,
        //    nitelikli/ağır ihlal (sağlık, sistematik) çok değer. Firma büyük — küçük kanıt yetmez.
        if victimCats.contains(c.cat) && fx[0] < 0 {
            var w = evidenceWeight(c.cat)
            if meters.e > 60 { w += 1 }                // yanında tanıklar var
            if fx[0] <= -4 { w += 1 }                  // güçlü, net bir karşı koyuş
            evidence += w
        }

        // 3. İLİŞKİ HAFIZASI: karaktere yönelik ekip etkisi
        if fx[2] > 0 { relationships[c.ch, default: 0] += 1 }
        else if fx[2] < 0 { relationships[c.ch, default: 0] -= 1 }

        // 4. GERÇEK KARŞILIK: "ezen" (baskı artıran) karar → kategori sayacı
        if fx[0] > 0 && c.cat != "YOU" { legalTally[c.cat, default: 0] += 1 }

        // 5. SENDİKA kalkanı: sendika kartında birlik (baskı düşür) seçilirse → güçlü toplu kanıt
        if c.id == "sendika" && fx[0] < 0 { solidarityShield = 5; evidence += 4 }

        if let nx = ch.next, !queue.contains(nx) { queue.append(nx) }
        day += 1

        // 1. SAĞLIK: baskı ve vicdana bağlı otomatik erime — gün geçtikçe beden daha az dayanır
        let worn = day > 30                            // uzun süre = yıpranmış eşik
        var hd = 0
        if meters.b > (worn ? 55 : 65) { hd -= 3 } else if meters.b > 45 { hd -= 1 }
        if meters.v < 25 { hd -= 2 }
        if day > 45 { hd -= 1 }                        // kronik yorgunluk
        if fx[0] < 0 && fx[2] > 0 { hd += 1 }          // insani karar = nefes
        meters.h = clamp(meters.h + hd)

        // Sistem yorulmaz: baskı creep gün geçtikçe hızlanır (sendika kalkanı varsa durur)
        if solidarityShield > 0 { solidarityShield -= 1 }
        else {
            let creep = day < 7 ? 0 : (day < 18 ? 1 : (day < 35 ? 2 : 3))
            meters.b = clamp(meters.b + creep)
        }

        // 2b. Dava teklifi: eşik gün geçtikçe yükselir (firma avukatları güçlenir).
        //     Ancak yeterince SAĞLAM kanıt biriktirdiysen teklif gelir → kazanacağın davadır.
        ended = checkEnd()
        if ended != nil { return }
        // Büyük firma: dava eşiği yüksek ve gün geçtikçe daha da yükselir (firma avukatları güçlenir).
        // Ancak yeterince AĞIR ve NİTELİKLİ kanıt biriktirdiysen teklif gelir — kazanacağın davadır.
        let threshold = max(lawsuitFloor, 55 + day / 3)
        if evidence >= threshold {
            showLawsuitOffer = true
            resolveTexts()
            return
        }
        drawNext()
    }

    private func clamp(_ x: Int) -> Int { max(0, min(100, x)) }

    /// Kanıtın ağırlığı — hangi ihlal türü mahkemede ne kadar ağır basar
    private func evidenceWeight(_ cat: String) -> Int {
        switch cat {
        case "SAG": return 3          // sağlık ihlali / rapor reddi — en ağır belge
        case "IS", "IZO", "ITB": return 2
        default: return 1             // YOU (sana yönelik) — tek başına zayıf
        }
    }

    func forceEnd(_ e: Ending) { ended = e }

    /// Rüşvet: seni bitiren göstergeyi güvenli bölgeye çeker
    func revive() {
        guard let end = ended else { return }
        switch end {
        case .b0: meters.b = 30;  case .b100: meters.b = 70
        case .v0: meters.v = 30;  case .v100: meters.v = 70
        case .e0: meters.e = 30;  case .e100: meters.e = 70
        case .k0: meters.k = 30;  case .k100: meters.k = 70
        case .h0: meters.h = 40
        case .lawsuit: return   // adalet sonu geri alınmaz
        }
        ended = nil
        drawNext()
    }

    private func checkEnd() -> Ending? {
        if meters.h <= 0 { return .h0 }
        if meters.b <= 0 { return .b0 };   if meters.b >= 100 { return .b100 }
        if meters.v <= 0 { return .v0 };   if meters.v >= 100 { return .v100 }
        if meters.e <= 0 { return .e0 };   if meters.e >= 100 { return .e100 }
        if meters.k <= 0 { return .k0 };   if meters.k >= 100 { return .k100 }
        return nil
    }

    // ----- Oyun sonu özetleri (UI için) -----
    /// Sevilen/nefret edilen karakter sayısı
    var lovedCount: Int { relationships.values.filter { $0 >= 3 }.count }
    var hatedCount: Int { relationships.values.filter { $0 <= -3 }.count }

    /// Gerçek karşılık satırları — verilen "ezen" kararların iş hukuku karşılığı
    func legalConsequences() -> [String] {
        var out: [String] = []
        let order = ["SAG", "IZO", "ITB", "IS"]
        for cat in order {
            if let n = legalTally[cat], n > 0 {
                out.append(String(format: L("legal_\(cat.lowercased())"), n))
            }
        }
        return out
    }
}
