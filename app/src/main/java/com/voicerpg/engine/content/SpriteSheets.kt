package com.voicerpg.engine.content

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Battle sprite sheets (docs/SPRITE_PLAN.md): a PNG sheet plus its Aseprite JSON export, in
 * `game/sprites/sheets/<spriteId>.json` (+ the PNG named by `meta.image`). Tags are animations, the
 * `feet` slice is the point that stands on the floor, and a tag's user data carries its event frames
 * (`release=3`, `impact=4`: counted within the tag, from 1). Art is drawn facing right.
 */
data class SpriteSheet(
    val id: String,
    /** Asset path of the PNG, from the assets root (e.g. `game/sprites/sheets/hero.png`). */
    val imagePath: String,
    val cellW: Int,
    val cellH: Int,
    val frames: List<SheetFrame>,
    val tags: Map<String, SheetTag>,
    val slices: Map<String, SheetSlice>
) {
    /** The point that stands on the floor: the `feet` slice's centre-x and bottom edge (or its pivot). */
    val feet: Pair<Int, Int>
        get() = slices["feet"]?.let { it.pivotX to it.pivotY } ?: (cellW / 2 to cellH)
}

/** One frame: its rectangle in the sheet, and where that (possibly trimmed) rectangle sits in the cell. */
data class SheetFrame(val x: Int, val y: Int, val w: Int, val h: Int, val offX: Int, val offY: Int, val durationMs: Int)

enum class TagDirection { FORWARD, REVERSE, PINGPONG }

/** An animation: sheet frames [from]..[to]; [events] maps an event name to its frame within the tag (0-based). */
data class SheetTag(val name: String, val from: Int, val to: Int, val direction: TagDirection, val events: Map<String, Int>)

data class SheetSlice(val name: String, val x: Int, val y: Int, val w: Int, val h: Int, val pivotX: Int, val pivotY: Int)

/** `game/sprites/sprites.json`: per-sprite settings and palette-swap variants of another sheet. */
data class SpriteSetting(
    val id: String,
    /** A variant draws this sheet... */
    val base: String? = null,
    /** ...with these colours swapped (ARGB ints). */
    val recolor: Map<Int, Int> = emptyMap(),
    /** Extra size for this sprite (a big variant), on top of the whole-number pixel scale. */
    val scale: Float = 1f
)

object AsepriteParser {

    /** Parses an Aseprite JSON export (array or hash frames). [dir] is the asset folder the PNG sits in. */
    fun parse(id: String, json: String, dir: String): SpriteSheet? = runCatching {
        val root = JsonParser.parseString(json).asJsonObject
        val meta = root.getAsJsonObject("meta") ?: JsonObject()
        val frameElements: List<JsonObject> = when (val f = root.get("frames")) {
            null -> emptyList()
            else -> if (f.isJsonArray) f.asJsonArray.map { it.asJsonObject } else f.asJsonObject.entrySet().map { it.value.asJsonObject }
        }
        val frames = frameElements.map { fo ->
            val r = fo.getAsJsonObject("frame")
            val sss = fo.getAsJsonObject("spriteSourceSize")
            SheetFrame(
                x = r.int("x"), y = r.int("y"), w = r.int("w"), h = r.int("h"),
                offX = sss?.int("x") ?: 0, offY = sss?.int("y") ?: 0,
                durationMs = fo.get("duration")?.asInt ?: 100
            )
        }
        val source = frameElements.firstOrNull()?.getAsJsonObject("sourceSize")
        val cellW = source?.int("w") ?: frames.firstOrNull()?.w ?: 0
        val cellH = source?.int("h") ?: frames.firstOrNull()?.h ?: 0
        val tags = meta.getAsJsonArray("frameTags")?.map { it.asJsonObject }?.associate { t ->
            val name = t.get("name").asString
            name to SheetTag(
                name = name,
                from = t.int("from"),
                to = t.int("to"),
                direction = when (t.get("direction")?.asString?.lowercase()) {
                    "reverse" -> TagDirection.REVERSE
                    "pingpong", "pingpong_reverse" -> TagDirection.PINGPONG
                    else -> TagDirection.FORWARD
                },
                events = parseEvents(t.get("data")?.takeIf { it.isJsonPrimitive }?.asString)
            )
        } ?: emptyMap()
        val slices = meta.getAsJsonArray("slices")?.mapNotNull { el ->
            val s = el.asJsonObject
            val key = s.getAsJsonArray("keys")?.firstOrNull()?.asJsonObject ?: return@mapNotNull null
            val b = key.getAsJsonObject("bounds")
            val pivot = key.getAsJsonObject("pivot")
            val x = b.int("x"); val y = b.int("y"); val w = b.int("w"); val h = b.int("h")
            SheetSlice(
                name = s.get("name").asString, x = x, y = y, w = w, h = h,
                pivotX = if (pivot != null) x + pivot.int("x") else x + w / 2,
                pivotY = if (pivot != null) y + pivot.int("y") else y + h
            )
        }?.associateBy { it.name } ?: emptyMap()
        val image = meta.get("image")?.asString?.substringAfterLast('/') ?: "$id.png"
        SpriteSheet(id, "$dir/$image", cellW, cellH, frames, tags, slices)
    }.getOrNull()

    /** `release=3, impact=4` (frames within the tag, from 1) → 0-based indices. */
    internal fun parseEvents(data: String?): Map<String, Int> =
        data.orEmpty().split(',', ';', ' ', '\n')
            .mapNotNull { part ->
                val (k, v) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
                val n = v.trim().toIntOrNull() ?: return@mapNotNull null
                k.trim().lowercase() to (n - 1).coerceAtLeast(0)
            }.toMap()

    private fun JsonObject.int(key: String): Int = (get(key) as JsonElement?)?.asInt ?: 0
}

/** DTOs for `game/sprites/sprites.json`. */
data class SpriteSettingDto(val base: String? = null, val palette: Map<String, String>? = null, val scale: Float? = null)
data class SpriteSettingsFileDto(val sprites: Map<String, SpriteSettingDto>? = null)

internal fun SpriteSettingDto.toSetting(id: String): SpriteSetting = SpriteSetting(
    id = id,
    base = base?.ifBlank { null },
    recolor = palette.orEmpty().mapNotNull { (from, to) ->
        val a = parseArgb(from) ?: return@mapNotNull null
        val b = parseArgb(to) ?: return@mapNotNull null
        a to b
    }.toMap(),
    scale = (scale ?: 1f).coerceIn(0.25f, 4f)
)

/** `#RRGGBB` or `#AARRGGBB` → ARGB int (opaque when no alpha is given). */
internal fun parseArgb(hex: String): Int? {
    val h = hex.trim().removePrefix("#")
    val v = h.toLongOrNull(16) ?: return null
    return when (h.length) {
        6 -> (0xFF000000L or v).toInt()
        8 -> v.toInt()
        else -> null
    }
}
