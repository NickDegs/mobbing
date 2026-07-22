package com.nickdegs.mobbing

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Uygulama içi dil sistemi — assets/ui_<lang>.json sözlüklerinden okur.
 * "auto" = cihaz dili; seçim SharedPreferences'ta kalıcıdır ve anında uygulanır.
 */
object Loc {
    val SUPPORTED = listOf("en","tr","de","fr","es","it","pt","ru","ja","ko","zh","ar","hi","id","nl","pl")
    val NATIVE_NAMES = mapOf(
        "en" to "English", "tr" to "Türkçe", "de" to "Deutsch", "fr" to "Français",
        "es" to "Español", "it" to "Italiano", "pt" to "Português", "ru" to "Русский",
        "ja" to "日本語", "ko" to "한국어", "zh" to "中文", "ar" to "العربية",
        "hi" to "हिन्दी", "id" to "Bahasa Indonesia", "nl" to "Nederlands", "pl" to "Polski")

    var lang: String = "en"; private set
    private var ui: Map<String, Any> = emptyMap()
    private var en: Map<String, Any> = emptyMap()

    fun overrideCode(ctx: Context): String =
        ctx.getSharedPreferences("mobbing_prefs", 0).getString("lang", "auto") ?: "auto"

    fun init(ctx: Context) {
        val ov = overrideCode(ctx)
        lang = if (ov != "auto" && ov in SUPPORTED) ov else deviceLang(ctx)
        en = load(ctx, "en")
        ui = if (lang == "en") en else load(ctx, lang)
    }

    fun setOverride(ctx: Context, code: String) {
        ctx.getSharedPreferences("mobbing_prefs", 0).edit().putString("lang", code).apply()
        init(ctx)
    }

    private fun deviceLang(ctx: Context): String {
        val l = ctx.resources.configuration.locales[0].language
        val code = if (l == "in") "id" else l
        return if (code in SUPPORTED) code else "en"
    }

    private fun load(ctx: Context, code: String): Map<String, Any> = try {
        val raw = ctx.assets.open("ui_$code.json").bufferedReader().use { it.readText() }
        val obj = Json.parseToJsonElement(raw) as JsonObject
        obj.mapValues { (_, v) ->
            when (v) {
                is JsonArray -> v.map { it.jsonPrimitive.content }
                else -> v.jsonPrimitive.content
            }
        }
    } catch (_: Exception) { emptyMap() }

    fun s(key: String): String =
        (ui[key] as? String) ?: (en[key] as? String) ?: key

    @Suppress("UNCHECKED_CAST")
    fun arr(key: String): List<String> =
        (ui[key] as? List<String>) ?: (en[key] as? List<String>) ?: emptyList()
}
