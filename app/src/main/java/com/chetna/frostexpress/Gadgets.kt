package com.chetna.frostexpress

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Colour used for a device family in lists and dialogs. */
fun gadgetColor(g: Gadget): Color = when (g) {
    Gadget.GEAR -> Color(0xFF8A6B4E)
    Gadget.WHEEL -> Color(0xFF3F7CAC)
    Gadget.FLAME -> Color(0xFFE5484D)
    Gadget.LAMP -> Color(0xFFE0A100)
    Gadget.STEAM -> Color(0xFF6C8EAD)
    Gadget.CRATE -> Color(0xFF2F8FE0)
    Gadget.FAN -> Color(0xFF2FA4A0)
    Gadget.COIL -> Color(0xFFB24DD6)
    Gadget.BELL -> Color(0xFFD08A1E)
    Gadget.TANK -> Color(0xFF3B8EDC)
    Gadget.DIAL -> Color(0xFF5C7CFA)
    Gadget.SHIELD -> Color(0xFF4D9BD6)
    Gadget.COIN -> Color(0xFFE0A100)
    Gadget.CLOCK -> Color(0xFF7A5CC8)
    Gadget.ANTENNA -> Color(0xFF3FAE5A)
    Gadget.CUP -> Color(0xFF3FAE5A)
    Gadget.PLOW -> Color(0xFF5BA7D9)
    Gadget.COALPILE -> Color(0xFF6B7580)
    Gadget.ENGINE -> Color(0xFFE8890C)
    Gadget.BED -> Color(0xFF5C7CFA)
}

private fun dark(c: Color, f: Float = 0.35f) = lerp(c, Color.Black, f)
private fun light(c: Color, f: Float = 0.45f) = lerp(c, Color.White, f)

private fun DrawScope.gear(cx: Float, cy: Float, r: Float, teeth: Int, rotDeg: Float, c: Color) {
    rotate(rotDeg, Offset(cx, cy)) {
        for (i in 0 until teeth) {
            rotate(360f / teeth * i, Offset(cx, cy)) {
                drawRoundRect(c, Offset(cx - r * 0.15f, cy - r * 1.22f), Size(r * 0.3f, r * 0.42f), CornerRadius(r * 0.05f))
            }
        }
        drawCircle(c, r, Offset(cx, cy))
        drawCircle(dark(c, 0.55f), r * 0.34f, Offset(cx, cy))
        drawCircle(light(c), r * 0.12f, Offset(cx, cy))
    }
}

/**
 * Draws one device in the unit box of [size]. [t] is time in seconds, [level] is the upgrade level
 * (more parts, faster motion, bigger effects as it grows) and [flash] is 1..0 right after an upgrade.
 */
