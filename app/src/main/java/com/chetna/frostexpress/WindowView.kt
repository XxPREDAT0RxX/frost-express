package com.chetna.frostexpress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import kotlin.math.sin

/** A biome colour set for the "looking out the window" side view. */
private class ViewPalette(
    val skyTop: Long, val skyBottom: Long,
    val farHill: Long, val nearHill: Long,
    val ground: Long, val tree: Long, val treeDark: Long,
    val snowy: Boolean = false, val desert: Boolean = false,
)

// Order matches ZONES in TrainScene: meadow, pine woods, savanna, desert, rainforest, snowfields.
private val VIEW_PALETTES = listOf(
    ViewPalette(0xFF8FD0F0, 0xFFD6F1FF, 0xFF9FD08A, 0xFF6FB05C, 0xFFAFD98C, 0xFF5BA34C, 0xFF3E7A36),
    ViewPalette(0xFF86C2E8, 0xFFCDEAF6, 0xFF6E9E63, 0xFF4E7E48, 0xFF93C47D, 0xFF3E6B3A, 0xFF2C4E2A),
    ViewPalette(0xFFF6D98A, 0xFFFBEFC8, 0xFFCDB06A, 0xFFB89A52, 0xFFE6CE86, 0xFF8A7A3C, 0xFF6E5E2C, desert = true),
    ViewPalette(0xFFF4C58A, 0xFFFBE6C0, 0xFFD6A368, 0xFFC08A52, 0xFFE7BC89, 0xFF7FA85E, 0xFF5E7E3E, desert = true),
    ViewPalette(0xFFA7D8C0, 0xFFDAF2E6, 0xFF5E9E78, 0xFF3E7E58, 0xFF86C58F, 0xFF2E6E4A, 0xFF1E4E33),
    ViewPalette(0xFFB8D0E0, 0xFFEAF4FB, 0xFFCAD8E4, 0xFFAFC2D2, 0xFFDCE6EE, 0xFF4E6E5A, 0xFF36503F, snowy = true),
)

