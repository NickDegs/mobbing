package com.nickdegs.mobbing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
        enableEdgeToEdge()
        setContent { MobbingApp() }
        Notif.scheduleDaily(this)
    }
}

enum class Screen { Menu, Game, Over, Info }

@Composable
fun MobbingApp() {
    val ctx = LocalContext.current
    val lang = remember { if (ctx.resources.configuration.locales[0].language == "tr") "tr" else "en" }
    var screen by remember { mutableStateOf(Screen.Menu) }
    var engine by remember { mutableStateOf<GameEngine?>(null) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Ice, background = Navy, surface = NavyPanel)) {
        Box(Modifier.fillMaxSize().background(Navy)) {
            when (screen) {
                Screen.Menu -> MenuScreen(
                    onStart = { engine = GameEngine(ctx, lang); screen = Screen.Game },
                    onInfo = { screen = Screen.Info })
                Screen.Game -> engine?.let { e ->
                    GameScreen(e, lang) { screen = Screen.Over }
                }
                Screen.Over -> engine?.let { e ->
                    OverScreen(e) { engine = GameEngine(ctx, lang); screen = Screen.Game }
                }
                Screen.Info -> InfoScreen { screen = Screen.Menu }
            }
        }
    }
}

// ── Ana menü ───────────────────────────────────────────────────────────────
@Composable
fun MenuScreen(onStart: () -> Unit, onInfo: () -> Unit) {
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
            Text(stringResource(R.string.tagline), color = Dim, fontSize = 13.sp,
                letterSpacing = 3.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            GlassButton(stringResource(R.string.start_shift), onStart)
            Spacer(Modifier.height(14.dp))
            GlassButton(stringResource(R.string.info_corner), onInfo, subtle = true)
        }
        Text(stringResource(R.string.menu_note), color = Dim, fontSize = 11.sp,
            textAlign = TextAlign.Center, lineHeight = 16.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp))
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
fun GameScreen(e: GameEngine, lang: String, onEnd: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }   // recompose tetikleyici
    var dragX by remember { mutableFloatStateOf(0f) }
    val card = e.current
    val screenW = LocalConfiguration.current.screenWidthDp.dp

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Göstergeler
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MeterView(R.drawable.m_baski, e.meters.b, dragX, card?.l?.fx?.get(0), card?.r?.fx?.get(0))
            MeterView(R.drawable.m_vicdan, e.meters.v, dragX, card?.l?.fx?.get(1), card?.r?.fx?.get(1))
            MeterView(R.drawable.m_ekip, e.meters.e, dragX, card?.l?.fx?.get(2), card?.r?.fx?.get(2))
            MeterView(R.drawable.m_kariyer, e.meters.k, dragX, card?.l?.fx?.get(3), card?.r?.fx?.get(3))
        }
        Text(stringResource(R.string.day_fmt, e.day), color = Dim, fontSize = 12.sp,
            letterSpacing = 3.sp, modifier = Modifier.align(Alignment.CenterHorizontally))

        // Kart
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            card?.let { c ->
                val rot by animateFloatAsState(dragX / 30f, tween(50), label = "rot")
                Box(
                    Modifier
                        .fillMaxWidth(.86f)
                        .aspectRatio(0.72f)
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
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().weight(1.1f)) {
                            Image(painterResource(charRes(c.ch)), null,
                                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Text(catLabel(c.cat), color = Dim, fontSize = 9.sp, letterSpacing = 2.sp,
                                modifier = Modifier.padding(10.dp)
                                    .background(Navy.copy(alpha = .65f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Column(Modifier.fillMaxWidth().weight(1f).padding(16.dp)) {
                            Text(charName(c.ch), color = IceSoft, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(e.cardText(c), color = Ink, fontSize = 15.sp, lineHeight = 21.sp)
                        }
                    }
                    // Seçim etiketleri
                    val p = (abs(dragX) / 260f).coerceIn(0f, 1f)
                    if (dragX < -20) ChoiceTag(e.choiceText(c.l), true, p, Modifier.align(Alignment.CenterStart))
                    if (dragX > 20) ChoiceTag(e.choiceText(c.r), false, p, Modifier.align(Alignment.CenterEnd))
                }
            }
        }
        Text(stringResource(R.string.swipe_hint), color = Dim, fontSize = 11.sp, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).navigationBarsPadding())
    }
}

