# MOBBING v2 — Gerçekçilik Güncellemesi (5 sistem)

Tasarım ilkesi: 5 yeni sistem, mevcut 180 kartın fx/cat/ch verisinden OTOMATİK türetilir.
Hiçbir kart elle değiştirilmez → %100 geriye uyumlu, sıfır bozulma riski.

## 1. SAĞLIK göstergesi (5. metre "h", 0-100, başlangıç 100)
Mobbing bedeni yer. Her turda otomatik:
- Baskı>65 → sağlık -3 · Baskı>45 → -1
- Vicdan<25 → sağlık -2 (vicdan azabı uykuyu böler)
- İyi karar (fx: baskı azalt + ekip artır) → sağlık +1 (nefes)
- Sağlık<30: "panik atak" uyarısı (kırmızı)
- Sağlık<=0 → YENİ SON: "Tükenmişlik" (sağlık çöktü, istifa/hastane)

## 2. KANIT toplama → DAVA sonu (evidence 0+)
Mağduriyet kartında (cat: SAG/IZO/ITB/IS/YOU) DİRENİŞ tarafını
(fx[0]<0 = ezmeyi reddeden) seçince kanıt +1 (belge sakla).
- Kanıt>=12 → özel kart tetiklenir: "Yeterince kanıt topladın. Dava açacak mısın?"
  - Dava aç → YENİ SON: "Adalet" (davayı kazandın, tazminat) — en onurlu son
  - Devam → oyun sürer, kanıt birikmeye devam
- Üst köşede klasör ikonu + kanıt sayısı gösterilir

## 3. KARAKTER HAFIZASI (rel: [char: Int])
choose'da kartın ch karakteri için ekip etkisi ilişkiye yansır:
rel[ch] += (fx_ekip>0 ? +1 : fx_ekip<0 ? -1 : 0)
- Oyun sonu: kaç karakter seni sevdi / kaç nefret etti gösterilir
- İlişkisi çok düşen karakter (<=-3) kriz kartında sana zarar verir (baskı bonusu)
- İlişkisi çok yüksek (>=3) karakter seni kollar

## 4. GERÇEK KARŞILIK ekranı (legal tracking)
"Ezen" tarafı (fx[0]>0 = baskı artıran = mobbing uygulayan) seçilen kartların
kategorileri sayılır. Oyun sonu ekranında:
"Verdiğin kararların gerçek iş hukukundaki karşılığı:"
- SAG kararları → "iş sağlığı/güvenliği ihlali" (idari para cezası + suç)
- IZO → "sistematik dışlama = mobbing (tazminat)"
- ITB → "hakaret/itibar (manevi tazminat)"
- IS → "kötüniyet feshi/haksız uygulama"
- YOU → (oyuncuya karşı, sayılmaz)
+ gerçek mobbing istatistiği. Farkındalık amaçlı, kaynaklı.

## 5. SENDİKA/dayanışma
Ekip>65 && gün>25 → "sendika" kartı garantili tetiklenir.
Birlik seçilirse: 5 tur boyunca baskı creep durur (dayanışma kalkanı) + kanıt +2.

## Yeni son'lar (16 dilde metin gerekir):
- h0: "TÜKENDİN" (sağlık çöküşü)
- lawsuit: "ADALET" (dava zaferi — en iyi son)

## Uygulama sırası:
1. Motor: Swift + Kotlin (5 sistem, fx'ten türetme)
2. UI: 5. gösterge + kanıt rozeti + dava kartı + gerçek karşılık ekranı (iOS+Android)
3. 16 dil: yeni son metinleri + gerçek karşılık metinleri + kanıt/sağlık etiketleri
4. Build + test (CI) → submit hazır
