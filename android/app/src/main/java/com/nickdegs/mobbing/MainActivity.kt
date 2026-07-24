package com.nickdegs.mobbing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

// ── Palet: mavi kurumsal Liquid Glass ──────────────────────────────────────
val Navy = Color(0xFF0D2137)
val NavyPanel = Color(0xFF10283F)
val Ice = Color(0xFF29B6F6)
val IceSoft = Color(0xFF4FC3F7)
val Steel = Color(0xFF1E5F8A)
val Ink = Color(0xFFEAF4FB)
val Dim = Color(0xFF7FA1BC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Loc.init(this)
        enableEdgeToEdge()
        setContent { MobbingApp() }
        Notif.scheduleDaily(this)
    }
}

enum class Screen { Menu, Game, Over, Info }

@Composable
fun MobbingApp() {
    val ctx = LocalContext.current
    var langTick by remember { mutableIntStateOf(0) }
    var showLang by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(Screen.Menu) }
    var engine by remember { mutableStateOf<GameEngine?>(null) }
    val billing = remember {
        (ctx as? MainActivity)?.let { BillingManager(it).also { b -> b.connect() } }
    }

    // Rüşvet başarılı → göstergeyi kurtar, oyuna dön
    SideEffect {
        billing?.onBribeSuccess = {
            engine?.revive()
            screen = Screen.Game
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Ice, background = Navy, surface = NavyPanel)) {
        Box(Modifier.fillMaxSize().background(Navy)) {
            when (screen) {
                Screen.Menu -> MenuScreen(
                    onStart = { engine = GameEngine(ctx, Loc.lang); screen = Screen.Game },
                    onInfo = { screen = Screen.Info },
                    onLang = { showLang = true })
                Screen.Game -> engine?.let { e ->
                    GameScreen(e) { screen = Screen.Over }
                }
                Screen.Over -> engine?.let { e ->
                    OverScreen(e,
                        onRestart = { engine = GameEngine(ctx, Loc.lang); screen = Screen.Game },
                        onBribe = { billing?.buy() },
                        bribeAvailable = billing?.product != null,
                        bribePrice = billing?.priceLabel ?: "$0.99")
                }
                Screen.Info -> InfoScreen { screen = Screen.Menu }
            }
            if (showLang) LangDialog(
                onPick = { code ->
                    Loc.setOverride(ctx, code)
                    langTick++
                    showLang = false
                },
                onClose = { showLang = false })
        }
        key(langTick) {}   // dil değişince yeniden çiz
    }
}

// ── Dil seçici ─────────────────────────────────────────────────────────────
@Composable
fun LangDialog(onPick: (String) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = NavyPanel,
        confirmButton = {},
        title = { Text("🌐", fontSize = 22.sp) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn {
                item {
                    TextButton(onClick = { onPick("auto") }) {
                        Text(Loc.s("lang_auto"), color = IceSoft, fontSize = 15.sp)
                    }
                }
                items(Loc.SUPPORTED.size) { i ->
                    val code = Loc.SUPPORTED[i]
                    TextButton(onClick = { onPick(code) }) {
                        Text(Loc.NATIVE_NAMES[code] ?: code, color = Ink, fontSize = 15.sp)
                    }
                }
            }
        })
}

// ── Ana menü ───────────────────────────────────────────────────────────────
@Composable
fun MenuScreen(onStart: () -> Unit, onInfo: () -> Unit, onLang: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.menu_bg), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("MOBBING", color = Ink, fontSize = 44.sp,
                fontWeight = FontWeight.Black, letterSpacing = 10.sp)
            Text(Loc.s("tagline"), color = Dim, fontSize = 13.sp,
                letterSpacing = 3.sp, textAlign = TextAlign.Center)
            val best = LocalContext.current.getSharedPreferences("mobbing_prefs", 0).getInt("best", 0)
            if (best > 0) {
                Spacer(Modifier.height(10.dp))
                Text("🏆 " + String.format(Loc.s("record_fmt"), best), color = IceSoft,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(48.dp))
            GlassButton(Loc.s("start_shift"), onStart)
            Spacer(Modifier.height(14.dp))
            GlassButton(Loc.s("info_corner"), onInfo, subtle = true)
        }
        Text(Loc.s("menu_note"), color = Dim, fontSize = 11.sp,
            textAlign = TextAlign.Center, lineHeight = 16.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp))
        TextButton(onClick = onLang,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(10.dp)) {
            Text("🌐", fontSize = 24.sp)
        }
    }
}

