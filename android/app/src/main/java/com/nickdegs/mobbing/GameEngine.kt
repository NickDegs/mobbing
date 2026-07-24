package com.nickdegs.mobbing

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Veri modeli — cards_*.json ile aynı şema (DEĞİŞMEDİ, geriye uyumlu)
// ---------------------------------------------------------------------------
@Serializable data class LocText(val en: String, val tr: String? = null) {
    fun get(lang: String): String = if (lang == "tr" && tr != null) tr else en
}
@Serializable data class Choice(val t: LocText, val fx: List<Int>, val next: String? = null)
@Serializable data class Card(
    val id: String, val cat: String, val ch: String,
    val minB: Int? = null, val minD: Int? = null, val t: LocText, val l: Choice, val r: Choice
)
@Serializable data class CardFile(val cards: List<Card>)

// 5 gösterge: baskı, vicdan, ekip, kariyer, SAĞLIK (yeni)
data class Meters(var b: Int = 50, var v: Int = 50, var e: Int = 50, var k: Int = 50, var h: Int = 100)

enum class Ending(val key: String) {
    B0("end_b0"), B100("end_b100"), V0("end_v0"), V100("end_v100"),
    E0("end_e0"), E100("end_e100"), K0("end_k0"), K100("end_k100"),
    H0("end_h0"), LAWSUIT("end_lawsuit"),  // YENİ: sağlık çöküşü + dava zaferi
    CAUGHT("end_caught")                    // YENİ: kanıt toplarken yakalandın (mahvoldun)
}

// ---------------------------------------------------------------------------
// Motor
// ---------------------------------------------------------------------------
class GameEngine(context: Context, private val lang: String) {

    private val allCards: List<Card>
    private val followupIds: Set<String>
    private val queue: ArrayDeque<String> = ArrayDeque()
    private val rng = Random(System.currentTimeMillis())

    val meters = Meters()
    var day = 1; private set
    var current: Card? = null; private set

    // v2 sistemleri
    var evidence = 0; private set                    // kanıt sayacı
    var showLawsuitOffer = false; private set        // "dava aç?" özel kartı
    val relationships = mutableMapOf<String, Int>()  // karakter ilişki hafızası
    val legalTally = mutableMapOf<String, Int>()     // kategori bazlı "ezen karar" sayısı
    private var solidarityShield = 0
    private var lawsuitFloor = 0
    private var unionTriggered = false

    private val projects = listOf("Atlas", "Phoenix", "Nova", "Titan", "Orion", "Vega", "Zenith", "Delta-9")
    private val clients = listOf("GlobalCorp", "Meridian AŞ", "NorthBridge", "Vertex Ltd", "OmniTrade", "BlueRock")

    private val victimCats = setOf("SAG", "IZO", "ITB", "IS", "YOU")

    private val overlay: Map<String, LocCard>
    @Serializable data class LocCard(val t: String, val l: String, val r: String)
    @Serializable data class LocFile(val cards: Map<String, LocCard> = emptyMap())

    init {
        val json = Json { ignoreUnknownKeys = true }
        val files = listOf("cards_core.json", "cards_ext.json", "cards_ext2.json")
        val cards = mutableListOf<Card>()
        for (f in files) {
            val raw = context.assets.open(f).bufferedReader().use { it.readText() }
            cards += json.decodeFromString<CardFile>(raw).cards
        }
        allCards = cards
        followupIds = cards.flatMap { listOfNotNull(it.l.next, it.r.next) }.toSet()
        overlay = if (lang !in listOf("en", "tr")) {
            try {
                val raw = context.assets.open("loc_$lang.json").bufferedReader().use { it.readText() }
                json.decodeFromString<LocFile>(raw).cards
            } catch (_: Exception) { emptyMap() }
        } else emptyMap()
        drawNext()
    }

    private val recent = ArrayDeque<String>()
    private fun mainPool() = allCards.filter {
        it.minB == null && it.id !in followupIds && (it.minD ?: 0) <= day
    }
    private fun pressurePool() = allCards.filter { (it.minB ?: 999) <= meters.b }

    private fun substitute(s: String): String = s
        .replace("{P}", projects[rng.nextInt(projects.size)])
        .replace("{C}", clients[rng.nextInt(clients.size)])
        .replace("{X}", (rng.nextInt(5, 40) * 10).toString())

