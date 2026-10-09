package com.chetna.frostexpress

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 3D-shaded sprites rendered offline by store_assets/make_sprites.py:
 * one per upgrade device (+ spinning parts), the train furniture, car walls, the worker and the station roof.
 */
class GameSprites(ctx: Context) {
    private val res = ctx.resources
    private val pkg = ctx.packageName

    private fun load(name: String): ImageBitmap? {
        val id = res.getIdentifier(name, "drawable", pkg)
        if (id == 0) return null
        val opts = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(res, id, opts)?.asImageBitmap()
    }

    val device: Map<UpgradeType, ImageBitmap> = UpgradeType.values().mapNotNull { t -> load("dev_" + t.name.lowercase())?.let { t to it } }.toMap()
    val rotor: Map<UpgradeType, ImageBitmap> = UpgradeType.values().mapNotNull { t -> load("rot_" + t.name.lowercase())?.let { t to it } }.toMap()
    val boiler = load("core_boiler")
    val coalBin = load("core_coalbin")
    val console = load("core_console")
    val bunk = load("core_bunk")
    val locker = load("core_locker")
    val heater = load("core_heater")
    val kitchen = load("core_kitchen")
    val table = load("core_table")
    val crate = load("core_crate")
    val carWalls = load("car_walls")
    val carGlass = load("car_glass")
    val nose = load("loco_nose")
    val npcBody = load("npc_body")
    val npcHat = load("npc_hat")
    val stationRoof = load("station_roof")
}

/** Loads the sprites once (can be warmed up on a background thread at start). */
object SpriteBank {
    @Volatile private var cache: GameSprites? = null
    fun get(ctx: Context): GameSprites = cache ?: synchronized(this) {
        cache ?: GameSprites(ctx.applicationContext).also { cache = it }
    }
}

@Composable
fun rememberGameSprites(): GameSprites {
    val ctx = LocalContext.current
    return remember { SpriteBank.get(ctx) }
}

/** Draws a bitmap into a rectangle given in pixels, optionally tinted (multiplied) with a colour. */
fun DrawScope.drawSprite(img: ImageBitmap, x: Float, y: Float, w: Float, h: Float, tint: Color? = null, alpha: Float = 1f) {
    drawImage(
        img,
        dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
        dstSize = IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)),
        alpha = alpha,
        colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.Modulate) },
        filterQuality = FilterQuality.Medium,
    )
}

/** A part that spins: pivot in sprite units (0..64), [speed] in degrees per second, or a swing of +-[swing] degrees. */
private class Rotor(val px: Float, val py: Float, val speed: Float, val swing: Float = 0f)

private val ROTORS = mapOf(
    UpgradeType.FLYWHEEL to Rotor(32f, 32f, 120f),
    UpgradeType.COOLING_FAN to Rotor(32f, 32f, 420f),
    UpgradeType.TURBO_VALVE to Rotor(32f, 32f, 60f),
    UpgradeType.ICE_CUTTER to Rotor(32f, 32f, 520f),
    UpgradeType.CARGO_CRANE to Rotor(20f, 40f, 0f, 30f),
    UpgradeType.COMPASS to Rotor(32f, 32f, 0f, 35f),
    UpgradeType.GOVERNOR to Rotor(32f, 32f, 170f),
    UpgradeType.ROOF_FAN to Rotor(32f, 32f, 300f),
    UpgradeType.FROST_RADAR to Rotor(32f, 32f, 100f),
    UpgradeType.POLAR_BEACON to Rotor(32f, 32f, 200f),
)

/**
 * One upgrade device: its 3D sprite, its spinning part and its live effects (steam, glow, sparks, needles).
 * ([ox],[oy]) is the top-left in pixels and [size] the side of the 64-unit sprite square.
 * Returns false if there is no sprite for this upgrade (the caller can fall back to the vector drawing).
 */
fun DrawScope.drawDeviceSprite(sp: GameSprites, type: UpgradeType, ox: Float, oy: Float, size: Float, t: Float, night: Float = 0f): Boolean {
    val base = sp.device[type] ?: return false
    drawSprite(base, ox, oy, size, size)
    val k = size / 64f
    sp.rotor[type]?.let { img ->
        val r = ROTORS[type]
        if (r != null) {
            val ang = if (r.swing > 0f) sin(t * 0.9f) * r.swing else t * r.speed
            rotate(ang, Offset(ox + r.px * k, oy + r.py * k)) { drawSprite(img, ox, oy, size, size) }
        } else {
            drawSprite(img, ox, oy, size, size)
        }
    }
    deviceFx(type, ox, oy, k, t, night)
    return true
}