/** Full-screen "look out the window" view: parallax scenery that scrolls with the train's speed. */
@Composable
fun WindowView(vm: GameViewModel, modifier: Modifier = Modifier) {
    var scroll by remember { mutableFloatStateOf(0f) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                t += dt
                // scenery moves with the real train speed; stopped at a station → it halts
                scroll += (vm.speed.toFloat().coerceIn(0f, 80f)) * dt * 26f
            }
        }
    }

    val zi = vm.zone.coerceIn(0, VIEW_PALETTES.lastIndex)
    val pal = VIEW_PALETTES[zi]

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // --- the view through the glass ---
        drawRect(Brush.verticalGradient(listOf(Color(pal.skyTop), Color(pal.skyBottom)), startY = 0f, endY = h * 0.62f))

        // sun / soft glow, drifting slowly
        val sunX = w * (0.72f - 0.04f * sin(t * 0.1f))
        val sunY = h * 0.16f
        if (pal.snowy || pal.desert) {
            drawCircle(Color(0x33FFFFFF), h * 0.14f, Offset(sunX, sunY))
            drawCircle(Color(0xFFFFF6D6), h * 0.055f, Offset(sunX, sunY))
        } else {
            drawCircle(Color(0x44FFF3C4), h * 0.12f, Offset(sunX, sunY))
            drawCircle(Color(0xFFFFEFA8), h * 0.05f, Offset(sunX, sunY))
        }

        // drifting clouds (skip in the desert for a clear sky)
        if (!pal.desert) {
            val cloud = Color(if (pal.snowy) 0xCCFFFFFF else 0xBBFFFFFF)
            for (k in 0 until 3) {
                val cx = ((t * 8f + k * 260f) % (w + 240f)) - 120f
                val cy = h * (0.10f + 0.07f * k)
                drawCloud(cx, cy, 1f - k * 0.12f, cloud)
            }
        }

        val horizon = h * 0.60f

        // far hills (slow parallax)
        drawHills(scroll * 0.12f, horizon, w, amp = h * 0.05f, len = 520f, Color(pal.farHill))
        // near hills (faster)
        drawHills(scroll * 0.3f, horizon + h * 0.03f, w, amp = h * 0.075f, len = 380f, Color(pal.nearHill))

        // ground band
        drawRect(Color(pal.ground), topLeft = Offset(0f, horizon + h * 0.02f), size = Size(w, h - horizon))

        // mid-ground trees, repeating with a pitch, scrolling at near-train speed
        val pitch = 230f
        val base = horizon + h * 0.04f
        var i = -1
        val n = (w / pitch).toInt() + 3
        while (i < n) {
            val phase = (scroll * 0.6f) % pitch
            val x = i * pitch - phase + 40f
            val jitter = sin((i * 73).toFloat()) * 18f
            drawBiomeTree(x, base + jitter, pal)
            i++
        }

        // foreground: fast ground strip + fence posts whipping past
        val fg = h * 0.86f
        drawRect(
            Brush.verticalGradient(listOf(Color(pal.ground), darken(Color(pal.ground), 0.82f)), startY = fg, endY = h),
            topLeft = Offset(0f, fg), size = Size(w, h - fg),
        )
        val postPitch = 300f
        val pn = (w / postPitch).toInt() + 3
        var p = -1
        while (p < pn) {
            val phaseP = (scroll * 1.0f) % postPitch
            val x = p * postPitch - phaseP + 60f
            drawRect(Color(0xFF8A6A4A), topLeft = Offset(x, fg - h * 0.06f), size = Size(6f, h * 0.06f))
            drawRect(Color(0xFF6E5238), topLeft = Offset(x, fg - h * 0.06f), size = Size(6f, 5f))
            p++
        }

        // --- the window frame on top of the view ---
        drawWindowFrame(w, h)
    }
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, s: Float, c: Color) {
    val r = size.height * 0.035f * s
    drawCircle(c, r, Offset(cx, cy))
    drawCircle(c, r * 1.25f, Offset(cx + r * 1.1f, cy + r * 0.15f))
    drawCircle(c, r * 0.95f, Offset(cx + r * 2.2f, cy))
    drawCircle(c, r * 1.1f, Offset(cx + r * 1.4f, cy - r * 0.5f))
}

private fun DrawScope.drawHills(shift: Float, y: Float, w: Float, amp: Float, len: Float, c: Color) {
    val path = Path()
    path.moveTo(0f, size.height)
    var x = 0f
    val off = shift % len
    while (x <= w) {
        val yy = y - amp * (0.5f + 0.5f * sin(((x + off) / len) * 2f * Math.PI.toFloat()))
        path.lineTo(x, yy)
        x += 12f
    }
    path.lineTo(w, size.height)
    path.close()
    drawPath(path, c)
}

