import Foundation
import Combine

/// Uygulama içi dil sistemi — ui_<lang>.json sözlüklerinden okur.
/// "auto" = cihaz dili; seçim UserDefaults'ta kalıcıdır ve anında uygulanır.
final class Loc: ObservableObject {
    static let shared = Loc()
    static let supported = ["en","tr","de","fr","es","it","pt","ru","ja","ko","zh","ar","hi","id","nl","pl"]
    static let nativeNames: [String: String] = [
        "en": "English", "tr": "Türkçe", "de": "Deutsch", "fr": "Français",
        "es": "Español", "it": "Italiano", "pt": "Português", "ru": "Русский",
        "ja": "日本語", "ko": "한국어", "zh": "中文", "ar": "العربية",
        "hi": "हिन्दी", "id": "Bahasa Indonesia", "nl": "Nederlands", "pl": "Polski"]

    @Published private(set) var lang = "en"
    private var ui: [String: Any] = [:]
    private var en: [String: Any] = [:]

    private init() { reload() }

    var overrideCode: String {
        UserDefaults.standard.string(forKey: "lang") ?? "auto"
    }

    func setOverride(_ code: String) {
        UserDefaults.standard.set(code, forKey: "lang")
        reload()
    }

    func reload() {
        let ov = overrideCode
        if ov != "auto", Loc.supported.contains(ov) {
            lang = ov
        } else {
            let code = Locale.current.language.languageCode?.identifier ?? "en"
            lang = Loc.supported.contains(code) ? code : "en"
        }
        en = Loc.load("en")
        ui = lang == "en" ? en : Loc.load(lang)
    }

    private static func load(_ code: String) -> [String: Any] {
        guard let url = Bundle.main.url(forResource: "ui_\(code)", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return [:] }
        return obj
    }

    func s(_ key: String) -> String {
        (ui[key] as? String) ?? (en[key] as? String) ?? key
    }

    func arr(_ key: String) -> [String] {
        (ui[key] as? [String]) ?? (en[key] as? [String]) ?? []
    }
}

/// Kısa erişim
func L(_ key: String) -> String { Loc.shared.s(key) }