private fun DrawScope.deviceFx(type: UpgradeType, ox: Float, oy: Float, k: Float, t: Float, night: Float) {
    fun p(x: Float, y: Float) = Offset(ox + x * k, oy + y * k)
    fun halo(x: Float, y: Float, r: Float, c: Color, a: Float) {
        if (a <= 0.01f) return
        val center = p(x, y)
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a.coerceIn(0f, 1f)), Color.Transparent), center, r * k), r * k, center, blendMode = BlendMode.Plus)
    }
    fun steam(x: Float, y: Float, n: Int = 3, rise: Float = 22f) {
        for (i in 0 until n) {
            val ph = (t * 0.55f + i.toFloat() / n) % 1f
            val c = p(x + sin(ph * 6f + i) * 3f, y - ph * rise)
            drawCircle(Color.White.copy(alpha = 0.55f * (1f - ph)), (2.5f + 5f * ph) * k, c)
        }
    }
    fun ripples(x: Float, y: Float, r0: Float, r1: Float, c: Color, n: Int = 2) {
        for (i in 0 until n) {
            val ph = (t * 0.6f + i.toFloat() / n) % 1f
            drawCircle(c.copy(alpha = 0.6f * (1f - ph)), (r0 + (r1 - r0) * ph) * k, p(x, y), style = Stroke(1.6f * k))
        }
    }
    fun needle(x: Float, y: Float, len: Float, ang: Float, c: Color, w: Float) {
        drawLine(c, p(x, y), p(x + cos(ang) * len, y + sin(ang) * len), w * k, StrokeCap.Round)
    }
    val flick = 0.5f + 0.5f * sin(t * 9f)
    when (type) {
        UpgradeType.STEAM_DOME -> steam(53f, 10f)
        UpgradeType.SAMOVAR -> steam(30f, 20f)
        UpgradeType.TEA_BREWER -> steam(54f, 20f, 2)
        UpgradeType.STEAM_WHISTLE -> { steam(44f, 26f, 3, 18f); ripples(44f, 32f, 13f, 22f, Color.White) }
        UpgradeType.STOVE_PIPE -> { halo(26f, 49f, 16f, Color(0xFFFF8A2B), 0.35f + 0.25f * flick); steam(60f, 26f, 3, 16f) }
        UpgradeType.HEADLAMP -> halo(32f, 32f, 34f, Color(0xFFFFF3B0), 0.3f + 0.5f * night)
        UpgradeType.LANTERN_RACK -> for (x in listOf(13f, 32f, 51f)) halo(x, 32f, 15f, Color(0xFFFFB347), 0.3f + 0.45f * night + 0.08f * sin(t * 5f + x))
        UpgradeType.CABIN_HEATER -> halo(32f, 32f, 34f, Color(0xFFFF6B3D), 0.18f + 0.14f * sin(t * 3f))
        UpgradeType.CHEFS_OVEN -> for (i in 0 until 4) halo(19f + (i % 2) * 26f, 19f + (i / 2) * 24f, 10f, Color(0xFFFF8A2B), 0.35f + 0.2f * sin(t * 8f + i))
        UpgradeType.FUEL_INJECTOR -> halo(55f, 14f, 10f, Color(0xFFFF8A2B), 0.25f + 0.25f * flick)
        UpgradeType.FROST_SHIELD -> { halo(32f, 32f, 30f, Color(0xFF9FDCFF), 0.2f + 0.15f * sin(t * 2.5f)); ripples(32f, 32f, 12f, 30f, Color(0xFFDFF3FF)) }
        UpgradeType.RADIO_MAST -> { ripples(32f, 32f, 6f, 30f, Color(0xFF6FE38B), 3); if (sin(t * 6f) > 0f) halo(32f, 32f, 12f, Color(0xFFFF4D4D), 0.6f) }
        UpgradeType.SIGNAL_BELL -> ripples(32f, 32f, 22f, 30f, Color(0xFFFFE680))
        UpgradeType.POLAR_BEACON -> halo(32f, 32f, 30f, Color(0xFF8FE9FF), 0.2f + 0.4f * night)
        UpgradeType.SPARK_COIL -> {
            for (i in 0 until 3) {
                val a = t * 3f + i * 2.1f
                if (sin(a * 9f) > -0.2f) {
                    val ang = a * 1.7f
                    val m = p(32f + cos(ang) * 14f + 3f * sin(a * 13f), 32f + sin(ang) * 14f + 3f * cos(a * 11f))
                    val e = p(32f + cos(ang) * 29f, 32f + sin(ang) * 29f)
                    drawLine(Color(0xFFFFF0A0), p(32f, 32f), m, 1.6f * k, StrokeCap.Round)
                    drawLine(Color(0xFFFFF0A0), m, e, 1.6f * k, StrokeCap.Round)
                }
            }
            halo(32f, 32f, 26f, Color(0xFFD28BEA), 0.25f)
        }
        UpgradeType.AURORA_PRISM -> {
            val cols = listOf(Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFFFFF176), Color(0xFF6FE38B), Color(0xFF4DD0E1), Color(0xFFB07CFF))
            for (i in cols.indices) drawLine(cols[i].copy(alpha = 0.55f), p(40f, 34f), p(63f, 22f + i * 5f + 1.5f * sin(t * 2f + i)), 2.2f * k, StrokeCap.Round)
        }
        UpgradeType.PRESSURE_GAUGE -> {
            val ang = (PI * (0.75 + 1.5 * (0.55 + 0.08 * sin(t * 4f)))).toFloat()
            needle(32f, 26f, 9f, ang, Color(0xFFD64545), 1.4f)
        }
        UpgradeType.POCKET_WATCH -> {
            needle(32f, 35f, 15f, t * 2.2f - 1.57f, Color(0xFF2A2118), 1.1f)
            needle(32f, 35f, 9f, t * 0.18f - 1.57f, Color(0xFFD64545), 1.6f)
        }
        UpgradeType.BOOSTER -> halo(32f, 32f, 28f, Color(0xFFFFB347), 0.25f + 0.2f * sin(t * 3f))
        UpgradeType.HEAT -> halo(30f, 32f, 22f, Color(0xFFFFB347), 0.2f + 0.2f * sin(t * 3f))
        else -> {}
    }
}
