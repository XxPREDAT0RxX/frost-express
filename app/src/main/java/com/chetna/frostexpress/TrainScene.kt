package com.chetna.frostexpress

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val FAR_PEAKS = floatArrayOf(0.9f, 0.55f, 1.0f, 0.7f, 0.5f, 0.85f)
private val MID_PEAKS = floatArrayOf(0.6f, 1.0f, 0.75f, 0.5f, 0.9f)
private val PINES = floatArrayOf(0.8f, 1.0f, 0.65f, 0.9f, 0.75f, 1.0f, 0.6f)

private val Dark = Color(0xFF2B2F36)
private val Window = Color(0xFFCFE9FF)

/** Frozen duniya + chalti train. worldX "dp" me chalta hai, speed ke hisaab se. */
@Composable
fun TrainScene(vm: GameViewModel, modifier: Modifier = Modifier) {
    var worldX by remember { mutableFloatStateOf(0f) }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                    time += dt
                    val dpPerSec = (60f + 45f * sqrt(vm.speed.toFloat())) * (if (vm.isBoosting) 1.5f else 1f)
                    worldX += dpPerSec * dt
                }
                last = now
            }
        }
    }

    val tier = TIERS[vm.tierIndex]
    Canvas(modifier) {
        drawScene(worldX, time, tier, vm.level(UpgradeType.WAGON), vm.isBoosting)
    }
}

private fun DrawScope.drawScene(worldX: Float, time: Float, tier: TrainTier, wagons: Int, boosting: Boolean) {
    val u = density
    val w = size.width
    val h = size.height
    val groundY = h * 0.66f
    val trackY = h * 0.80f

    // Aasmaan aur suraj
    drawRect(Color(0xFFD7E8F4), size = Size(w, groundY))
    drawCircle(Color(0xFFFFF6DA), 26f * u, Offset(w * 0.82f, h * 0.18f))

    // Pahad (parallax)
    drawRidge(worldX * u * 0.08f, groundY, 480f * u, h * 0.40f, FAR_PEAKS, Color(0xFFB9D0E1), Color(0xFFEAF3F9))
    drawRidge(worldX * u * 0.18f, groundY, 360f * u, h * 0.28f, MID_PEAKS, Color(0xFF9DB9CE), Color(0xFFE2EEF6))

    // Barf ki zameen
    drawRect(Color(0xFFEEF5FA), Offset(0f, groundY), Size(w, h - groundY))

    // Jame hue ped
    val spacing = 64f * u
    val pineOff = (worldX * u * 0.5f) % (spacing * PINES.size)
    var px = -pineOff
    var pi = 0
    while (px < w + spacing) {
        drawPine(px, groundY + 8f * u, PINES[pi % PINES.size] * h * 0.2f, u)
        px += spacing
        pi++
    }

    // Track
    drawRect(Color(0xFFC5D3DE), Offset(0f, trackY), Size(w, 12f * u))
    val tieGap = 26f * u
    var tx = -((worldX * u) % tieGap)
    while (tx < w) {
        drawRect(Color(0xFF7A6A5A), Offset(tx, trackY + 3f * u), Size(12f * u, 5f * u))
        tx += tieGap
    }
    drawRect(Color(0xFF3D4A57), Offset(0f, trackY - 2f * u), Size(w, 4f * u))
    drawRect(Color(0xFFF8FBFD), Offset(0f, trackY + 12f * u), Size(w, h - trackY - 12f * u))

    drawTrain(worldX, time, tier, wagons, boosting, trackY, w * 0.60f)

    // Boost speed lines
    if (boosting) {
        for (i in 0 until 7) {
            val y = groundY * 0.12f + i * groundY * 0.11f
            val x = (time * 900f * u + i * 173f * u) % (w + 200f * u)
            drawLine(Color.White.copy(alpha = 0.75f), Offset(w - x, y), Offset(w - x + 80f * u, y), 3f * u)
        }
    }

    // Barfbari
    for (i in 0 until 40) {
        val fx = ((i * 0.6180339f) % 1f) * w
        val fy = ((i * 0.4142136f) % 1f) * h
        val sx = (((fx - worldX * u * 0.25f) % w) + w) % w
        val sy = (fy + time * 30f * u + i * 7f) % h
        drawCircle(Color.White.copy(alpha = 0.9f), (1.5f + (i % 3)) * u, Offset(sx, sy))
    }
}

private fun DrawScope.drawRidge(
    offsetPx: Float, baseY: Float, tilePx: Float, maxH: Float,
    heights: FloatArray, color: Color, cap: Color,
) {
    val step = tilePx / heights.size
    var x = -(offsetPx % tilePx) + step * 0.5f
    var i = 0
    while (x < size.width + step) {
        val ph = heights[i % heights.size] * maxH
        val half = step * 0.8f
        val body = Path().apply {
            moveTo(x - half, baseY); lineTo(x, baseY - ph); lineTo(x + half, baseY); close()
        }
        drawPath(body, color)
        val capY = baseY - ph + ph * 0.25f
        val capHalf = half * 0.25f
        val snow = Path().apply {
            moveTo(x - capHalf, capY); lineTo(x, baseY - ph); lineTo(x + capHalf, capY); close()
        }
        drawPath(snow, cap)
        x += step
        i++
    }
}