fun DrawScope.drawGadget(g: Gadget, color: Color, t: Float, level: Int, flash: Float) {
    val s = min(size.width, size.height)
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    val lv = level.coerceAtLeast(0)
    val stage = (lv / 3).coerceAtMost(4) // 0..4: extra parts appear as the level grows
    val spin = 1f + lv * 0.12f
    fun x(v: Float) = ox + v * s
    fun y(v: Float) = oy + v * s
    fun sz(v: Float) = v * s
    val body = color
    val shade = dark(color)
    val hi = light(color)

    // soft floor shadow
    drawOval(Color(0x33000000), Offset(x(0.18f), y(0.86f)), Size(sz(0.64f), sz(0.09f)))

    when (g) {
        Gadget.GEAR -> {
            gear(x(0.38f), y(0.55f), sz(0.2f), 10, t * 40f * spin, body)
            if (stage >= 1) gear(x(0.68f), y(0.38f), sz(0.13f), 8, -t * 60f * spin + 12f, hi)
            if (stage >= 2) gear(x(0.7f), y(0.72f), sz(0.11f), 8, -t * 70f * spin, shade)
            if (stage >= 3) gear(x(0.16f), y(0.28f), sz(0.09f), 7, t * 80f * spin, hi)
        }
        Gadget.WHEEL -> {
            val cx = x(0.5f); val cy = y(0.55f); val r = sz(0.3f)
            drawCircle(shade, r, Offset(cx, cy), style = Stroke(width = sz(0.07f)))
            rotate(t * 90f * spin, Offset(cx, cy)) {
                for (i in 0 until 6) {
                    val a = (PI / 3 * i).toFloat()
                    drawLine(body, Offset(cx, cy), Offset(cx + cos(a) * r, cy + sin(a) * r), sz(0.04f), StrokeCap.Round)
                }
            }
            drawCircle(hi, sz(0.06f), Offset(cx, cy))
            if (stage >= 2) drawCircle(hi.copy(alpha = 0.6f), r * 1.12f, Offset(cx, cy), style = Stroke(width = sz(0.015f)))
            drawLine(Color(0xFF5A4A3B), Offset(x(0.05f), y(0.86f)), Offset(x(0.95f), y(0.86f)), sz(0.04f))
        }
        Gadget.FLAME -> {
            drawRoundRect(Color(0xFF3A3028), Offset(x(0.2f), y(0.5f)), Size(sz(0.6f), sz(0.36f)), CornerRadius(sz(0.05f)))
            drawRoundRect(Color(0xFF1B1511), Offset(x(0.27f), y(0.57f)), Size(sz(0.46f), sz(0.22f)), CornerRadius(sz(0.03f)))
            val n = 2 + stage
            for (i in 0 until n) {
                val fx = x(0.3f + 0.4f * (i + 0.5f) / n)
                val hgt = sz(0.18f + 0.04f * stage) * (0.8f + 0.25f * sin(t * 9f + i * 1.7f))
                val p = Path().apply {
                    moveTo(fx - sz(0.05f), y(0.79f))
                    cubicTo(fx - sz(0.07f), y(0.79f) - hgt * 0.6f, fx, y(0.79f) - hgt * 0.7f, fx, y(0.79f) - hgt)
                    cubicTo(fx + sz(0.01f), y(0.79f) - hgt * 0.7f, fx + sz(0.07f), y(0.79f) - hgt * 0.6f, fx + sz(0.05f), y(0.79f))
                    close()
                }
                drawPath(p, Color(0xFFFF8A2B))
                drawCircle(Color(0xFFFFD66B), sz(0.025f), Offset(fx, y(0.76f)))
            }
            drawRect(shade, Offset(x(0.2f), y(0.46f)), Size(sz(0.6f), sz(0.05f)))
        }
        Gadget.LAMP -> {
            val beamLen = sz(0.25f + 0.05f * stage) * (0.9f + 0.1f * sin(t * 4f))
            val beam = Path().apply {
                moveTo(x(0.5f), y(0.5f)); lineTo(x(0.5f) - beamLen, y(0.5f) - beamLen * 0.0f - sz(0.3f))
                lineTo(x(0.5f) + beamLen, y(0.5f) - sz(0.3f)); close()
            }
            drawPath(beam, Color(0xFFFFF3B0).copy(alpha = 0.35f + 0.08f * stage))
            drawRect(dark(body), Offset(x(0.46f), y(0.66f)), Size(sz(0.08f), sz(0.2f)))
            drawCircle(shade, sz(0.17f), Offset(x(0.5f), y(0.56f)))
            drawCircle(Color(0xFFFFE680), sz(0.12f), Offset(x(0.5f), y(0.56f)))
            drawCircle(Color.White.copy(alpha = 0.8f), sz(0.05f), Offset(x(0.47f), y(0.53f)))
        }
        Gadget.STEAM -> {
            drawRoundRect(body, Offset(x(0.15f), y(0.62f)), Size(sz(0.7f), sz(0.14f)), CornerRadius(sz(0.04f)))
            drawRoundRect(shade, Offset(x(0.4f), y(0.45f)), Size(sz(0.14f), sz(0.2f)), CornerRadius(sz(0.03f)))
            for (i in 0 until 3 + stage) {
                val ph = ((t * (0.5f + 0.1f * lv.coerceAtMost(8)) + i / (3f + stage)) % 1f)
                val cx = x(0.47f) + sin((ph * 6f + i).toDouble()).toFloat() * sz(0.05f)
                drawCircle(Color.White.copy(alpha = 0.85f * (1f - ph)), sz(0.05f + 0.07f * ph), Offset(cx, y(0.45f) - ph * sz(0.38f)))
            }
        }
        Gadget.CRATE -> {
            val n = 1 + min(5, lv / 2)
            val w = sz(0.26f)
            val pos = listOf(0.12f to 0.86f, 0.40f to 0.86f, 0.68f to 0.86f, 0.26f to 0.6f, 0.54f to 0.6f, 0.4f to 0.34f)
            for (i in 0 until n) {
                val (px, bottom) = pos[i]
                val drop = if (i == n - 1) flash * sz(0.25f) else 0f
                val tx = x(px); val ty = y(bottom) - w - drop
                drawRoundRect(body, Offset(tx, ty), Size(w, w), CornerRadius(sz(0.02f)))
                drawRect(shade, Offset(tx, ty + w * 0.46f), Size(w, w * 0.08f))
                drawRect(shade, Offset(tx + w * 0.46f, ty), Size(w * 0.08f, w))
            }
        }
        Gadget.FAN -> {
            val cx = x(0.5f); val cy = y(0.5f)
            drawCircle(shade, sz(0.36f), Offset(cx, cy), style = Stroke(width = sz(0.04f)))
            val blades = 3 + (stage / 2)
            rotate(t * 220f * spin, Offset(cx, cy)) {
                for (i in 0 until blades) {
                    rotate(360f / blades * i, Offset(cx, cy)) {
                        drawOval(body, Offset(cx - sz(0.06f), cy - sz(0.3f)), Size(sz(0.12f), sz(0.27f)))
                    }
                }
            }
            drawCircle(hi, sz(0.06f), Offset(cx, cy))
        }
        Gadget.COIL -> {
            drawRoundRect(shade, Offset(x(0.3f), y(0.6f)), Size(sz(0.4f), sz(0.25f)), CornerRadius(sz(0.04f)))
            for (i in 0 until 5) drawLine(hi, Offset(x(0.34f + i * 0.08f), y(0.62f)), Offset(x(0.38f + i * 0.08f), y(0.83f)), sz(0.03f), StrokeCap.Round)
            drawRect(body, Offset(x(0.46f), y(0.4f)), Size(sz(0.08f), sz(0.2f)))
            drawCircle(body, sz(0.07f), Offset(x(0.5f), y(0.38f)))
            val arcs = 1 + stage
            for (i in 0 until arcs) {
                val a = t * 3f + i * 2.1f
                val on = sin((a * 7f).toDouble()) > -0.2
                if (on) {
                    val ang = (a % 6.28f) - PI.toFloat()
                    val ex = x(0.5f) + cos(ang) * sz(0.32f)
                    val ey = y(0.38f) + sin(ang) * sz(0.25f) - sz(0.05f)
                    val mx = (x(0.5f) + ex) / 2f + sz(0.04f) * sin(a * 11f)
                    val my = (y(0.38f) + ey) / 2f - sz(0.04f) * cos(a * 9f)
                    val p = Path().apply { moveTo(x(0.5f), y(0.38f)); lineTo(mx, my); lineTo(ex, ey) }
                    drawPath(p, Color(0xFFFFF0A0), style = Stroke(width = sz(0.025f), cap = StrokeCap.Round))
                }
            }
        }
        Gadget.BELL -> {
            val sw = sin(t * (2.5f + 0.2f * lv.coerceAtMost(10))) * (14f + 3f * stage)
            drawRect(Color(0xFF5A4A3B), Offset(x(0.25f), y(0.16f)), Size(sz(0.5f), sz(0.05f)))
            rotate(sw, Offset(x(0.5f), y(0.2f))) {
                drawLine(shade, Offset(x(0.5f), y(0.2f)), Offset(x(0.5f), y(0.3f)), sz(0.03f))
                val p = Path().apply {
                    moveTo(x(0.3f), y(0.7f)); cubicTo(x(0.3f), y(0.5f), x(0.4f), y(0.32f), x(0.5f), y(0.3f))
                    cubicTo(x(0.6f), y(0.32f), x(0.7f), y(0.5f), x(0.7f), y(0.7f)); close()
                }
                drawPath(p, body)
                drawRect(shade, Offset(x(0.27f), y(0.68f)), Size(sz(0.46f), sz(0.05f)))
                drawCircle(shade, sz(0.05f), Offset(x(0.5f), y(0.76f)))
            }
            if (stage >= 2) for (i in 0..1) {
                val d = if (i == 0) -1f else 1f
                drawArc(hi.copy(alpha = 0.6f), if (i == 0) 150f else -30f, 60f, false, Offset(x(0.5f) + d * sz(0.3f) - sz(0.08f), y(0.5f) - sz(0.1f)), Size(sz(0.16f), sz(0.2f)), style = Stroke(width = sz(0.02f)))
            }
        }
        Gadget.TANK -> {
            val fill = (0.22f + 0.07f * lv).coerceAtMost(0.92f)
            val tx = x(0.28f); val ty = y(0.2f); val tw = sz(0.44f); val th = sz(0.62f)
            drawRoundRect(Color(0x33FFFFFF), Offset(tx, ty), Size(tw, th), CornerRadius(sz(0.08f)))
            val top = ty + th * (1f - fill)
            val wave = Path().apply {
                moveTo(tx, top)
                var px = 0f
                while (px <= tw) { lineTo(tx + px, top + sin((px / tw * 6.28f + t * 3f).toDouble()).toFloat() * sz(0.012f)); px += tw / 12f }
                lineTo(tx + tw, ty + th - sz(0.06f)); lineTo(tx, ty + th - sz(0.06f)); close()
            }
            drawPath(wave, body.copy(alpha = 0.85f))
            drawRoundRect(shade, Offset(tx, ty), Size(tw, th), CornerRadius(sz(0.08f)), style = Stroke(width = sz(0.04f)))
            drawRect(shade, Offset(x(0.46f), y(0.12f)), Size(sz(0.08f), sz(0.08f)))
        }
        Gadget.DIAL -> {
            val cx = x(0.5f); val cy = y(0.5f); val r = sz(0.36f)
            drawCircle(Color(0xFFF6F0E4), r, Offset(cx, cy))
            drawCircle(shade, r, Offset(cx, cy), style = Stroke(width = sz(0.05f)))
            for (i in 0..8) {
                val a = (PI * (0.75 + 1.5 * i / 8.0)).toFloat()
                drawLine(shade, Offset(cx + cos(a) * r * 0.78f, cy + sin(a) * r * 0.78f), Offset(cx + cos(a) * r * 0.92f, cy + sin(a) * r * 0.92f), sz(0.02f))
            }
            val target = (0.15f + 0.07f * lv).coerceAtMost(0.95f)
            val v = target + 0.03f * sin(t * 6f)
            val a = (PI * (0.75 + 1.5 * v)).toFloat()
            drawLine(Color(0xFFE5484D), Offset(cx, cy), Offset(cx + cos(a) * r * 0.75f, cy + sin(a) * r * 0.75f), sz(0.03f), StrokeCap.Round)
            drawCircle(shade, sz(0.04f), Offset(cx, cy))
        }
        Gadget.SHIELD -> {
            val pulse = 0.5f + 0.5f * sin(t * 3f)
            drawCircle(hi.copy(alpha = 0.12f + 0.12f * pulse * (1 + stage) / 5f), sz(0.42f), Offset(x(0.5f), y(0.5f)))
            val p = Path().apply {
                moveTo(x(0.5f), y(0.14f)); lineTo(x(0.8f), y(0.26f)); lineTo(x(0.78f), y(0.55f))
                cubicTo(x(0.76f), y(0.72f), x(0.6f), y(0.82f), x(0.5f), y(0.88f))
                cubicTo(x(0.4f), y(0.82f), x(0.24f), y(0.72f), x(0.22f), y(0.55f)); lineTo(x(0.2f), y(0.26f)); close()
            }
            drawPath(p, body)
            drawPath(p, shade, style = Stroke(width = sz(0.03f)))
            drawLine(hi, Offset(x(0.5f), y(0.28f)), Offset(x(0.5f), y(0.72f)), sz(0.04f), StrokeCap.Round)
            drawLine(hi, Offset(x(0.34f), y(0.44f)), Offset(x(0.66f), y(0.44f)), sz(0.04f), StrokeCap.Round)
        }
        Gadget.COIN -> {
            val n = 2 + min(6, lv)
            val cw = sz(0.34f); val chh = sz(0.07f)
            for (i in 0 until n) {
                val ty = y(0.84f) - (i + 1) * chh * 1.1f
                val dx = if (i % 2 == 0) 0f else sz(0.02f)
                val drop = if (i == n - 1) flash * sz(0.2f) else 0f
                drawOval(dark(Color(0xFFE0A100), 0.25f), Offset(x(0.33f) + dx, ty + chh * 0.4f - drop), Size(cw, chh))
                drawOval(Color(0xFFFFD43B), Offset(x(0.33f) + dx, ty - drop), Size(cw, chh))
            }
            val sp = (t * 1.3f) % 1f
            drawCircle(Color.White.copy(alpha = 1f - sp), sz(0.03f), Offset(x(0.42f), y(0.4f) + sp * sz(0.1f)))
        }
        Gadget.CLOCK -> {
            val cx = x(0.5f); val cy = y(0.5f); val r = sz(0.34f)
            drawCircle(shade, r + sz(0.03f), Offset(cx, cy))
            drawCircle(Color(0xFFF6F0E4), r, Offset(cx, cy))
            for (i in 0 until 12) {
                val a = (PI / 6 * i).toFloat()
                drawLine(shade, Offset(cx + cos(a) * r * 0.82f, cy + sin(a) * r * 0.82f), Offset(cx + cos(a) * r * 0.94f, cy + sin(a) * r * 0.94f), sz(0.015f))
            }
            val ma = t * 1.6f * spin - PI.toFloat() / 2f
            val ha = t * 0.13f * spin - PI.toFloat() / 2f
            drawLine(body, Offset(cx, cy), Offset(cx + cos(ma) * r * 0.78f, cy + sin(ma) * r * 0.78f), sz(0.02f), StrokeCap.Round)
            drawLine(shade, Offset(cx, cy), Offset(cx + cos(ha) * r * 0.5f, cy + sin(ha) * r * 0.5f), sz(0.03f), StrokeCap.Round)
            drawCircle(shade, sz(0.025f), Offset(cx, cy))
        }
        Gadget.ANTENNA -> {
            drawLine(shade, Offset(x(0.5f), y(0.86f)), Offset(x(0.5f), y(0.3f)), sz(0.04f), StrokeCap.Round)
            drawLine(shade, Offset(x(0.38f), y(0.86f)), Offset(x(0.5f), y(0.5f)), sz(0.02f))
            drawLine(shade, Offset(x(0.62f), y(0.86f)), Offset(x(0.5f), y(0.5f)), sz(0.02f))
            drawCircle(body, sz(0.05f), Offset(x(0.5f), y(0.27f)))
            val waves = 2 + stage / 2
            for (i in 0 until waves) {
                val ph = ((t * 0.9f + i.toFloat() / waves) % 1f)
                drawCircle(body.copy(alpha = 0.7f * (1f - ph)), sz(0.06f + 0.34f * ph), Offset(x(0.5f), y(0.27f)), style = Stroke(width = sz(0.02f)))
            }
        }
        Gadget.CUP -> {
            val p = Path().apply {
                moveTo(x(0.25f), y(0.45f)); lineTo(x(0.7f), y(0.45f)); lineTo(x(0.66f), y(0.8f))
                cubicTo(x(0.64f), y(0.86f), x(0.31f), y(0.86f), x(0.29f), y(0.8f)); close()
            }
            drawPath(p, body)
            drawArc(shade, -90f, 180f, false, Offset(x(0.62f), y(0.5f)), Size(sz(0.2f), sz(0.2f)), style = Stroke(width = sz(0.04f)))
            drawOval(dark(body, 0.5f), Offset(x(0.25f), y(0.42f)), Size(sz(0.45f), sz(0.07f)))
            for (i in 0 until 2 + stage / 2) {
                val ph = ((t * 0.6f + i * 0.33f) % 1f)
                val cx = x(0.35f + i * 0.12f) + sin((ph * 8f + i).toDouble()).toFloat() * sz(0.03f)
                drawCircle(Color.White.copy(alpha = 0.8f * (1f - ph)), sz(0.03f + 0.03f * ph), Offset(cx, y(0.4f) - ph * sz(0.3f)))
            }
        }
        Gadget.PLOW -> {
            val sh = sin(t * 6f) * sz(0.01f)
            val p = Path().apply {
                moveTo(x(0.12f), y(0.84f) + sh); lineTo(x(0.88f), y(0.84f) + sh); lineTo(x(0.5f), y(0.22f) + sh); close()
            }
            drawPath(p, body)
            drawLine(hi, Offset(x(0.5f), y(0.36f)), Offset(x(0.5f), y(0.76f)), sz(0.04f), StrokeCap.Round)
            drawPath(p, shade, style = Stroke(width = sz(0.025f)))
            for (i in 0 until 3 + stage) {
                val ph = ((t * 1.4f + i * 0.27f) % 1f)
                val dir = if (i % 2 == 0) -1f else 1f
                drawCircle(Color.White.copy(alpha = 1f - ph), sz(0.025f), Offset(x(0.5f) + dir * ph * sz(0.38f), y(0.3f) + ph * sz(0.25f) - sin(ph * 3.14f) * sz(0.1f)))
            }
        }
        Gadget.COALPILE -> {
            val rows = listOf(0.28f to 0.76f, 0.5f to 0.76f, 0.72f to 0.76f, 0.39f to 0.6f, 0.61f to 0.6f, 0.5f to 0.44f)
            val n = 3 + min(3, stage)
            for (i in 0 until n) {
                val (cx, cy) = rows[i]
                drawCircle(Color(0xFF2B2B30), sz(0.12f), Offset(x(cx), y(cy + 0.06f)))
                drawCircle(Color(0xFF45454D), sz(0.045f), Offset(x(cx - 0.03f), y(cy + 0.03f)))
            }
            for (i in 0 until 3) {
                val tw = 0.5f + 0.5f * sin(t * 5f + i * 2f)
                drawCircle(Color(0xFFFF8A2B).copy(alpha = 0.4f + 0.5f * tw), sz(0.02f), Offset(x(0.35f + i * 0.15f), y(0.72f - 0.03f * i)))
            }
        }
        Gadget.ENGINE -> {
            drawRoundRect(body, Offset(x(0.14f), y(0.42f)), Size(sz(0.56f), sz(0.34f)), CornerRadius(sz(0.08f)))
            drawRect(shade, Offset(x(0.58f), y(0.3f)), Size(sz(0.2f), sz(0.46f)))
            drawRect(dark(shade), Offset(x(0.24f), y(0.28f)), Size(sz(0.1f), sz(0.16f)))
            for (i in 0 until 3) {
                val ph = ((t * 0.7f * spin + i * 0.33f) % 1f)
                drawCircle(Color.White.copy(alpha = 0.8f * (1f - ph)), sz(0.04f + 0.06f * ph), Offset(x(0.29f) + ph * sz(0.1f), y(0.26f) - ph * sz(0.28f)))
            }
            val cx = x(0.36f); val cy = y(0.78f)
            drawCircle(dark(shade), sz(0.12f), Offset(cx, cy))
            rotate(t * 200f * spin, Offset(cx, cy)) { drawLine(hi, Offset(cx - sz(0.1f), cy), Offset(cx + sz(0.1f), cy), sz(0.03f)) }
            drawCircle(dark(shade), sz(0.09f), Offset(x(0.66f), y(0.8f)))
            val rod = sin(t * 8f * spin) * sz(0.04f)
            drawLine(hi, Offset(x(0.36f) + rod, cy), Offset(x(0.66f), y(0.8f)), sz(0.025f))
        }
        Gadget.BED -> {
            drawRoundRect(Color(0xFF8A5A3B), Offset(x(0.12f), y(0.56f)), Size(sz(0.76f), sz(0.26f)), CornerRadius(sz(0.04f)))
            drawRoundRect(body, Offset(x(0.16f), y(0.5f)), Size(sz(0.68f), sz(0.2f)), CornerRadius(sz(0.04f)))
            drawRoundRect(Color(0xFFF6F0E4), Offset(x(0.18f), y(0.48f)), Size(sz(0.2f), sz(0.12f)), CornerRadius(sz(0.04f)))
            for (i in 0 until 3 + stage) {
                val ph = ((t * 0.5f + i * 0.3f) % 1f)
                drawCircle(Color(0xFFFFF3B0).copy(alpha = 1f - ph), sz(0.025f), Offset(x(0.55f + 0.05f * i), y(0.4f) - ph * sz(0.25f)))
            }
            drawArc(Color(0xFFFFE680), 40f, 260f, true, Offset(x(0.68f), y(0.12f)), Size(sz(0.18f), sz(0.18f)))
        }
    }

    // upgrade burst: a ring and sparks that fade out right after buying
    if (flash > 0.01f) {
        val cx = x(0.5f); val cy = y(0.5f)
        val grow = 1f - flash
        drawCircle(Color(0xFFFFE680).copy(alpha = flash * 0.8f), sz(0.15f + 0.4f * grow), Offset(cx, cy), style = Stroke(width = sz(0.04f) * flash))
        for (i in 0 until 8) {
            val a = (PI / 4 * i).toFloat() + 0.3f
            val d = sz(0.2f + 0.3f * grow)
            drawCircle(Color.White.copy(alpha = flash), sz(0.02f), Offset(cx + cos(a) * d, cy + sin(a) * d))
        }
    }
}