    var text = ""; private set
    var lText = ""; private set
    var rText = ""; private set

    private fun resolveTexts() {
        // Dava teklifi özel kartı — koddan gelir, JSON'da yok
        if (showLawsuitOffer) {
            text = Loc.s("lawsuit_offer_t")
            lText = Loc.s("lawsuit_offer_l")
            rText = Loc.s("lawsuit_offer_r")
            return
        }
        val c = current ?: return
        val o = overlay[c.id]
        text = substitute(o?.t ?: c.t.get(lang))
        lText = substitute(o?.l ?: c.l.t.get(lang))
        rText = substitute(o?.r ?: c.r.t.get(lang))
    }

    private fun drawNext() {
        pickNext()
        resolveTexts()
    }

    private fun pickNext() {
        // Sendika olayı: ekip güçlü + oturmuş + bir kez
        if (!unionTriggered && meters.e > 65 && day > 25) {
            val u = allCards.firstOrNull { it.id == "sendika" }
            if (u != null) { unionTriggered = true; current = u; return }
        }
        // 1) zincir kuyruğu öncelikli (%60)
        if (queue.isNotEmpty() && rng.nextFloat() < 0.6f) {
            val id = queue.removeFirst()
            current = allCards.firstOrNull { it.id == id } ?: return pickNext()
            return
        }
        // 2) baskı yüksekse "sana mobbing" destesi (%35)
        if (meters.b >= 75 && rng.nextFloat() < 0.35f) {
            val p = pressurePool()
            if (p.isNotEmpty()) { current = p[rng.nextInt(p.size)]; return }
        }
        // 3) güne uygun havuzdan çek (son 12 kart tekrarlanmaz)
        val pool = mainPool().filter { it.id !in recent }
        val pick = if (pool.isNotEmpty()) pool[rng.nextInt(pool.size)]
                   else mainPool()[rng.nextInt(mainPool().size)]
        recent.addLast(pick.id)
        if (recent.size > 12) recent.removeFirst()
        current = pick
    }

    var ended: Ending? = null; private set

    /** Seçim uygula. true dönerse oyun devam ediyor; false ise ended/lawsuitOffer dolu. */
    fun choose(left: Boolean): Boolean {
        // Dava teklifi ekranındaki seçim
        if (showLawsuitOffer) {
            showLawsuitOffer = false
            if (left) { ended = Ending.LAWSUIT; return false }        // dava aç → adalet (kanıt sağlam)
            lawsuitFloor = evidence + 8; drawNext(); return true      // devam: daha çok kanıt topla
        }
        val c = current ?: return false
        val ch = if (left) c.l else c.r
        val fx = ch.fx.map { it + if (it != 0) rng.nextInt(-2, 3) else 0 }

        meters.b = clamp(meters.b + fx[0]); meters.v = clamp(meters.v + fx[1])
        meters.e = clamp(meters.e + fx[2]); meters.k = clamp(meters.k + fx[3])

        // 2. KANIT: mağduriyet kartında direniş → belge. AĞIRLIKLI: sıradan direniş az,
        //    nitelikli/ağır ihlal (sağlık, sistematik) çok değer. Firma büyük — küçük kanıt yetmez.
        //    RİSKLİ: firma seni izliyor. Kanıt biriktikçe iz artar, yakalanabilirsin.
        if (c.cat in victimCats && fx[0] < 0) {
            val riskP = 0.015 + evidence * 0.0015
            if (rng.nextDouble() < riskP) {
                meters.b = clamp(meters.b + 15)        // gözetim/şüphe → baskı sıçraması
                if (evidence >= 30 && rng.nextDouble() < 0.3) {
                    evidence = 0
                    ended = Ending.CAUGHT              // yakalandın → belgeler imha, mahvoldun
                    return false
                }
                evidence /= 2                          // belgelerinin bir kısmı ele geçti
            } else {
                var w = evidenceWeight(c.cat)
                if (meters.e > 60) w += 1              // yanında tanıklar var
                if (fx[0] <= -4) w += 1                // güçlü, net bir karşı koyuş
                evidence += w
            }
        }

        // 3. İLİŞKİ HAFIZASI
        if (fx[2] > 0) relationships[c.ch] = (relationships[c.ch] ?: 0) + 1
        else if (fx[2] < 0) relationships[c.ch] = (relationships[c.ch] ?: 0) - 1

        // 4. GERÇEK KARŞILIK: "ezen" karar → kategori sayacı
        if (fx[0] > 0 && c.cat != "YOU") legalTally[c.cat] = (legalTally[c.cat] ?: 0) + 1

        // 5. SENDİKA kalkanı → güçlü toplu kanıt
        if (c.id == "sendika" && fx[0] < 0) { solidarityShield = 5; evidence += 4 }

        ch.next?.let { if (it !in queue) queue.addLast(it) }
        day++

        // 1. SAĞLIK: gün geçtikçe beden daha az dayanır
        val worn = day > 30
        var hd = 0
        if (meters.b > (if (worn) 55 else 65)) hd -= 3 else if (meters.b > 45) hd -= 1
        if (meters.v < 25) hd -= 2
        if (day > 45) hd -= 1                          // kronik yorgunluk
        if (fx[0] < 0 && fx[2] > 0) hd += 1
        meters.h = clamp(meters.h + hd)

        // Baskı creep gün geçtikçe hızlanır (sendika kalkanı varsa durur)
        if (solidarityShield > 0) solidarityShield--
        else {
            val creep = when { day < 7 -> 0; day < 18 -> 1; day < 35 -> 2; else -> 3 }
            meters.b = clamp(meters.b + creep)
        }

        ended = checkEnd()
        if (ended != null) return false

        // 2b. Dava teklifi: büyük firma — eşik yüksek ve gün geçtikçe daha da yükselir.
        //     Ancak yeterince AĞIR ve NİTELİKLİ kanıt biriktirdiysen teklif gelir → kazanacağın davadır.
        val threshold = maxOf(lawsuitFloor, 45 + day / 3)
        if (evidence >= threshold) {
            showLawsuitOffer = true
            resolveTexts()
            return true
        }
        drawNext()
        return true
    }

