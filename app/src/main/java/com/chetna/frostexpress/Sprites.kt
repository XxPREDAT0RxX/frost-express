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
import androidx.compose.ui.graphics.drawscope.withTransform
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
    val carFill = load("car_fill")      // light layer, tinted with the train colour
    val carLine = load("car_line")      // outlines and windows on top
    val noseFill = load("nose_fill")
    val noseLine = load("nose_line")
    val npc = load("npc_front")
    val npcSleep = load("npc_sleep")
    val stationRoof = load("station_roof")
    val item: Map<ShopItem, ImageBitmap> = ShopItem.values().mapNotNull { i -> load("item_" + i.name.lowercase())?.let { i to it } }.toMap()
    val iconShop = load("icon_shop")
    val iconGems = load("icon_gems")
    val iconBag = load("icon_bag")
    /** Rescued travellers (Survivors upgrade), each with their own look. */
    val survivors: List<ImageBitmap> = (1..4).mapNotNull { load("npc_surv$it") }
    /** A different floor in every car (tiled, 4 px per dp). */
    val floors: List<androidx.compose.ui.graphics.ShaderBrush> = (0 until 11).mapNotNull { i ->
        load("floor_$i")?.let { androidx.compose.ui.graphics.ShaderBrush(androidx.compose.ui.graphics.ImageShader(it, androidx.compose.ui.graphics.TileMode.Repeated, androidx.compose.ui.graphics.TileMode.Repeated)) }
    }
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

/** Devices that pump up and down instead of gently breathing. */
private val PUMPING = setOf(UpgradeType.BRASS_PISTONS, UpgradeType.BOGIE_SPRINGS, UpgradeType.GREASE_PUMP, UpgradeType.ENGINE, UpgradeType.BOOSTER)

/**
 * One upgrade device: its sprite with a soft idle bounce, its spinning part and its live effects.
 * ([ox],[oy]) is the top-left in pixels and [size] the side of the 64-unit sprite square.
 * Returns false if there is no sprite for this upgrade (the caller can fall back to the vector drawing).
 */
fun DrawScope.drawDeviceSprite(sp: GameSprites, type: UpgradeType, ox: Float, oy: Float, size: Float, t: Float, night: Float = 0f): Boolean {
    val base = sp.device[type] ?: return false
    val k = size / 64f
    // squash and stretch around the feet, each device on its own phase
    val phase = type.ordinal * 1.37f
    val pump = type in PUMPING
    val sy = 1f + (if (pump) 0.055f else 0.022f) * sin(t * (if (pump) 6f else 2.2f) + phase)
    val sx = 1f - (sy - 1f) * 0.6f
    withTransform({ scale(sx, sy, Offset(ox + 32f * k, oy + 57f * k)) }) {
        drawSprite(base, ox, oy, size, size)
        sp.rotor[type]?.let { img ->
            val r = DEVICE_ROTORS[type]
            if (r != null) {
                val ang = if (r[3] > 0f) sin(t * 0.9f + phase) * r[3] else t * r[2]
                rotate(ang, Offset(ox + r[0] * k, oy + r[1] * k)) { drawSprite(img, ox, oy, size, size) }
            } else {
                drawSprite(img, ox, oy, size, size)
            }
        }
    }
    DEVICE_FX[type]?.forEach { fx -> drawFx(fx, ox, oy, k, t + phase, night) }
    return true
}