@Composable
fun ChoiceTag(text: String, left: Boolean, progress: Float, modifier: Modifier) {
    Box(
        modifier.padding(10.dp).graphicsLayer { alpha = progress }
            .rotate(if (left) -4f else 4f)
            .background(NavyPanel, RoundedCornerShape(12.dp))
            .padding(10.dp).widthIn(max = 150.dp)
    ) {
        Text(text, color = if (left) Color(0xFF39D98A) else Color(0xFFFF4D5E),
            fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
    }
}

@Composable
fun MeterView(icon: Int, value: Int, dragX: Float, lFx: Int?, rFx: Int?) {
    val fx = if (dragX < -20) lFx else if (dragX > 20) rFx else null
    val danger = value <= 20 || value >= 80
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(8.dp).clip(CircleShape)
            .background(if (fx != null && fx != 0) IceSoft else Color.Transparent))
        Spacer(Modifier.height(3.dp))
        Image(painterResource(icon), null, Modifier.size(30.dp))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(46.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Steel.copy(alpha = .35f))) {
            Box(Modifier.fillMaxWidth(value / 100f).height(5.dp)
                .background(if (danger) Color(0xFFFF4D5E) else Ice, RoundedCornerShape(3.dp)))
        }
    }
}

// ── Oyun sonu ──────────────────────────────────────────────────────────────
@Composable
fun OverScreen(e: GameEngine, onRestart: () -> Unit) {
    val end = e.ended ?: return
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.gameover_bg), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Navy.copy(alpha = .55f)))
        Column(
            Modifier.fillMaxSize().padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(endTitle(end)), color = IceSoft, fontSize = 24.sp,
                fontWeight = FontWeight.Black, letterSpacing = 2.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(endBody(end)), color = Ink, fontSize = 15.sp,
                lineHeight = 22.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.lasted_fmt, e.day), color = Dim, fontSize = 13.sp)
            Spacer(Modifier.height(34.dp))
            GlassButton(stringResource(R.string.restart), onRestart)
        }
    }
}

// ── Bilgi köşesi (gerçek mobbing) ──────────────────────────────────────────
@Composable
fun InfoScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp).statusBarsPadding()) {
        Text(stringResource(R.string.info_title), color = IceSoft, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.info_body), color = Ink, fontSize = 14.sp, lineHeight = 22.sp,
            modifier = Modifier.weight(1f))
        GlassButton(stringResource(R.string.back), onBack, subtle = true)
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
@Composable fun charName(ch: String) = stringResource(when (ch) {
    "ceo" -> R.string.ch_ceo; "gm" -> R.string.ch_gm; "ik" -> R.string.ch_ik
    "tolga" -> R.string.ch_tolga; "ayse" -> R.string.ch_ayse; "mehmet" -> R.string.ch_mehmet
    "selin" -> R.string.ch_selin; "deniz" -> R.string.ch_deniz; "kerem" -> R.string.ch_kerem
    "ece" -> R.string.ch_ece; else -> R.string.ch_hekim
})
@Composable fun catLabel(cat: String) = stringResource(when (cat) {
    "ILT" -> R.string.cat_ilt; "IZO" -> R.string.cat_izo; "ITB" -> R.string.cat_itb
    "IS" -> R.string.cat_is; "SAG" -> R.string.cat_sag; "YOU" -> R.string.cat_you
    else -> R.string.cat_sys
})
fun endTitle(e: Ending) = when (e) {
    Ending.B0 -> R.string.end_b0_t; Ending.B100 -> R.string.end_b100_t
    Ending.V0 -> R.string.end_v0_t; Ending.V100 -> R.string.end_v100_t
    Ending.E0 -> R.string.end_e0_t; Ending.E100 -> R.string.end_e100_t
    Ending.K0 -> R.string.end_k0_t; Ending.K100 -> R.string.end_k100_t
}
fun endBody(e: Ending) = when (e) {
    Ending.B0 -> R.string.end_b0_b; Ending.B100 -> R.string.end_b100_b
    Ending.V0 -> R.string.end_v0_b; Ending.V100 -> R.string.end_v100_b
    Ending.E0 -> R.string.end_e0_b; Ending.E100 -> R.string.end_e100_b
    Ending.K0 -> R.string.end_k0_b; Ending.K100 -> R.string.end_k100_b
}