private fun DrawScope.drawPine(x: Float, baseY: Float, height: Float, u: Float) {
    drawRect(Color(0xFF6B5B4B), Offset(x - 2f * u, baseY - 6f * u), Size(4f * u, 6f * u))
    val tree = Path().apply {
        moveTo(x - 14f * u, baseY - 6f * u); lineTo(x, baseY - 6f * u - height); lineTo(x + 14f * u, baseY - 6f * u); close()
    }
    drawPath(tree, Color(0xFF4E7387))
    val topY = baseY - 6f * u - height
    val snow = Path().apply {
        moveTo(x - 5f * u, topY + height * 0.3f); lineTo(x, topY); lineTo(x + 5f * u, topY + height * 0.3f); close()
    }
    drawPath(snow, Color(0xFFF2F8FC))
}

private fun DrawScope.drawWheel(cx: Float, cy: Float, r: Float, angle: Float, hub: Color, u: Float) {
    drawCircle(Dark, r, Offset(cx, cy))
    drawCircle(hub, r * 0.35f, Offset(cx, cy))
    for (k in 0 until 3) {
        val a = angle + k * 1.0471976f
        val dx = cos(a) * r * 0.85f
        val dy = sin(a) * r * 0.85f
        drawLine(Color(0xFFB9C4CE), Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), 1.6f * u)
    }
}

private fun DrawScope.drawTrain(
    worldX: Float, time: Float, tier: TrainTier, wagons: Int, boosting: Boolean,
    trackY: Float, locoX: Float,
) {
    val u = density
    val body = Color(tier.body)
    val accent = Color(tier.accent)
    val locoW = 80f * u
    val wheelR = 10f * u
    val wheelCy = trackY - 2f * u - wheelR
    val chassisTop = wheelCy - 4f * u
    val boilerTop = chassisTop - 28f * u

    // Wagons (jitne screen pe fit ho)
    val maxFit = ((locoX - 6f * u) / (58f * u)).toInt()
    val shown = minOf(wagons, maxFit)
    for (i in 0 until shown) {
        val wx = locoX - (i + 1) * 58f * u
        drawRoundRect(body, Offset(wx, chassisTop - 30f * u), Size(52f * u, 30f * u), CornerRadius(4f * u))
        drawRect(accent, Offset(wx, chassisTop - 10f * u), Size(52f * u, 5f * u))
        for (k in 0 until 3) {
            drawRect(Window, Offset(wx + 6f * u + k * 15f * u, chassisTop - 25f * u), Size(10f * u, 9f * u))
        }
        drawRect(Dark, Offset(wx, chassisTop), Size(52f * u, 6f * u))
        drawRect(Dark, Offset(wx + 52f * u, chassisTop + 1f * u), Size(6f * u, 3f * u))
        val r = 7f * u
        drawWheel(wx + 10f * u, trackY - 2f * u - r, r, worldX / 7f, accent, u)
        drawWheel(wx + 42f * u, trackY - 2f * u - r, r, worldX / 7f, accent, u)
    }

    // Engine
    drawRect(Dark, Offset(locoX, chassisTop), Size(locoW, 6f * u))
    drawRoundRect(body, Offset(locoX + 30f * u, boilerTop), Size(50f * u, 28f * u), CornerRadius(6f * u))
    drawRoundRect(body, Offset(locoX, chassisTop - 42f * u), Size(32f * u, 42f * u), CornerRadius(3f * u))
    drawRect(accent, Offset(locoX - 3f * u, chassisTop - 46f * u), Size(38f * u, 5f * u))
    drawRect(Window, Offset(locoX + 8f * u, chassisTop - 34f * u), Size(16f * u, 14f * u))
    drawRect(Dark, Offset(locoX + 62f * u, boilerTop - 12f * u), Size(9f * u, 12f * u))
    drawRect(accent, Offset(locoX + 60f * u, boilerTop - 15f * u), Size(13f * u, 4f * u))
    drawCircle(accent, 6f * u, Offset(locoX + 46f * u, boilerTop))
    val cowcatcher = Path().apply {
        moveTo(locoX + locoW - 4f * u, chassisTop + 6f * u)
        lineTo(locoX + locoW + 14f * u, trackY - 3f * u)
        lineTo(locoX + locoW - 4f * u, trackY - 3f * u)
        close()
    }
    drawPath(cowcatcher, Color(0xFF3D4A57))

    val lampX = locoX + locoW
    val lampY = boilerTop + 10f * u
    if (boosting) {
        val beam = Path().apply {
            moveTo(lampX, lampY); lineTo(lampX + 120f * u, lampY - 14f * u); lineTo(lampX + 120f * u, lampY + 22f * u); close()
        }
        drawPath(beam, Color(0xFFFFE680).copy(alpha = 0.3f))
    }
    drawCircle(Color(0xFFFFE680), 4f * u, Offset(lampX, lampY))

    drawWheel(locoX + 16f * u, wheelCy, wheelR, worldX / 10f, accent, u)
    drawWheel(locoX + 42f * u, wheelCy, wheelR, worldX / 10f, accent, u)
    drawWheel(locoX + 66f * u, wheelCy, wheelR, worldX / 10f, accent, u)

    // Dhuan
    val cx = locoX + 66.5f * u
    val cy = boilerTop - 15f * u
    for (i in 0 until 5) {
        val p = (time * 0.7f + i * 0.2f) % 1f
        drawCircle(
            Color(0xFF8FA3B3).copy(alpha = (1f - p) * 0.75f),
            (5f + p * 10f) * u,
            Offset(cx - p * 90f * u, cy - p * 46f * u),
        )
    }
    if (boosting) {
        val flicker = 1f + 0.2f * sin(time * 30f)
        drawCircle(Color(0xFFFF9F43).copy(alpha = 0.55f), 9f * u * flicker, Offset(cx, cy + 2f * u))
    }
}