@Composable
fun GlassButton(label: String, onClick: () -> Unit, subtle: Boolean = false) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (subtle) NavyPanel.copy(alpha = .8f) else Ice,
            contentColor = if (subtle) IceSoft else Navy),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(.78f).height(54.dp)
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
}

// ── Oyun ekranı ────────────────────────────────────────────────────────────
@Composable
fun GameScreen(e: GameEngine, onEnd: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }   // recompose tetikleyici
    var dragX by remember { mutableFloatStateOf(0f) }
    val card = e.current
    val screenW = LocalConfiguration.current.screenWidthDp.dp

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Sağlık şeridi (tam genişlik) + kanıt rozeti
        HealthBar(e.meters.h, e.evidence,
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 8.dp))
        // Göstergeler
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MeterView(R.drawable.m_baski, "meter_b", e.meters.b, dragX, card?.l?.fx?.get(0), card?.r?.fx?.get(0))
            MeterView(R.drawable.m_vicdan, "meter_v", e.meters.v, dragX, card?.l?.fx?.get(1), card?.r?.fx?.get(1))
            MeterView(R.drawable.m_ekip, "meter_e", e.meters.e, dragX, card?.l?.fx?.get(2), card?.r?.fx?.get(2))
            MeterView(R.drawable.m_kariyer, "meter_k", e.meters.k, dragX, card?.l?.fx?.get(3), card?.r?.fx?.get(3))
        }
        Text(String.format(Loc.s("day_fmt"), e.day), color = Dim, fontSize = 12.sp,
            letterSpacing = 3.sp, modifier = Modifier.align(Alignment.CenterHorizontally))

        // Kart
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            card?.let { c ->
                val rot by animateFloatAsState(dragX / 30f, tween(50), label = "rot")
                Box(
                    Modifier
                        .fillMaxWidth(.94f)
                        .fillMaxHeight(.97f)
                        .graphicsLayer { translationX = dragX; rotationZ = rot }
                        .pointerInput(tick) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (abs(dragX) > 260f) {
                                        val cont = e.choose(left = dragX < 0)
                                        dragX = 0f; tick++
                                        if (!cont) onEnd()
                                    } else dragX = 0f
                                },
                                onDrag = { change, amt -> change.consume(); dragX += amt.x }
                            )
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.verticalGradient(listOf(NavyPanel, Navy)))
                ) {
                    val law = e.showLawsuitOffer
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().weight(1.1f)) {
                            if (law) {
                                Box(Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(listOf(Color(0xFF1A5238), Color(0xFF0D2921)))),
                                    contentAlignment = Alignment.Center) {
                                    Text("⚖️", fontSize = 84.sp)
                                }
                                Text("⚖️ " + Loc.s("lawsuit_cat"), color = IceSoft, fontSize = 9.sp, letterSpacing = 2.sp,
                                    modifier = Modifier.padding(10.dp)
                                        .background(Navy.copy(alpha = .65f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp))
                            } else {
                                Image(painterResource(charRes(c.ch)), null,
                                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Text(catEmoji(c.cat) + " " + catLabel(c.cat), color = Dim, fontSize = 9.sp, letterSpacing = 2.sp,
                                    modifier = Modifier.padding(10.dp)
                                        .background(Navy.copy(alpha = .65f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        Column(Modifier.fillMaxWidth().weight(1f).padding(16.dp)) {
                            Text(if (law) Loc.s("lawsuit_head") else charName(c.ch), color = IceSoft, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(e.text, color = Ink, fontSize = 17.sp, lineHeight = 24.sp)
                        }
                    }
                    // Seçim etiketleri
                    val p = (abs(dragX) / 260f).coerceIn(0f, 1f)
                    if (dragX < -20) ChoiceTag(e.lText, true, p, Modifier.align(Alignment.Center))
                    if (dragX > 20) ChoiceTag(e.rText, false, p, Modifier.align(Alignment.Center))
                }
            }
        }
        Text(Loc.s("swipe_hint"), color = Dim, fontSize = 11.sp, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).navigationBarsPadding())
    }
}

