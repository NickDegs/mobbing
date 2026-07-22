# MOBBING — Oyun Tasarım Dokümanı (dev2, 2026-07-22)

## Konsept
Reigns mekaniğinde (kart kaydırma) hiciv oyunu. Oyuncu orta kademe yönetici.
Üst yönetim mobbing yapmaya zorlar; yapmazsan SANA mobbing başlar. Hiciv mesajı
mekanikten çıkar: bu sistemde temiz kalmak imkansızdır.

## Bilimsel Temel — Leymann Envanteri (LIPT-45)
Kart kategorileri Leymann'ın 5 mobbing kategorisine dayanır:
1. **İletişime saldırı** — sözünü kesme, bağırma, sürekli eleştiri
2. **Sosyal izolasyon** — konuşma yasağı, ayrı oda, yok sayma
3. **İtibara saldırı** — dedikodu, alay, taklit, aşağılama
4. **İş durumuna saldırı** — anlamsız görev, işleri elinden alma, imkansız deadline
5. **Sağlığa saldırı** — ağır iş yükü, tehdit, tacize göz yumma

## Mekanik
- **Kart kaydırma:** sola = genelde "insanca davran", sağa = genelde "mobbing uygula"
  (ambiguity için bazen karışık — oyuncu ezberleyemesin)
- **4 gösterge (0–100, başlangıç 50):**
  - 🏢 BASKI (üst yönetim) — mobbing yapmazsan yükselir; 100 = kovulma/istifa sonu, 0 = "yönetimin gözdesi" karanlık sonu
  - ❤️ VİCDAN — mobbing yaptıkça düşer; 0 = "ruhunu kaybettin" sonu, 100 = "bu şirkete fazla iyisin" istifa sonu
  - 👥 EKİP — çalışan morali; 0 = toplu istifa, 100 = "ekip seni yönetime şikayet etti: fazla samimi"
  - 📈 KARİYER — 0 = kovulma, 100 = terfi → yeni sezon (daha üst kademe, daha ağır kartlar)
- **Gösterge boşalırsa DA taşarsa DA oyun biter** (Reigns kuralı) → her uçta özel son metni
- **Skor:** dayanılan gün sayısı
- **Zincir kartlar:** bazı kararlar ileride takip kartı tetikler (ör. izni reddedilen Ayşe istifa dilekçesiyle döner)
- **Baskı ≥ 80:** üst yönetim SANA mobbing kartları göndermeye başlar (oyuncunun yaşadığı taraf)

## Bildirimler (yerel)
Günde 2-3 rastgele: "🔔 Bir çalışan izin istedi", "🔔 İK seni çağırıyor", "🔔 Genel Müdür toplantı istiyor"

## Ciddi köşe
Ayarlar menüsünde "Gerçekte mobbinge mi uğruyorsun?" → haklar, kayıt tutma önerileri.
Mağaza uyumu + sosyal değer.

## Teknoloji (Barış kararı, 2026-07-22 — KESİN)
- **iOS: FULL NATIVE SwiftUI, iOS 26, Liquid Glass tasarım dili** (Capacitor DEĞİL)
- **Android: %100 Kotlin** (Jetpack Compose), en optimize şekilde
- İki taraf ayrı native codebase, bulut derleme GitHub Actions
- www/index.html prototipi = oyun mekaniği + kart verisi REFERANSI (native'e taşınacak)
- Dikey kilitli, tek el
- 8 dil (TR önce), reklam-hazır (ödül reklamı: "vicdan tazele" temalı), istisna değil → AdMob VAR
- Tasarım: koyu kurumsal gri-lacivert + asit sarısı vurgu + iOS'ta Liquid Glass malzemesi

## Durum
- [x] Konsept onayı (Barış, 2026-07-22)
- [x] Prototip (www/index.html — self-contained)
- [x] İlk kart havuzu (~50 kart, 5 Leymann kategorisi + sistem kartları)
- [ ] Barış prototip onayı
- [ ] Capacitor kurulum + repo + CI
- [ ] Kart havuzu genişletme (hedef 300+)
- [ ] 8 dil, AdMob, bildirimler, TestFlight/Play
