package com.chetna.frostexpress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val STEEL = Color(0xFF8A949E)
private val LSTEEL = Color(0xFFC8CED4)
private val DSTEEL = Color(0xFF5E6873)
private val IRON = Color(0xFF3A3F47)
private val BRASS = Color(0xFFD9A441)
private val COPPER = Color(0xFFC1703A)
private val WOODC = Color(0xFF8B5E3C)
private val LWOOD = Color(0xFFB98B5E)
private val REDC = Color(0xFFE5484D)
private val BLUEC = Color(0xFF3B8EDC)
private val ICEC = Color(0xFF9FD8F0)
private val GREENC = Color(0xFF3FAE5A)
private val CREAMC = Color(0xFFF6F0E4)
private val FIRE = Color(0xFFFF8A2B)

/** Upgrades drawn as overhead (ceiling or wall mounted) devices; the others stand on the floor. */
val OVERHEAD_TYPES = setOf(
    UpgradeType.STEAM_WHISTLE, UpgradeType.HEADLAMP, UpgradeType.ROOF_FAN, UpgradeType.RADIO_MAST,
    UpgradeType.POCKET_WATCH, UpgradeType.LANTERN_RACK, UpgradeType.SIGNAL_BELL, UpgradeType.FROST_RADAR,
    UpgradeType.NIGHT_WATCH, UpgradeType.TIMETABLE, UpgradeType.POLAR_BEACON,
)

/**
 * One hand-drawn top-down model for every generic upgrade, in the unit box starting at ([ox],[oy]) with side [s].
 * [t] is time in seconds, [lv] the upgrade level, [flash] 1..0 right after buying, [night] 0..1 for lamp glow.
 */