private fun DrawScope.drawBiomeTree(x: Float, baseY: Float, pal: ViewPalette) {
    val h = size.height
    when {
        pal.snowy -> { // snowy pine
            val th = h * 0.16f
            drawRect(Color(0xFF6E5238), topLeft = Offset(x - 4f, baseY - th * 0.2f), size = Size(8f, th * 0.2f))
            val tri = Path().apply {
                moveTo(x, baseY - th); lineTo(x - th * 0.42f, baseY - th * 0.15f); lineTo(x + th * 0.42f, baseY - th * 0.15f); close()
            }
            drawPath(tri, Color(pal.tree))
            val cap = Path().apply {
                moveTo(x, baseY - th); lineTo(x - th * 0.22f, baseY - th * 0.55f); lineTo(x + th * 0.22f, baseY - th * 0.55f); close()
            }
            drawPath(cap, Color(0xFFFFFFFF))
        }
        pal.desert -> { // cactus
            val ch = h * 0.12f
            val col = Color(pal.tree)
            drawRoundRect(col, topLeft = Offset(x - 7f, baseY - ch), size = Size(14f, ch), cornerRadius = CornerRadius(7f, 7f))
            drawRoundRect(col, topLeft = Offset(x - 18f, baseY - ch * 0.75f), size = Size(11f, ch * 0.4f), cornerRadius = CornerRadius(5f, 5f))
            drawRoundRect(col, topLeft = Offset(x - 18f, baseY - ch * 0.75f), size = Size(6f, ch * 0.55f), cornerRadius = CornerRadius(3f, 3f))
            drawRoundRect(col, topLeft = Offset(x + 7f, baseY - ch * 0.62f), size = Size(11f, ch * 0.34f), cornerRadius = CornerRadius(5f, 5f))
            drawRoundRect(col, topLeft = Offset(x + 12f, baseY - ch * 0.62f), size = Size(6f, ch * 0.45f), cornerRadius = CornerRadius(3f, 3f))
        }
        else -> { // leafy tree
            val th = h * 0.15f
            drawRect(Color(0xFF7A5638), topLeft = Offset(x - 5f, baseY - th * 0.35f), size = Size(10f, th * 0.35f))
            drawCircle(Color(pal.treeDark), th * 0.34f, Offset(x - th * 0.18f, baseY - th * 0.55f))
            drawCircle(Color(pal.tree), th * 0.4f, Offset(x + th * 0.12f, baseY - th * 0.6f))
            drawCircle(Color(pal.tree), th * 0.3f, Offset(x - th * 0.22f, baseY - th * 0.75f))
        }
    }
}

/** The train carriage window the player looks through: twin panes split by a pillar, gasket frame,
 *  luggage-rack valance above and a sill ledge below, all set in the coach's inner wall. */
