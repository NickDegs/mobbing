package com.nickdegs.mobbing

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Veri modeli — cards_*.json ile aynı şema
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

// ---------------------------------------------------------------------------
// Göstergeler
// ---------------------------------------------------------------------------
data class Meters(var b: Int = 50, var v: Int = 50, var e: Int = 50, var k: Int = 50)

enum class Ending(val key: String) {
    B0("end_b0"), B100("end_b100"), V0("end_v0"), V100("end_v100"),
    E0("end_e0"), E100("end_e100"), K0("end_k0"), K100("end_k100")
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

    // {P} proje, {C} müşteri, {X} sayı değişken havuzları
    private val projects = listOf("Atlas", "Phoenix", "Nova", "Titan", "Orion", "Vega", "Zenith", "Delta-9")
    private val clients = listOf("GlobalCorp", "Meridian AŞ", "NorthBridge", "Vertex Ltd", "OmniTrade", "BlueRock")

    // Çeviri katmanı: assets/loc_<lang>.json → id -> {t,l,r}
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

    // Gerçekçi tırmanış: kart ancak minD gününden sonra havuza girer
    private val recent = ArrayDeque<String>()
    private fun mainPool() = allCards.filter {
        it.minB == null && it.id !in followupIds && (it.minD ?: 0) <= day
    }
    private fun pressurePool() = allCards.filter { (it.minB ?: 999) <= meters.b }

    private fun substitute(s: String): String = s
        .replace("{P}", projects[rng.nextInt(projects.size)])
        .replace("{C}", clients[rng.nextInt(clients.size)])
        .replace("{X}", (rng.nextInt(5, 40) * 10).toString())

    // Çözülmüş metinler — kart çekildiğinde BİR KEZ hesaplanır (render'da rastgelelik bug'ı önlenir)
    var text = ""; private set
    var lText = ""; private set
    var rText = ""; private set

    private fun resolveTexts() {
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

    /** Seçim uygula. true dönerse oyun devam ediyor; false ise ended dolu. */
    var ended: Ending? = null; private set

    fun choose(left: Boolean): Boolean {
        val c = current ?: return false
        val ch = if (left) c.l else c.r
        // fx + hafif rastgele sapma (±2) → aynı kart bile her oyunda farklı hissettirir
        val fx = ch.fx.map { it + if (it != 0) rng.nextInt(-2, 3) else 0 }
        meters.b = clamp(meters.b + fx[0]); meters.v = clamp(meters.v + fx[1])
        meters.e = clamp(meters.e + fx[2]); meters.k = clamp(meters.k + fx[3])
        ch.next?.let { if (it !in queue) queue.addLast(it) }
        day++
        // Sistem yorulmaz: balayı bitince üst yönetim baskısı her gün kendiliğinden artar
        val creep = when { day < 10 -> 0; day < 50 -> 1; else -> 2 }
        meters.b = clamp(meters.b + creep)
        ended = checkEnd()
        if (ended != null) return false
        drawNext()
        return true
    }

    private fun clamp(x: Int) = max(0, min(100, x))

    /** Rüşvet: seni bitiren göstergeyi güvenli bölgeye çeker, oyun kaldığı günden sürer. */
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
            null -> return
        }
        ended = null
        drawNext()
    }

    private fun checkEnd(): Ending? = when {
        meters.b <= 0 -> Ending.B0;   meters.b >= 100 -> Ending.B100
        meters.v <= 0 -> Ending.V0;   meters.v >= 100 -> Ending.V100
        meters.e <= 0 -> Ending.E0;   meters.e >= 100 -> Ending.E100
        meters.k <= 0 -> Ending.K0;   meters.k >= 100 -> Ending.K100
        else -> null
    }
}