fun DrawScope.drawDevice(type: UpgradeType, ox: Float, oy: Float, s: Float, t: Float, lv: Int, flash: Float = 0f, night: Float = 0f) {
    fun rb(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 0.05f) =
        drawRoundRect(c, Offset(ox + x * s, oy + y * s), Size(w * s, h * s), CornerRadius(r * s, r * s))
    fun ci(c: Color, x: Float, y: Float, r: Float) = drawCircle(c, r * s, Offset(ox + x * s, oy + y * s))
    fun rg(c: Color, x: Float, y: Float, r: Float, w: Float) =
        drawCircle(c, r * s, Offset(ox + x * s, oy + y * s), style = Stroke(w * s))
    fun ln(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float) =
        drawLine(c, Offset(ox + x1 * s, oy + y1 * s), Offset(ox + x2 * s, oy + y2 * s), w * s, StrokeCap.Round)
    fun ov(c: Color, x: Float, y: Float, w: Float, h: Float) =
        drawOval(c, Offset(ox + x * s, oy + y * s), Size(w * s, h * s))
    fun halo(c: Color, x: Float, y: Float, r: Float, a: Float) {
        if (a <= 0.01f) return
        val center = Offset(ox + x * s, oy + y * s)
        drawCircle(
            Brush.radialGradient(listOf(c.copy(alpha = a.coerceAtMost(1f)), Color.Transparent), center, r * s),
            r * s, center, blendMode = BlendMode.Plus,
        )
    }
    fun spin(deg: Float, px: Float, py: Float, block: DrawScope.() -> Unit) =
        rotate(deg, Offset(ox + px * s, oy + py * s), block)
    fun frac(i: Int, n: Int, speed: Float) = (t * speed + i.toFloat() / n) % 1f
    val k = lv.coerceAtLeast(1)

    when (type) {
        // ---------------- floor devices ----------------
        UpgradeType.SNOW_CHAINS -> {
            ci(IRON, .5f, .5f, .42f); ci(Color(0xFF23262B), .5f, .5f, .3f)
            for (i in 0 until 14) {
                val a = i * 0.4488f + t * 0.9f
                ci(if (i % 2 == 0) STEEL else LSTEEL, .5f + cos(a) * .4f, .5f + sin(a) * .4f, .04f)
            }
            for (i in 0 until 6) {
                val a = i * 1.047f - t * 0.9f
                ln(DSTEEL, .5f + cos(a) * .1f, .5f + sin(a) * .1f, .5f + cos(a) * .3f, .5f + sin(a) * .3f, .03f)
            }
            ci(STEEL, .5f, .5f, .08f)
        }
        UpgradeType.WATER_TANK -> {
            ci(DSTEEL, .5f, .5f, .44f); ci(Color(0xFF2E7DBF), .5f, .5f, .37f)
            for (i in 0 until 3) {
                val p = frac(i, 3, 0.45f)
                rg(Color.White.copy(alpha = 0.55f * (1f - p)), .5f, .5f, .06f + .3f * p, .02f)
            }
            ci(LSTEEL, .5f, .5f, .08f); rb(STEEL, .84f, .44f, .16f, .12f)
            ln(LSTEEL, .26f, .26f, .34f, .3f, .03f)
        }
        UpgradeType.COAL_TENDER -> {
            rb(Color(0xFF5B4636), .06f, .12f, .88f, .76f, .08f); rb(Color(0xFF1E2126), .12f, .18f, .76f, .64f, .06f)
            for (i in 0 until 9) {
                ci(Color(0xFF0F1113), .22f + (i % 3) * .2f, .3f + (i / 3) * .18f, .09f)
                ci(Color(0xFF3A3F47), .19f + (i % 3) * .2f, .27f + (i / 3) * .18f, .03f)
            }
            for (i in 0 until 3) ci(FIRE.copy(alpha = 0.4f + 0.5f * (0.5f + 0.5f * sin(t * 5f + i))), .3f + i * .2f, .5f, .02f)
            ln(LWOOD, .7f, .1f, .9f, .4f, .03f); rb(DSTEEL, .84f, .36f, .14f, .1f, .02f)
        }
        UpgradeType.PRESSURE_GAUGE -> {
            rb(BRASS, .06f, .6f, .88f, .16f, .06f); rb(COPPER, .4f, .46f, .2f, .2f, .03f)
            ci(DSTEEL, .5f, .3f, .3f); ci(CREAMC, .5f, .3f, .25f)
            for (i in 0..8) {
                val a = (PI * (0.75 + 1.5 * i / 8.0)).toFloat()
                ln(IRON, .5f + cos(a) * .2f, .3f + sin(a) * .2f, .5f + cos(a) * .24f, .3f + sin(a) * .24f, .015f)
            }
            val v = 0.3f + 0.05f * k.coerceAtMost(12) + 0.04f * sin(t * 4f)
            val a = (PI * (0.75 + 1.5 * v.coerceAtMost(.98f))).toFloat()
            ln(REDC, .5f, .3f, .5f + cos(a) * .2f, .3f + sin(a) * .2f, .025f); ci(IRON, .5f, .3f, .03f)
            ci(REDC, .22f, .84f, .08f); ci(REDC, .78f, .84f, .08f)
        }
        UpgradeType.BRASS_PISTONS -> {
            for (i in 0..1) {
                val x = .12f + i * .44f
                val ph = sin(t * 5f + i * PI.toFloat()) * .08f
                rb(BRASS, x, .1f, .32f, .46f, .05f); rb(Color(0xFFF1CF7A), x + .04f, .14f, .08f, .38f, .03f)
                ln(LSTEEL, x + .16f, .56f, x + .16f, .66f + ph, .06f)
                ci(DSTEEL, x + .16f, .74f + ph, .1f)
            }
            rb(DSTEEL, .06f, .84f, .88f, .1f, .03f)
        }
        UpgradeType.TICKET_OFFICE -> {
            rb(WOODC, .1f, .3f, .8f, .64f, .05f); rb(Color(0xFF2B2F36), .2f, .5f, .6f, .16f, .02f)
            for (i in 0 until 6) rb(if (i % 2 == 0) REDC else Color.White, .06f + i * .146f, .1f, .146f, .26f, .01f)
            rb(BRASS, .34f, .72f, .32f, .1f, .03f)
            val p = frac(0, 1, 0.8f)
            ci(Color(0xFFFFD43B), .5f, .78f - .1f * p, .03f)
        }
        UpgradeType.SAMOVAR -> {
            ci(Color(0xFF7A4A24), .5f, .52f, .4f); ci(COPPER, .5f, .5f, .37f); ci(Color(0xFFE09A62), .42f, .42f, .12f)
            rg(Color(0xFF8A4F2A), .5f, .5f, .3f, .03f); ci(Color(0xFF8A4F2A), .5f, .5f, .1f)
            rb(IRON, .86f, .46f, .14f, .08f, .02f); ci(REDC, .96f, .5f, .04f)
            for (i in 0 until 3) {
                val p = frac(i, 3, 0.5f)
                ci(Color.White.copy(alpha = .5f * (1f - p)), .5f + sin(p * 6f + i) * .05f, .5f - p * .35f, .04f + .06f * p)
            }
        }
        UpgradeType.FROST_SHIELD -> {
            val pulse = 0.5f + 0.5f * sin(t * 2.5f)
            halo(ICEC, .5f, .5f, .55f, .35f + .3f * pulse)
            val hex = Path().apply {
                for (i in 0 until 6) {
                    val a = (PI / 3 * i).toFloat()
                    val px = ox + (.5f + cos(a) * .36f) * s; val py = oy + (.5f + sin(a) * .36f) * s
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(hex, Color(0xFF4D9BD6)); drawPath(hex, Color(0xFFDFF3FF), style = Stroke(.03f * s))
            for (i in 0 until 3) {
                val p = frac(i, 3, 0.5f)
                rg(Color.White.copy(alpha = .7f * (1f - p)), .5f, .5f, .1f + .4f * p, .02f)
            }
            ci(Color.White, .5f, .5f, .07f)
        }
        UpgradeType.SPARK_COIL -> {
            ci(DSTEEL, .5f, .5f, .4f); rg(Color(0xFFB24DD6), .5f, .5f, .3f, .04f); rg(Color(0xFFD28BEA), .5f, .5f, .2f, .03f)
            ci(Color(0xFF6E2A8A), .5f, .5f, .12f)
            for (i in 0 until 2 + (k / 4).coerceAtMost(3)) {
                val a = t * 3f + i * 2.1f
                if (sin(a * 9f) > -0.3f) {
                    val ang = a * 1.7f
                    val ex = .5f + cos(ang) * .46f; val ey = .5f + sin(ang) * .46f
                    val mx = .5f + cos(ang) * .24f + .05f * sin(a * 13f); val my = .5f + sin(ang) * .24f + .05f * cos(a * 11f)
                    ln(Color(0xFFFFF0A0), .5f, .5f, mx, my, .025f); ln(Color(0xFFFFF0A0), mx, my, ex, ey, .025f)
                }
            }
            halo(Color(0xFFD28BEA), .5f, .5f, .5f, .3f)
        }
        UpgradeType.FLYWHEEL -> {
            ci(IRON, .5f, .5f, .45f); ci(Color(0xFF23262B), .5f, .5f, .38f)
            spin(t * 120f, .5f, .5f) {
                for (i in 0 until 5) {
                    val a = (2 * PI / 5 * i).toFloat()
                    ln(STEEL, .5f, .5f, .5f + cos(a) * .36f, .5f + sin(a) * .36f, .07f)
                }
                ci(REDC, .5f, .12f, .04f)
            }
            ci(LSTEEL, .5f, .5f, .1f); ci(DSTEEL, .5f, .5f, .04f)
        }
        UpgradeType.STEAM_DOME -> {
            ci(Color(0xFF36424D), .5f, .54f, .42f); ci(Color(0xFF6C8EAD), .5f, .5f, .38f); ci(Color(0xFF9DBAD3), .42f, .4f, .16f)
            rg(DSTEEL, .5f, .5f, .38f, .03f); ci(BRASS, .5f, .5f, .1f); ci(Color(0xFFF1CF7A), .48f, .48f, .04f)
            ln(BRASS, .5f, .5f, .8f, .26f, .04f); ci(REDC, .8f, .26f, .05f)
            for (i in 0 until 3) {
                val p = frac(i, 3, 0.7f)
                ci(Color.White.copy(alpha = .7f * (1f - p)), .8f + p * .1f, .26f - p * .2f, .03f + .06f * p)
            }
        }
        UpgradeType.SPARE_CRATES -> {
            val cols = listOf(Color(0xFFB98B5E), Color(0xFFD9B27C), Color(0xFFA9774A), Color(0xFFC49A6C))
            val n = (1 + k / 3).coerceAtMost(4)
            for (i in 0 until n) {
                val x = .04f + (i % 2) * .48f; val y = .04f + (i / 2) * .48f
                rb(cols[i], x, y, .44f, .44f, .03f); rb(WOODC, x, y + .19f, .44f, .06f, 0f); rb(WOODC, x + .19f, y, .06f, .44f, 0f)
            }
        }
        UpgradeType.WIND_DEFLECTOR -> {
            for (i in 0 until 3) {
                val y = .15f + i * .3f
                val p = Path().apply {
                    moveTo(ox + .1f * s, oy + (y + .2f) * s); lineTo(ox + .55f * s, oy + y * s); lineTo(ox + .6f * s, oy + (y + .06f) * s)
                    lineTo(ox + .16f * s, oy + (y + .26f) * s); close()
                }
                drawPath(p, ICEC); drawPath(p, STEEL, style = Stroke(.02f * s))
            }
            for (i in 0 until 4) {
                val p = frac(i, 4, 1.1f)
                ln(Color.White.copy(alpha = .8f * (1f - p)), .95f - p * .5f, .2f + i * .2f, .85f - p * .5f, .2f + i * .2f, .02f)
            }
        }
        UpgradeType.INSULATION -> {
            val cs = listOf(Color(0xFFF2A6B8), Color(0xFFF5D060), Color(0xFFF5D060), Color(0xFFF2A6B8))
            for (i in 0 until 4) {
                val cx = .27f + (i % 2) * .46f; val cy = .27f + (i / 2) * .46f
                ci(cs[i], cx, cy, .22f); rg(Color(0x55000000), cx, cy, .15f, .02f); rg(Color(0x55000000), cx, cy, .08f, .02f)
                ci(Color(0xFF8A6A4A), cx, cy, .03f)
            }
        }
        UpgradeType.STOVE_PIPE -> {
            val fl = 0.5f + 0.5f * sin(t * 8f)
            halo(FIRE, .4f, .55f, .5f, .35f + .25f * fl)
            ci(IRON, .4f, .55f, .34f); ci(Color(0xFF23262B), .4f, .55f, .26f)
            ci(lerpColor(Color(0xFF8A2B0A), FIRE, fl), .4f, .55f, .13f)
            rb(IRON, .72f, .46f, .26f, .1f, .03f); rg(DSTEEL, .94f, .51f, .06f, .02f)
            for (i in 0 until 3) {
                val p = frac(i, 3, 0.8f)
                ci(Color.White.copy(alpha = .5f * (1f - p)), .94f, .5f - p * .4f, .03f + .05f * p)
            }
        }
        UpgradeType.CARGO_NET -> {
            rb(LWOOD, .12f, .12f, .76f, .76f, .05f)
            for (i in 0..5) {
                ln(Color(0xFFE8DDB5), .05f + i * .18f, .05f, .05f + i * .18f, .95f, .025f)
                ln(Color(0xFFE8DDB5), .05f, .05f + i * .18f, .95f, .05f + i * .18f, .025f)
            }
            for (i in 0..5) for (j in 0..5) ci(Color(0xFFB8A878), .05f + i * .18f, .05f + j * .18f, .022f)
        }
        UpgradeType.COOLING_FAN -> {
            rb(DSTEEL, .04f, .04f, .92f, .92f, .08f); ci(Color(0xFF23262B), .5f, .5f, .4f)
            spin(t * 300f, .5f, .5f) {
                for (i in 0 until 3) {
                    val a = (2 * PI / 3 * i).toFloat()
                    ov(Color(0xFF6FD3E8), .5f + cos(a) * .2f - .1f, .5f + sin(a) * .2f - .1f, .2f, .2f)
                }
            }
            ci(LSTEEL, .5f, .5f, .06f)
            for (c in listOf(.1f to .1f, .9f to .1f, .1f to .9f, .9f to .9f)) ci(LSTEEL, c.first, c.second, .03f)
        }
        UpgradeType.BOILER_RIVETS -> {
            rb(DSTEEL, .06f, .1f, .88f, .8f, .05f)
            for (i in 0 until 5) for (j in 0 until 4) ci(if ((i + j) % 2 == 0) LSTEEL else STEEL, .16f + i * .17f, .22f + j * .19f, .045f)
            val sw = sin(t * 9f)
            ln(WOODC, .75f, .05f, .86f + .04f * sw, .3f, .05f); rb(IRON, .8f + .04f * sw, .26f, .16f, .1f, .02f)
            if (sw > .85f) halo(Color(0xFFFFD66B), .84f, .34f, .2f, .8f)
        }
        UpgradeType.WOOL_BLANKETS -> {
            val cs = listOf(Color(0xFFD1685D), Color(0xFF4A6FA5), Color(0xFF6FAE5A), Color(0xFFE0A21B))
            val n = (2 + k / 3).coerceAtMost(4)
            for (i in 0 until n) {
                val y = .72f - i * .2f
                rb(cs[i], .08f + i * .02f, y, .84f, .2f, .05f)
                for (j in 0 until 5) ln(Color(0x55FFFFFF), .16f + j * .16f, y + .02f, .16f + j * .16f, y + .18f, .02f)
            }
        }
        UpgradeType.TEA_BREWER -> {
            ci(Color(0xFFE9E4D8), .42f, .55f, .3f); ci(Color(0xFF4A6FA5), .42f, .55f, .24f); ci(Color(0xFFE9E4D8), .42f, .55f, .1f)
            rb(Color(0xFFE9E4D8), .66f, .45f, .3f, .1f, .04f)
            drawArc(Color(0xFFE9E4D8), -60f, 240f, false, Offset(ox + .05f * s, oy + .3f * s), Size(.2f * s, .2f * s), style = Stroke(.04f * s))
            ci(Color(0xFFCFE9FF), .85f, .85f, .08f); ci(Color(0xFF8B5E3C), .85f, .85f, .05f)
            for (i in 0 until 3) {
                val p = frac(i, 3, 0.6f)
                ci(Color.White.copy(alpha = .6f * (1f - p)), .42f + sin(p * 7f + i) * .05f, .5f - p * .4f, .04f + .05f * p)
            }
        }
        UpgradeType.GOLD_COUPLER -> {
            val g = Color(0xFFE0A100)
            spin(sin(t * 1.5f) * 6f, .5f, .5f) {
                rg(g, .32f, .5f, .22f, .08f); rg(Color(0xFFF1CF7A), .68f, .5f, .22f, .08f)
                rb(g, .4f, .44f, .2f, .12f, .03f)
            }
            rb(DSTEEL, .02f, .46f, .12f, .08f, .02f); rb(DSTEEL, .86f, .46f, .12f, .08f, .02f)
            val p = frac(0, 1, 0.7f)
            ci(Color.White.copy(alpha = 1f - p), .2f + .6f * p, .3f, .03f)
        }
        UpgradeType.TURBO_VALVE -> {
            rb(DSTEEL, .04f, .44f, .92f, .14f, .05f); rb(DSTEEL, .43f, .04f, .14f, .92f, .05f)
            ci(IRON, .5f, .5f, .24f)
            spin(t * 70f, .5f, .5f) {
                rg(REDC, .5f, .5f, .22f, .05f)
                for (i in 0 until 3) {
                    val a = (2 * PI / 3 * i).toFloat()
                    ln(REDC, .5f, .5f, .5f + cos(a) * .22f, .5f + sin(a) * .22f, .05f)
                }
            }
            ci(LSTEEL, .5f, .5f, .05f)
            for (i in 0 until 2) {
                val p = frac(i, 2, 0.9f)
                ci(Color.White.copy(alpha = .6f * (1f - p)), .06f - 0f + p * .2f, .1f, .03f + .04f * p)
            }
        }
        UpgradeType.ICE_CUTTER -> {
            spin(t * 380f, .5f, .5f) {
                ci(LSTEEL, .5f, .5f, .3f)
                for (i in 0 until 12) {
                    val a = (PI / 6 * i).toFloat()
                    ci(LSTEEL, .5f + cos(a) * .33f, .5f + sin(a) * .33f, .05f)
                }
                ci(DSTEEL, .5f, .5f, .12f)
            }
            ci(IRON, .5f, .5f, .05f)
            for (i in 0 until 4) {
                val p = frac(i, 4, 1.2f)
                ci(ICEC.copy(alpha = 1f - p), .85f + p * .1f, .15f + i * .06f - p * .05f, .03f)
            }
            rb(ICEC, .02f, .78f, .3f, .18f, .03f)
        }
        UpgradeType.GREASE_PUMP -> {
            ci(Color(0xFF8A5A1E), .42f, .6f, .34f); ci(Color(0xFFB3792A), .42f, .6f, .28f); rg(Color(0xFF6A4412), .42f, .6f, .2f, .03f)
            val pump = sin(t * 4f) * .1f
            ln(LSTEEL, .42f, .6f, .86f, .2f + pump, .05f); ci(REDC, .88f, .18f + pump, .06f)
            ci(Color(0xFFE0A21B).copy(alpha = .8f), .9f, .86f, .05f + .02f * sin(t * 6f))
        }
        UpgradeType.FUEL_INJECTOR -> {
            ci(Color(0xFF2F7A52), .38f, .5f, .34f); ci(Color(0xFF3FAE7A), .38f, .5f, .28f); rg(Color(0xFF1F5A3A), .38f, .5f, .2f, .03f)
            ci(LSTEEL, .38f, .5f, .08f)
            drawArc(IRON, 20f, 250f, false, Offset(ox + .55f * s, oy + .55f * s), Size(.36f * s, .36f * s), style = Stroke(.05f * s))
            rb(DSTEEL, .76f, .15f, .2f, .12f, .03f); ln(IRON, .7f, .55f, .86f, .27f, .05f)
            halo(FIRE, .96f, .21f, .22f, 0.3f + 0.3f * sin(t * 6f))
        }
        UpgradeType.CABIN_HEATER -> {
            val pulse = 0.5f + 0.5f * sin(t * 3f)
            halo(Color(0xFFFF6B3D), .5f, .5f, .6f, .25f + .2f * pulse)
            rb(Color(0xFF8A2B2B), .08f, .14f, .84f, .72f, .06f)
            for (i in 0 until 6) rb(lerpColor(Color(0xFFB83A3A), Color(0xFFFF8A5B), pulse), .14f + i * .12f, .2f, .07f, .6f, .02f)
            ci(Color(0xFFE9E4D8), .84f, .84f, .05f)
        }
        UpgradeType.CARGO_CRANE -> {
            ci(DSTEEL, .3f, .62f, .26f); ci(IRON, .3f, .62f, .15f)
            val a = -0.8f + sin(t * .8f) * .5f
            ln(Color(0xFFE0A21B), .3f, .62f, .3f + cos(a) * .6f, .62f + sin(a) * .6f, .07f)
            val hx = .3f + cos(a) * .6f; val hy = .62f + sin(a) * .6f
            ln(DSTEEL, hx, hy, hx, hy + .1f, .02f); rb(LWOOD, hx - .08f, hy + .08f, .16f, .16f, .02f)
            ci(REDC, .3f, .62f, .04f)
        }
        UpgradeType.LOUNGE -> {
            rb(Color(0xFFB5443B), .04f, .1f, .92f, .8f, .06f); rb(Color(0xFFD1685D), .08f, .15f, .84f, .7f, .04f)
            rb(Color(0xFF7A4B3A), .1f, .18f, .46f, .38f, .1f); rb(Color(0xFFD1856B), .16f, .24f, .34f, .26f, .08f)
            rb(WOODC, .6f, .52f, .3f, .3f, .04f); ci(Color(0xFFF2F2F2), .72f, .64f, .05f); ci(Color(0xFFE0A21B), .72f, .64f, .028f)
        }
        UpgradeType.BANK_SAFE -> {
            rb(DSTEEL, .08f, .08f, .84f, .84f, .06f); rb(STEEL, .14f, .14f, .72f, .72f, .04f)
            ci(IRON, .5f, .5f, .2f); rg(LSTEEL, .5f, .5f, .17f, .03f)
            spin(t * 20f, .5f, .5f) { ln(BRASS, .5f, .5f, .5f, .36f, .04f); ln(BRASS, .5f, .5f, .62f, .56f, .03f) }
            ci(BRASS, .5f, .5f, .05f); rb(BRASS, .78f, .42f, .06f, .16f, .02f)
            if (k > 2) { ci(Color(0xFFFFD43B), .24f, .8f, .05f); ci(Color(0xFFFFD43B), .32f, .82f, .05f) }
        }
        UpgradeType.COMPASS -> {
            ci(Color(0xFF6A4412), .5f, .5f, .45f); ci(CREAMC, .5f, .5f, .4f); rg(BRASS, .5f, .5f, .4f, .04f)
            for (i in 0 until 4) {
                val a = (PI / 2 * i).toFloat()
                ln(IRON, .5f + cos(a) * .3f, .5f + sin(a) * .3f, .5f + cos(a) * .38f, .5f + sin(a) * .38f, .03f)
            }
            spin(sin(t * 0.7f) * 40f + 20f, .5f, .5f) {
                val n = Path().apply { moveTo(ox + .5f * s, oy + .14f * s); lineTo(ox + .58f * s, oy + .5f * s); lineTo(ox + .42f * s, oy + .5f * s); close() }
                drawPath(n, REDC)
                val sp = Path().apply { moveTo(ox + .5f * s, oy + .86f * s); lineTo(ox + .58f * s, oy + .5f * s); lineTo(ox + .42f * s, oy + .5f * s); close() }
                drawPath(sp, DSTEEL)
            }
            ci(BRASS, .5f, .5f, .04f)
        }
        UpgradeType.GOVERNOR -> {
            ci(DSTEEL, .5f, .5f, .14f); ci(IRON, .5f, .5f, .08f)
            val ext = .22f + .1f * (0.5f + 0.5f * sin(t * 2f))
            spin(t * 90f, .5f, .5f) {
                ln(STEEL, .5f, .5f, .5f - ext - .08f, .5f, .04f); ln(STEEL, .5f, .5f, .5f + ext + .08f, .5f, .04f)
                ci(REDC, .5f - ext - .1f, .5f, .1f); ci(REDC, .5f + ext + .1f, .5f, .1f)
                ci(Color(0xAAFFFFFF), .5f - ext - .12f, .47f, .03f); ci(Color(0xAAFFFFFF), .5f + ext + .08f, .47f, .03f)
            }
            rg(DSTEEL, .5f, .5f, .46f, .02f)
        }
        UpgradeType.MAGNET_COUPLER -> {
            val u = Path().apply {
                moveTo(ox + .16f * s, oy + .1f * s); lineTo(ox + .16f * s, oy + .56f * s)
                cubicTo(ox + .16f * s, oy + .98f * s, ox + .84f * s, oy + .98f * s, ox + .84f * s, oy + .56f * s)
                lineTo(ox + .84f * s, oy + .1f * s)
            }
            drawPath(u, Color(0xFFB8343A), style = Stroke(.16f * s, cap = StrokeCap.Butt))
            rb(LSTEEL, .08f, .06f, .16f, .16f, .01f); rb(LSTEEL, .76f, .06f, .16f, .16f, .01f)
            for (i in 0 until 3) {
                val p = frac(i, 3, 0.6f)
                drawArc(Color(0xFF8FD0FF).copy(alpha = 1f - p), 180f, 180f, false, Offset(ox + (.3f - .1f * p) * s, oy + (.0f - .05f * p) * s), Size((.4f + .2f * p) * s, (.3f + .1f * p) * s), style = Stroke(.02f * s))
            }
        }
        UpgradeType.AURORA_PRISM -> {
            val tri = Path().apply { moveTo(ox + .5f * s, oy + .12f * s); lineTo(ox + .86f * s, oy + .76f * s); lineTo(ox + .14f * s, oy + .76f * s); close() }
            val cols = listOf(Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFFFFF176), Color(0xFF6FE38B), Color(0xFF4DD0E1), Color(0xFFB07CFF))
            for (i in cols.indices) ln(cols[i].copy(alpha = .8f), .6f, .5f, .98f, .38f + i * .07f + .02f * sin(t * 2f + i), .045f)
            ln(Color.White.copy(alpha = .8f), .0f, .6f, .45f, .55f, .04f)
            drawPath(tri, Color(0x99DFF3FF)); drawPath(tri, Color.White, style = Stroke(.025f * s))
            halo(Color(0xFFB07CFF), .5f, .5f, .5f, .15f + .1f * sin(t * 2f))
        }
        UpgradeType.TRACTION_SAND -> {
            rb(Color(0xFF7A5A3C), .06f, .1f, .88f, .8f, .06f); rb(Color(0xFFD9C08A), .12f, .16f, .76f, .68f, .05f)
            ci(Color(0xFFE8D4A0), .5f, .5f, .22f + .01f * k.coerceAtMost(8)); ci(Color(0xFFF2E4BC), .45f, .45f, .1f)
            for (i in 0 until 8) ci(Color(0xFFB89A62), .2f + (i * .37f) % .6f, .24f + (i * .53f) % .52f, .015f)
            ln(WOODC, .6f, .15f, .9f, .6f, .04f); rb(DSTEEL, .8f, .55f, .16f, .14f, .03f)
        }
        UpgradeType.CHEFS_OVEN -> {
            rb(IRON, .06f, .06f, .88f, .88f, .06f)
            for (i in 0 until 4) {
                val cx = .3f + (i % 2) * .4f; val cy = .3f + (i / 2) * .4f
                ci(Color(0xFF23262B), cx, cy, .15f); rg(DSTEEL, cx, cy, .12f, .02f)
                val on = i < (1 + k / 3).coerceAtMost(4)
                if (on) {
                    val fl = 0.5f + 0.5f * sin(t * 8f + i)
                    halo(FIRE, cx, cy, .22f, .4f + .2f * fl); ci(lerpColor(Color(0xFFB83A0A), Color(0xFFFFB347), fl), cx, cy, .08f)
                }
            }
            ci(REDC, .5f, .96f, .02f)
        }
        UpgradeType.BOGIE_SPRINGS -> {
            for (i in 0..1) {
                val cx = .27f + i * .46f
                val comp = sin(t * 4f + i * 2f) * .03f
                ci(DSTEEL, cx, .5f, .22f)
                for (r in 0 until 4) rg(if (r % 2 == 0) LSTEEL else STEEL, cx, .5f, .2f - r * (.045f + comp), .035f)
                ci(IRON, cx, .5f, .03f)
            }
            rb(DSTEEL, .04f, .88f, .92f, .08f, .02f)
        }

        // ---------------- overhead devices ----------------
        UpgradeType.STEAM_WHISTLE -> {
            rb(BRASS, .05f, .42f, .6f, .16f, .05f); ci(Color(0xFFF1CF7A), .72f, .5f, .22f); rg(COPPER, .72f, .5f, .22f, .04f); ci(COPPER, .72f, .5f, .08f)
            for (i in 0 until 4) {
                val p = frac(i, 4, 0.9f)
                rg(Color.White.copy(alpha = .6f * (1f - p)), .72f, .5f, .22f + .26f * p, .02f)
            }
        }
        UpgradeType.HEADLAMP -> {
            val a = .35f + .6f * night
            halo(Color(0xFFFFF3B0), .5f, .5f, .62f, a * (0.85f + 0.15f * sin(t * 4f)))
            ci(DSTEEL, .5f, .5f, .34f); ci(Color(0xFFFFE680), .5f, .5f, .26f); ci(Color.White, .44f, .44f, .09f)
            rg(IRON, .5f, .5f, .3f, .035f)
        }
        UpgradeType.ROOF_FAN -> {
            ci(Color(0x33000000), .5f, .5f, .44f)
            spin(t * 260f, .5f, .5f) {
                for (i in 0 until 4) {
                    val a = (PI / 2 * i).toFloat()
                    ov(Color(0xFFB98B5E), .5f + cos(a) * .22f - .1f, .5f + sin(a) * .22f - .08f, .22f, .16f)
                }
            }
            ci(BRASS, .5f, .5f, .09f); ci(Color(0xFFFFE680), .5f, .5f, .04f)
        }
        UpgradeType.RADIO_MAST -> {
            for (i in 0 until 3) {
                val p = frac(i, 3, 0.7f)
                rg(Color(0xFF3FAE5A).copy(alpha = .7f * (1f - p)), .5f, .5f, .1f + .38f * p, .025f)
            }
            ln(DSTEEL, .5f, .5f, .5f, .1f, .05f); ln(DSTEEL, .26f, .5f, .74f, .5f, .04f); ln(DSTEEL, .34f, .3f, .66f, .3f, .03f)
            ci(REDC, .5f, .5f, .07f)
            val blink = if (sin(t * 6f) > 0f) 1f else 0.3f
            halo(REDC, .5f, .5f, .25f, blink * 0.5f)
        }
        UpgradeType.POCKET_WATCH -> {
            ln(BRASS, .5f, .14f, .8f, .02f, .04f); rg(BRASS, .5f, .1f, .07f, .03f)
            ci(BRASS, .5f, .56f, .38f); ci(CREAMC, .5f, .56f, .32f)
            for (i in 0 until 12) {
                val a = (PI / 6 * i).toFloat()
                ln(IRON, .5f + cos(a) * .26f, .56f + sin(a) * .26f, .5f + cos(a) * .3f, .56f + sin(a) * .3f, .015f)
            }
            val ma = t * 2.2f - 1.57f; val ha = t * .18f - 1.57f
            ln(IRON, .5f, .56f, .5f + cos(ma) * .26f, .56f + sin(ma) * .26f, .02f); ln(REDC, .5f, .56f, .5f + cos(ha) * .16f, .56f + sin(ha) * .16f, .03f)
        }
        UpgradeType.LANTERN_RACK -> {
            rb(WOODC, .02f, .42f, .96f, .1f, .03f)
            for (i in 0 until 3) {
                val cx = .2f + i * .3f
                halo(Color(0xFFFFB347), cx, .5f, .3f, .3f + .5f * night + .08f * sin(t * 5f + i))
                ci(Color(0xFF3A3F47), cx, .5f, .13f); ci(Color(0xFFFFC857), cx, .5f, .09f); ci(Color.White, cx - .02f, .48f, .03f)
            }
        }
        UpgradeType.SIGNAL_BELL -> {
            val sw = sin(t * 4f) * .06f
            ci(Color(0x33000000), .5f, .5f, .44f)
            ci(Color(0xFFB0782A), .5f, .5f, .4f); rg(Color(0xFFF1CF7A), .5f, .5f, .3f, .04f); rg(Color(0xFF7A4F14), .5f, .5f, .2f, .03f)
            ci(Color(0xFFF1CF7A), .5f, .5f, .1f); ci(IRON, .5f + sw, .5f, .05f)
            for (i in 0 until 2) {
                val p = frac(i, 2, 1.0f)
                rg(Color(0xFFFFE680).copy(alpha = .6f * (1f - p)), .5f, .5f, .4f + .1f * p, .02f)
            }
        }
        UpgradeType.FROST_RADAR -> {
            ci(Color(0xFF14323A), .5f, .5f, .45f); rg(Color(0xFF3FAE9A), .5f, .5f, .45f, .025f)
            rg(Color(0x663FAE9A), .5f, .5f, .3f, .015f); rg(Color(0x663FAE9A), .5f, .5f, .15f, .015f)
            ln(Color(0x663FAE9A), .05f, .5f, .95f, .5f, .012f); ln(Color(0x663FAE9A), .5f, .05f, .5f, .95f, .012f)
            val a = t * 3f
            for (i in 0 until 8) {
                val aa = a - i * 0.12f
                ln(Color(0xFF6FFFD0).copy(alpha = .6f - i * .07f), .5f, .5f, .5f + cos(aa) * .43f, .5f + sin(aa) * .43f, .04f)
            }
            val bl = frac(0, 1, 0.5f)
            ci(Color(0xFFFFE680).copy(alpha = 1f - bl), .66f, .36f, .04f)
        }
        UpgradeType.NIGHT_WATCH -> {
            ci(Color(0xFF1C2541), .5f, .5f, .45f); rg(Color(0xFF5BC0BE), .5f, .5f, .45f, .03f)
            drawArc(Color(0xFFFFE680), 40f, 260f, true, Offset(ox + .3f * s, oy + .3f * s), Size(.4f * s, .4f * s))
            ci(Color(0xFF1C2541), .6f, .42f, .17f)
            for (i in 0 until 5) {
                val tw = 0.5f + 0.5f * sin(t * 3f + i * 1.7f)
                ci(Color.White.copy(alpha = .3f + .7f * tw), .18f + i * .16f, .18f + (i * .37f) % .5f, .02f)
            }
        }
        UpgradeType.TIMETABLE -> {
            rb(Color(0xFF23262B), .02f, .14f, .96f, .72f, .04f)
            for (i in 0 until 4) {
                val on = ((t * 0.7f + i * 0.9f).toInt() % 2 == 0)
                ln(if (on) Color(0xFFFFE680) else Color(0xFF8FD0FF), .1f, .26f + i * .16f, .5f + .1f * ((i * 3) % 4), .26f + i * .16f, .06f)
                ci(if (on) GREENC else REDC, .88f, .26f + i * .16f, .035f)
            }
            rb(BRASS, .02f, .1f, .96f, .06f, .02f)
        }
        UpgradeType.POLAR_BEACON -> {
            halo(Color(0xFF8FE9FF), .5f, .5f, .6f, .25f + .5f * night)
            ci(DSTEEL, .5f, .5f, .32f); ci(Color(0xFF1B6A86), .5f, .5f, .26f)
            spin(t * 140f, .5f, .5f) {
                val b = Path().apply { moveTo(ox + .5f * s, oy + .5f * s); lineTo(ox + .98f * s, oy + .36f * s); lineTo(ox + .98f * s, oy + .64f * s); close() }
                drawPath(b, Color(0xAA8FE9FF))
                val b2 = Path().apply { moveTo(ox + .5f * s, oy + .5f * s); lineTo(ox + .02f * s, oy + .36f * s); lineTo(ox + .02f * s, oy + .64f * s); close() }
                drawPath(b2, Color(0x668FE9FF))
            }
            ci(Color.White, .5f, .5f, .08f)
        }
        else -> {
            ci(DSTEEL, .5f, .5f, .35f)
        }
    }

    // upgrade burst: a ring and sparks that fade out right after buying
    if (flash > 0.01f) {
        val grow = 1f - flash
        rg(Color(0xFFFFE680).copy(alpha = flash * 0.8f), .5f, .5f, .15f + .4f * grow, .04f * flash)
        for (i in 0 until 8) {
            val a = (PI / 4 * i).toFloat() + 0.3f
            val d = .2f + .3f * grow
            ci(Color.White.copy(alpha = flash), .5f + cos(a) * d, .5f + sin(a) * d, .02f)
        }
    }
}

private fun lerpColor(a: Color, b: Color, f: Float): Color = androidx.compose.ui.graphics.lerp(a, b, f.coerceIn(0f, 1f))


// ---------------- materials: real-looking surfaces multiplied onto the device drawings ----------------

/** Greyscale surface textures (generated by store_assets/make_textures.py). */
class Materials(
    val metal: ShaderBrush,
    val wood: ShaderBrush,
    val fabric: ShaderBrush,
    val rough: ShaderBrush,
    val paint: ShaderBrush,
) {
    /** The surface each device is made of. */
    fun forType(t: UpgradeType): ShaderBrush = when (t) {
        UpgradeType.WAGON, UpgradeType.TICKET_OFFICE, UpgradeType.SPARE_CRATES, UpgradeType.CARGO_NET -> wood
        UpgradeType.CONDUCTOR, UpgradeType.WOOL_BLANKETS, UpgradeType.INSULATION, UpgradeType.LOUNGE -> fabric
        UpgradeType.COAL, UpgradeType.COAL_TENDER, UpgradeType.STOVE_PIPE, UpgradeType.TRACTION_SAND,
        UpgradeType.CHEFS_OVEN, UpgradeType.FLYWHEEL, UpgradeType.SNOW_CHAINS, UpgradeType.ICE_CUTTER -> rough
        UpgradeType.FOOD, UpgradeType.SAMOVAR, UpgradeType.TEA_BREWER, UpgradeType.AURORA_PRISM,
        UpgradeType.TIMETABLE, UpgradeType.NIGHT_WATCH, UpgradeType.HEAT -> paint
        else -> metal
    }
}

private fun tile(img: ImageBitmap) = ShaderBrush(ImageShader(img, TileMode.Repeated, TileMode.Repeated))

@Composable
fun rememberMaterials(): Materials {
    val metal = ImageBitmap.imageResource(R.drawable.mat_metal)
    val wood = ImageBitmap.imageResource(R.drawable.mat_wood)
    val fabric = ImageBitmap.imageResource(R.drawable.mat_fabric)
    val rough = ImageBitmap.imageResource(R.drawable.mat_rough)
    val paint = ImageBitmap.imageResource(R.drawable.mat_paint)
    return remember(metal, wood, fabric, rough, paint) { Materials(tile(metal), tile(wood), tile(fabric), tile(rough), tile(paint)) }
}

/**
 * Draws [block] into its own layer, then multiplies a material texture onto it and adds light from the
 * top-left and shade toward the bottom-right. Only the drawn pixels are affected, the rest stays clear.
 * [texScale] = local units per texture pixel.
 */
fun DrawScope.withMaterial(mat: ShaderBrush?, left: Float, top: Float, w: Float, h: Float, texScale: Float, block: DrawScope.() -> Unit) {
    if (mat == null || w <= 1f || h <= 1f) { block(); return }
    val canvas = drawContext.canvas
    canvas.saveLayer(Rect(left, top, left + w, top + h), Paint())
    block()
    withTransform({ translate(left, top); scale(texScale, texScale, Offset.Zero) }) {
        drawRect(mat, Offset.Zero, Size(w / texScale, h / texScale), blendMode = BlendMode.Modulate)
    }
    drawRect(
        Brush.linearGradient(
            listOf(Color(0x3DFFFFFF), Color.Transparent, Color(0x47000000)),
            start = Offset(left, top), end = Offset(left + w, top + h),
        ),
        Offset(left, top), Size(w, h), blendMode = BlendMode.SrcAtop,
    )
    canvas.restore()
}