private fun DrawScope.drawWindowFrame(w: Float, h: Float) {
    val wall = Color(0xFFEADFC8)       // warm cream coach wall
    val wallLo = Color(0xFFD8C8A6)      // lower wainscot band
    val gasket = Color(0xFF3B4653)      // dark rubber/metal window seal
    val frame = Color(0xFF6B7684)       // metal frame
    val sill = Color(0xFFBF9A5E)
    val sillHi = Color(0xFFD8B87A)

    val mx = w * 0.055f
    val top = h * 0.145f
    val bottom = h * 0.72f
    val r = w * 0.045f
    val pillarHW = w * 0.028f           // half-width of the centre pillar between the two panes
    val cx = w / 2f
    val leftR = Rect(mx, top, cx - pillarHW, bottom)
    val rightR = Rect(cx + pillarHW, top, w - mx, bottom)

    // carve the two glass panes out of a full-screen wall
    val panes = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(leftR, CornerRadius(r, r)))
        addRoundRect(androidx.compose.ui.geometry.RoundRect(rightR, CornerRadius(r, r)))
    }
    clipPath(panes, clipOp = ClipOp.Difference) {
        drawRect(wall)
        // lower wainscot: a slightly darker band along the bottom wall
        drawRect(wallLo, topLeft = Offset(0f, h * 0.82f), size = Size(w, h * 0.18f))
        drawRect(Color(0x22000000), topLeft = Offset(0f, h * 0.82f), size = Size(w, 3f))
        // soft top shade
        drawRect(Brush.verticalGradient(listOf(Color(0x33000000), Color.Transparent), startY = 0f, endY = h * 0.14f))
    }

    // glass sheen + a faint interior reflection, per pane
    for (pane in listOf(leftR, rightR)) {
        clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(pane, CornerRadius(r, r))) }) {
            val sheen = Path().apply {
                moveTo(pane.left, pane.top + pane.height * 0.12f)
                lineTo(pane.left + pane.width * 0.42f, pane.top)
                lineTo(pane.left + pane.width * 0.62f, pane.top)
                lineTo(pane.left, pane.top + pane.height * 0.5f)
                close()
            }
            drawPath(sheen, Color(0x1FFFFFFF))
        }
    }

    // gasket + metal frame around each pane
    for (pane in listOf(leftR, rightR)) {
        drawRoundRect(gasket, topLeft = Offset(pane.left - w * 0.012f, pane.top - w * 0.012f),
            size = Size(pane.width + w * 0.024f, pane.height + w * 0.024f),
            cornerRadius = CornerRadius(r + 4f, r + 4f), style = Stroke(width = w * 0.022f))
        drawRoundRect(frame, topLeft = Offset(pane.left, pane.top), size = Size(pane.width, pane.height),
            cornerRadius = CornerRadius(r, r), style = Stroke(width = w * 0.009f))
    }

    // centre pillar between the panes, with a couple of rivets
    drawRoundRect(wall, topLeft = Offset(cx - pillarHW, top - w * 0.02f), size = Size(pillarHW * 2, bottom - top + w * 0.04f), cornerRadius = CornerRadius(4f, 4f))
    drawRect(Color(0x22000000), topLeft = Offset(cx - pillarHW, top), size = Size(2f, bottom - top))
    drawRect(Color(0x33FFFFFF), topLeft = Offset(cx + pillarHW - 2f, top), size = Size(2f, bottom - top))
    drawCircle(Color(0x55000000), 2.5f, Offset(cx, top + h * 0.03f))
    drawCircle(Color(0x55000000), 2.5f, Offset(cx, bottom - h * 0.03f))

    // luggage-rack valance: a dark bar across the top, with small brackets
    val vy = top - h * 0.055f
    drawRoundRect(Color(0xFFB7A07A), topLeft = Offset(mx - w * 0.02f, vy), size = Size(w - 2 * (mx - w * 0.02f), h * 0.03f), cornerRadius = CornerRadius(6f, 6f))
    drawRect(Color(0x33000000), topLeft = Offset(mx - w * 0.02f, vy + h * 0.03f - 3f), size = Size(w - 2 * (mx - w * 0.02f), 3f))
    drawRect(Color(0xFF9A835E), topLeft = Offset(mx + w * 0.04f, vy + h * 0.03f), size = Size(5f, h * 0.02f))
    drawRect(Color(0xFF9A835E), topLeft = Offset(w - mx - w * 0.04f, vy + h * 0.03f), size = Size(5f, h * 0.02f))

    // the sill ledge below the panes
    val sy = bottom + w * 0.012f
    drawRect(sill, topLeft = Offset(mx - w * 0.03f, sy), size = Size(w - 2 * (mx - w * 0.03f), h * 0.045f))
    drawRect(sillHi, topLeft = Offset(mx - w * 0.03f, sy), size = Size(w - 2 * (mx - w * 0.03f), 5f))
    drawRect(Color(0x33000000), topLeft = Offset(mx - w * 0.03f, sy + h * 0.045f - 5f), size = Size(w - 2 * (mx - w * 0.03f), 5f))

    // a tiny potted plant on the sill corner, for cosiness
    val px = w - mx - w * 0.08f
    val py = sy
    drawRoundRect(Color(0xFFCB6D4F), topLeft = Offset(px, py - h * 0.028f), size = Size(w * 0.045f, h * 0.03f), cornerRadius = CornerRadius(4f, 4f))
    drawCircle(Color(0xFF5BA34C), w * 0.026f, Offset(px + w * 0.022f, py - h * 0.04f))
    drawCircle(Color(0xFF6FB85C), w * 0.02f, Offset(px + w * 0.008f, py - h * 0.052f))
    drawCircle(Color(0xFF6FB85C), w * 0.018f, Offset(px + w * 0.036f, py - h * 0.05f))
}

private fun darken(c: Color, f: Float) = lerp(c, Color.Black, 1f - f)