private fun DrawScope.drawFx(fx: DevFx, ox: Float, oy: Float, k: Float, t: Float, night: Float) {
    fun p(x: Float, y: Float) = Offset(ox + x * k, oy + y * k)
    val col = Color(fx.color.toInt())
    val x = fx.x
    val y = fx.y
    when (fx.kind) {
        FxKind.STEAM -> for (i in 0 until 3) {
            val ph = (t * 0.55f + i / 3f) % 1f
            drawCircle(Color.White.copy(alpha = 0.8f * (1f - ph)), (2.2f + 4.5f * ph) * k, p(x + sin(ph * 6f + i) * 3f, y - ph * 20f))
            drawCircle(Color(0xFF6B4A3A).copy(alpha = 0.25f * (1f - ph)), (2.2f + 4.5f * ph) * k, p(x + sin(ph * 6f + i) * 3f, y - ph * 20f), style = Stroke(0.6f * k))
        }
        FxKind.GLOW -> {
            val a = 0.2f + 0.12f * sin(t * 3f) + 0.08f * night
            val c = p(x, y)
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = a.coerceIn(0f, 1f)), Color.Transparent), c, fx.a * k), fx.a * k, c, blendMode = BlendMode.Plus)
        }
        FxKind.RIPPLE -> for (i in 0 until 2) {
            val ph = (t * 0.6f + i / 2f) % 1f
            drawCircle(col.copy(alpha = 0.7f * (1f - ph)), (fx.a * 0.3f + fx.a * 0.7f * ph) * k, p(x, y), style = Stroke(1.6f * k))
        }
        FxKind.SPARKS -> for (i in 0 until 3) {
            val a = t * 3f + i * 2.1f
            if (sin(a * 9f) > -0.2f) {
                val ang = a * 1.7f
                val m = p(x + cos(ang) * fx.a * 0.5f + 3f * sin(a * 13f), y + sin(ang) * fx.a * 0.5f + 3f * cos(a * 11f))
                val e = p(x + cos(ang) * fx.a, y + sin(ang) * fx.a)
                drawLine(col, p(x, y), m, 1.8f * k, StrokeCap.Round)
                drawLine(col, m, e, 1.8f * k, StrokeCap.Round)
            }
        }
        FxKind.RAINBOW -> {
            val cols = listOf(Color(0xFFFF8A8A), Color(0xFFFFC371), Color(0xFFFFF176), Color(0xFF8FDBA8), Color(0xFF7FD3F0), Color(0xFFC3A6FF))
            for (i in cols.indices) drawLine(cols[i].copy(alpha = 0.75f), p(x, y), p(x + fx.a, y - 10f + i * 4f + 1.5f * sin(t * 2f + i)), 2.4f * k, StrokeCap.Round)
        }
        FxKind.NEEDLE -> {
            val ang = (PI * (0.75 + 1.5 * (0.55 + 0.1 * sin(t * 4f)))).toFloat()
            drawLine(col, p(x, y), p(x + cos(ang) * fx.a, y + sin(ang) * fx.a), 1.6f * k, StrokeCap.Round)
            drawCircle(Color(0xFF6B4A3A), 1.6f * k, p(x, y))
        }
        FxKind.HANDS -> {
            val m = t * 2.2f - 1.57f
            val h = t * 0.18f - 1.57f
            drawLine(col, p(x, y), p(x + cos(m) * fx.a, y + sin(m) * fx.a), 1.3f * k, StrokeCap.Round)
            drawLine(Color(0xFFE46A6A), p(x, y), p(x + cos(h) * fx.a * 0.6f, y + sin(h) * fx.a * 0.6f), 1.8f * k, StrokeCap.Round)
            drawCircle(col, 1.4f * k, p(x, y))
        }
        FxKind.BLINK -> if (sin(t * 5f) > 0f) {
            val c = p(x, y)
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.8f), Color.Transparent), c, fx.a * k), fx.a * k, c, blendMode = BlendMode.Plus)
        }
        FxKind.SPARKLE -> for (i in 0 until 3) {
            val ph = (t * 0.7f + i / 3f) % 1f
            val a = (sin(ph * PI.toFloat())).coerceAtLeast(0f)
            val ang = i * 2.1f + (t * 0.7f + i / 3f).toInt() * 1.3f
            val c = p(x + cos(ang) * fx.a * 0.7f, y + sin(ang) * fx.a * 0.5f)
            val r = 2.6f * a * k
            drawLine(col.copy(alpha = a), Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.1f * k, StrokeCap.Round)
            drawLine(col.copy(alpha = a), Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.1f * k, StrokeCap.Round)
        }
        FxKind.ZZZ -> for (i in 0 until 3) {
            val ph = (t * 0.4f + i / 3f) % 1f
            val c = p(x + ph * 10f, y - ph * 16f)
            val r = (1.6f + i * 0.6f) * k
            val a = 1f - ph
            val z = Path().apply { moveTo(c.x - r, c.y - r); lineTo(c.x + r, c.y - r); lineTo(c.x - r, c.y + r); lineTo(c.x + r, c.y + r) }
            drawPath(z, Color(0xFF6B4A3A).copy(alpha = a), style = Stroke(1.1f * k, cap = StrokeCap.Round))
        }
        FxKind.BUBBLES -> for (i in 0 until 3) {
            val ph = (t * 0.5f + i / 3f) % 1f
            drawCircle(col.copy(alpha = 0.9f * (1f - ph)), (0.8f + 0.6f * i) * k, p(x + sin(ph * 8f + i), y - ph * fx.a))
        }
        FxKind.WIND -> for (i in 0 until 3) {
            val ph = (t * 1.1f + i / 3f) % 1f
            val yy = y - fx.a * 0.3f + i * fx.a * 0.3f
            drawLine(col.copy(alpha = 0.8f * (1f - ph)), p(x + fx.a * 0.6f - ph * fx.a, yy), p(x + fx.a * 0.6f - ph * fx.a - 7f, yy), 1.3f * k, StrokeCap.Round)
        }
        FxKind.DRIP -> {
            val ph = (t * 0.8f) % 1f
            drawCircle(col.copy(alpha = 1f - ph * 0.5f), 1.6f * k, p(x, y - 6f + ph * 6f))
        }
        else -> {}
    }
}