@Composable
fun ChoiceTag(text: String, left: Boolean, progress: Float, modifier: Modifier) {
    val accent = if (left) Color(0xFF39D98A) else Color(0xFFFF4D5E)
    Box(
        modifier.padding(horizontal = 14.dp).graphicsLayer { alpha = progress }
            .fillMaxWidth()
            .background(Navy.copy(alpha = .95f), RoundedCornerShape(18.dp))
            .border(2.5.dp, accent, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = accent, textAlign = TextAlign.Center,
            fontSize = 19.sp, fontWeight = FontWeight.Black, lineHeight = 26.sp)
    }
}

@Composable
fun HealthBar(value: Int, evidence: Int, modifier: Modifier = Modifier) {
    val panic = value < 30
    val barColor = when {
        value < 30 -> Color(0xFFFF4D5E)
        value < 55 -> Color(0xFFFFAD42)
        else -> Color(0xFF39D98A)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (panic) "💔" else "❤️", fontSize = 15.sp)
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
            .background(Steel.copy(alpha = .35f))) {
            Box(Modifier.fillMaxWidth((value / 100f).coerceIn(0f, 1f)).fillMaxHeight()
                .background(barColor, RoundedCornerShape(4.dp)))
        }
        if (panic) Text(Loc.s("health_panic"), color = barColor,
            fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        if (evidence > 0) {
            // Kanıt biriktikçe iz bırakırsın — rozet risk rengine döner
            val risky = evidence >= 25
            val badge = if (risky) Color(0xFFFF8C4D) else IceSoft
            Row(
                Modifier.background(NavyPanel.copy(alpha = .9f), RoundedCornerShape(50))
                    .then(if (risky) Modifier.border(1.dp, Color(0xFFFF734D).copy(alpha = .7f), RoundedCornerShape(50)) else Modifier)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(if (risky) "🕵️" else "📁", fontSize = 11.sp)
                Text("$evidence", color = badge, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MeterView(icon: Int, label: String, value: Int, dragX: Float, lFx: Int?, rFx: Int?) {
    val fx = if (dragX < -20) lFx else if (dragX > 20) rFx else null
    val danger = value <= 20 || value >= 80
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(9.dp).clip(CircleShape)
            .background(if (fx != null && fx != 0) IceSoft else Color.Transparent))
        Spacer(Modifier.height(3.dp))
        Image(painterResource(icon), null, Modifier.size(48.dp))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(76.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Steel.copy(alpha = .35f))) {
            Box(Modifier.fillMaxWidth(value / 100f).height(10.dp)
                .background(if (danger) Color(0xFFFF4D5E) else Ice, RoundedCornerShape(5.dp)))
        }
        Spacer(Modifier.height(3.dp))
        Text(Loc.s(label), color = if (danger) Color(0xFFFF4D5E) else Dim,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// ── Oyun sonu ──────────────────────────────────────────────────────────────
@Composable
fun OverScreen(
    e: GameEngine,
    onRestart: () -> Unit,
    onBribe: () -> Unit,
    bribeAvailable: Boolean,
    bribePrice: String
) {
    val end = e.ended ?: return
    var showConfirm by remember { mutableStateOf(false) }
    val prefs = LocalContext.current.getSharedPreferences("mobbing_prefs", 0)
    LaunchedEffect(Unit) {
        if (e.day > prefs.getInt("best", 0)) prefs.edit().putInt("best", e.day).apply()
    }
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.gameover_bg), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Navy.copy(alpha = .55f)))
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 36.dp, vertical = 48.dp)
                .statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(Loc.s(endTitle(end)), color = IceSoft, fontSize = 24.sp,
                fontWeight = FontWeight.Black, letterSpacing = 2.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(Loc.s(endBody(end)), color = Ink, fontSize = 15.sp,
                lineHeight = 22.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(String.format(Loc.s("lasted_fmt"), e.day), color = Dim, fontSize = 13.sp)

            // İlişki özeti — kaç kişi seni sevdi / nefret etti
            if (e.lovedCount > 0 || e.hatedCount > 0) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (e.lovedCount > 0) Text("❤️ " + String.format(Loc.s("summary_loved"), e.lovedCount),
                        color = Color(0xFF39D98A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (e.hatedCount > 0) Text("⚡ " + String.format(Loc.s("summary_hated"), e.hatedCount),
                        color = Color(0xFFFF4D5E), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Gerçek karşılık — verdiğin "ezen" kararların iş hukuku karşılığı
            val consequences = e.legalConsequences()
            if (consequences.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth(.86f)
                        .background(NavyPanel.copy(alpha = .6f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(Loc.s("legal_intro"), color = IceSoft, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(5.dp))
                    consequences.forEach { line ->
                        Text("• $line", color = Ink, fontSize = 11.sp, lineHeight = 15.sp)
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(Loc.s("legal_outro"), color = Dim, fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }

            Spacer(Modifier.height(28.dp))
            // 💼 Rüşvet — kaldığın yerden devam
            if (bribeAvailable) {
                Button(
                    onClick = { showConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = IceSoft, contentColor = Navy),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(.78f).height(58.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💼 " + Loc.s("bribe_btn") + " — " + bribePrice,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(Loc.s("bribe_flavor"), fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (showConfirm) {
                val uri2 = androidx.compose.ui.platform.LocalUriHandler.current
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    containerColor = NavyPanel,
                    confirmButton = {
                        Button(
                            onClick = { showConfirm = false; onBribe() },
                            colors = ButtonDefaults.buttonColors(containerColor = IceSoft, contentColor = Navy)
                        ) { Text(Loc.s("bribe_btn") + " — " + bribePrice, fontWeight = FontWeight.Bold) }
                    },
                    title = { Text("💼 " + Loc.s("bribe_btn"), color = Ink, fontWeight = FontWeight.Black) },
                    text = {
                        Column {
                            Text(Loc.s("bribe_flavor"), color = Dim, fontSize = 13.sp)
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(Loc.s("privacy_link"), color = IceSoft, fontSize = 12.sp,
                                    modifier = Modifier.clickable { uri2.openUri("https://realvirtuality.app/mobbing/privacy.html") })
                                Text(Loc.s("terms_link"), color = IceSoft, fontSize = 12.sp,
                                    modifier = Modifier.clickable { uri2.openUri("https://realvirtuality.app/mobbing/terms.html") })
                            }
                        }
                    })
            }
            GlassButton(Loc.s("restart"), onRestart, subtle = true)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val uri = androidx.compose.ui.platform.LocalUriHandler.current
                Text(Loc.s("privacy_link"), color = Dim, fontSize = 11.sp,
                    modifier = Modifier.clickable { uri.openUri("https://realvirtuality.app/mobbing/privacy.html") })
                Text(Loc.s("terms_link"), color = Dim, fontSize = 11.sp,
                    modifier = Modifier.clickable { uri.openUri("https://realvirtuality.app/mobbing/terms.html") })
            }
        }
    }
}

// ── Bilgi köşesi (gerçek mobbing) ──────────────────────────────────────────
@Composable
fun InfoScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp).statusBarsPadding()) {
        Text(Loc.s("info_title"), color = IceSoft, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(Loc.s("info_body"), color = Ink, fontSize = 14.sp, lineHeight = 22.sp,
            modifier = Modifier.weight(1f))
        GlassButton(Loc.s("back"), onBack, subtle = true)
        Spacer(Modifier.height(20.dp))
    }
}

// ── Yardımcılar ────────────────────────────────────────────────────────────
fun charRes(ch: String) = when (ch) {
    "ceo" -> R.drawable.c_ceo; "gm" -> R.drawable.c_gm; "ik" -> R.drawable.c_ik
    "tolga" -> R.drawable.c_tolga; "ayse" -> R.drawable.c_ayse; "mehmet" -> R.drawable.c_mehmet
    "selin" -> R.drawable.c_selin; "deniz" -> R.drawable.c_deniz; "kerem" -> R.drawable.c_kerem
    "ece" -> R.drawable.c_ece; else -> R.drawable.c_hekim
}
fun charName(ch: String) = Loc.s("ch_$ch")
fun catEmoji(cat: String) = when (cat) {
    "ILT" -> "\uD83D\uDDE3\uFE0F"; "IZO" -> "\uD83D\uDEAA"; "ITB" -> "\uD83C\uDFAD"
    "IS" -> "\uD83D\uDCCB"; "SAG" -> "\uD83E\uDE7A"; "YOU" -> "\uD83C\uDFAF"; else -> "\uD83D\uDC54"
}
fun catLabel(cat: String) = Loc.s("cat_" + cat.lowercase())
fun endTitle(e: Ending) = e.key + "_t"
fun endBody(e: Ending) = e.key + "_b"