    private fun clamp(x: Int) = max(0, min(100, x))

    /** Kanıtın ağırlığı — hangi ihlal türü mahkemede ne kadar ağır basar */
    private fun evidenceWeight(cat: String): Int = when (cat) {
        "SAG" -> 3                    // sağlık ihlali / rapor reddi — en ağır belge
        "IS", "IZO", "ITB" -> 2
        else -> 1                     // YOU — tek başına zayıf
    }

    fun revive() {
        when (ended) {
            Ending.B0 -> meters.b = 30
            Ending.B100 -> meters.b = 70
            Ending.V0 -> meters.v = 30
            Ending.V100 -> meters.v = 70
            Ending.E0 -> meters.e = 30
            Ending.E100 -> meters.e = 70
            Ending.K0 -> meters.k = 30
            Ending.K100 -> meters.k = 70
            Ending.H0 -> meters.h = 40
            Ending.CAUGHT -> meters.b = 45   // rüşvetle kovulmaktan döndün, kanıt yok
            Ending.LAWSUIT -> return  // adalet sonu geri alınmaz
            null -> return
        }
        ended = null
        drawNext()
    }

    private fun checkEnd(): Ending? = when {
        meters.h <= 0 -> Ending.H0
        meters.b <= 0 -> Ending.B0;   meters.b >= 100 -> Ending.B100
        meters.v <= 0 -> Ending.V0;   meters.v >= 100 -> Ending.V100
        meters.e <= 0 -> Ending.E0;   meters.e >= 100 -> Ending.E100
        meters.k <= 0 -> Ending.K0;   meters.k >= 100 -> Ending.K100
        else -> null
    }

    // ----- Oyun sonu özetleri (UI için) -----
    val lovedCount: Int get() = relationships.values.count { it >= 3 }
    val hatedCount: Int get() = relationships.values.count { it <= -3 }

    /** Gerçek karşılık satırları */
    fun legalConsequences(): List<String> {
        val out = mutableListOf<String>()
        for (cat in listOf("SAG", "IZO", "ITB", "IS")) {
            val n = legalTally[cat] ?: 0
            if (n > 0) out.add(String.format(Loc.s("legal_${cat.lowercase()}"), n))
        }
        return out
    }
}
