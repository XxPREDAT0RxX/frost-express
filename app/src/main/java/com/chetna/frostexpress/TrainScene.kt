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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ---------------------------------------------------------------------------------------------
// Top-down cutaway of the train. All sizes below are in dp, measured in "train space":
// x runs across the car (0..CAR_W), y runs from the nose (0) back along the cars.
// ---------------------------------------------------------------------------------------------
private const val CAR_W = 190f
private const val CAR_H = 116f
private const val GAP = 10f
private const val PITCH = CAR_H + GAP
private const val NOSE = 26f
private const val AISLE_X = 95f
private const val MAX_CARGO = 8
private const val PI_F = 3.1415927f
private const val DECOR_PERIOD = 420f

private enum class Act { WALK, SHOVEL_PICK, SHOVEL_DROP, DRIVE, TURN_VALVE, COOK, EAT, CRATE_PICK, CRATE_PLACE, SLEEP }

/** One stop in the worker's routine: stand at (x, y), face (fx, fy), do [act] for [dur] seconds. */
private class Step(val x: Float, val y: Float, val fx: Float, val fy: Float, val act: Act, val dur: Float)

/** Which upgrades are installed, so the interior can show them. */
private class Look(
    val heat: Int,
    val food: Int,
    val coal: Int,
    val booster: Int,
    val conductor: Int,
    val plow: Int,
    val cargo: Int,
)

private class Decor(val side: Int, val fx: Float, val y: Float, val r: Float, val kind: Int)

private val DECOR = List(16) { i ->
    Decor(i % 2, (i * 0.37f + 0.11f) % 1f, (i * 27.3f) % DECOR_PERIOD, 7f + ((i * 5) % 9), i % 3)
}

private fun carY(car: Int, localY: Float): Float = NOSE + car * PITCH + localY

/** The worker visits every installed station in turn. Positions are car-local, converted by carY. */
private fun buildCycle(heat: Boolean, food: Boolean, bed: Boolean, cargo: Int): List<Step> {
    val s = ArrayList<Step>()
    s.add(Step(112f, carY(0, 28f), 1f, 0f, Act.SHOVEL_PICK, 1.0f))
    s.add(Step(88f, carY(0, 34f), -1f, 0f, Act.SHOVEL_DROP, 1.1f))
    s.add(Step(112f, carY(0, 28f), 1f, 0f, Act.SHOVEL_PICK, 1.0f))
    s.add(Step(88f, carY(0, 34f), -1f, 0f, Act.SHOVEL_DROP, 1.1f))
    s.add(Step(112f, carY(0, 94f), 1f, 0f, Act.DRIVE, 2.2f))
    if (heat) s.add(Step(74f, carY(1, 34f), -1f, 0f, Act.TURN_VALVE, 2.4f))
    if (food) {
        s.add(Step(124f, carY(1, 30f), 1f, 0f, Act.COOK, 2.6f))
        s.add(Step(76f, carY(1, 92f), -1f, 0f, Act.EAT, 2.4f))
    }
    for (w in 0 until cargo) {
        val c = 2 + w
        s.add(Step(72f, carY(c, 34f), -1f, 0f, Act.CRATE_PICK, 1.0f))
        s.add(Step(118f, carY(c, 84f), 1f, 0f, Act.CRATE_PLACE, 1.1f))
    }
    if (bed) s.add(Step(68f, carY(0, 94f), -1f, 0f, Act.SLEEP, 2.4f))
    return s
}

/** Animation-only state for the worker (not part of the game rules). */
private class Npc {
    var x = AISLE_X
    var y = NOSE + 70f
    var faceDeg = 0f
    var phase = 0f
    var actT = 0f
    var camY = 0f
    var viewH = 600f // visible height in train-space dp, set from the canvas every frame
    var trainLen = NOSE + 2 * PITCH
    var working = false
    var cycle: List<Step> = emptyList()
    var idx = 0
    var sig = Int.MIN_VALUE
    private val path = ArrayList<Offset>()

    val step: Step? get() = if (cycle.isEmpty()) null else cycle[idx % cycle.size]
    val act: Act get() = if (working) (step?.act ?: Act.WALK) else Act.WALK
    val carIdx: Int get() = max(0, ((y - NOSE) / PITCH).toInt())

    val carrying: Boolean
        get() {
            val st = step ?: return false
            return st.act == Act.CRATE_PLACE && (!working || actT < st.dur * 0.5f)
        }

    val holdingCoal: Boolean
        get() {
            val st = step ?: return false
            return st.act == Act.SHOVEL_DROP && (!working || actT < st.dur * 0.6f)
        }

    fun sync(heat: Boolean, food: Boolean, bed: Boolean, cargo: Int) {
        val s = (if (heat) 1 else 0) + (if (food) 2 else 0) + (if (bed) 4 else 0) + cargo * 8
        if (s == sig) return
        sig = s
        cycle = buildCycle(heat, food, bed, cargo)
        trainLen = NOSE + (2 + cargo) * PITCH - GAP + 10f
        if (y > trainLen - 20f) y = trainLen - 20f
        idx = 0
        plan()
    }

    private fun plan() {
        path.clear()
        working = false
        actT = 0f
        val t = step ?: return
        if (abs(x - AISLE_X) > 1f) path.add(Offset(AISLE_X, y))
        if (abs(y - t.y) > 1f) path.add(Offset(AISLE_X, t.y))
        path.add(Offset(t.x, t.y))
    }

    private fun turnTo(target: Float, d: Float) {
        var diff = (target - faceDeg) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        faceDeg += diff * min(1f, d * 12f)
    }

    fun update(dt: Float, tempo: Float) {
        val st = step ?: return
        val d = dt * tempo
        if (!working && path.isNotEmpty()) {
            val tgt = path[0]
            val dx = tgt.x - x
            val dy = tgt.y - y
            val dist = sqrt(dx * dx + dy * dy)
            val stride = 64f * d
            if (dist <= stride) {
                x = tgt.x
                y = tgt.y
                path.removeAt(0)
            } else {
                x += dx / dist * stride
                y += dy / dist * stride
            }
            if (dist > 0.01f) turnTo(atan2(dx, -dy) * 57.29578f, d)
            phase += d * 9f
            if (path.isEmpty()) {
                working = true
                actT = 0f
            }
        } else if (working) {
            turnTo(atan2(st.fx, -st.fy) * 57.29578f, d)
            actT += d
            phase += d * 6f
            if (actT >= st.dur) {
                idx = (idx + 1) % cycle.size
                plan()
            }
        }
        val target = (y - viewH / 2f).coerceIn(0f, max(0f, trainLen - viewH))
        camY += (target - camY) * min(1f, dt * 3f)
    }
}

@Composable
fun TrainScene(vm: GameViewModel, modifier: Modifier = Modifier) {
    val npc = remember { Npc() }
    var time by remember { mutableFloatStateOf(0f) }
    var scroll by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                    val boosting = vm.isBoosting
                    time += dt
                    scroll += (40f + 26f * sqrt(vm.speed.toFloat())) * (if (boosting) 1.6f else 1f) * dt
                    npc.sync(
                        heat = vm.level(UpgradeType.HEAT) > 0,
                        food = vm.level(UpgradeType.FOOD) > 0,
                        bed = vm.level(UpgradeType.CONDUCTOR) > 0,
                        cargo = min(vm.level(UpgradeType.WAGON), MAX_CARGO),
                    )
                    npc.update(dt, if (boosting) 1.8f else 1f)
                }
                last = now
            }
        }
    }

    val tier = TIERS[vm.tierIndex]
    Canvas(modifier) {
        val look = Look(
            heat = vm.level(UpgradeType.HEAT),
            food = vm.level(UpgradeType.FOOD),
            coal = vm.level(UpgradeType.COAL),
            booster = vm.level(UpgradeType.BOOSTER),
            conductor = vm.level(UpgradeType.CONDUCTOR),
            plow = vm.level(UpgradeType.PLOW),
            cargo = min(vm.level(UpgradeType.WAGON), MAX_CARGO),
        )
        drawScene(npc, time, scroll, tier, look, vm.isBoosting)
    }
}

// ---------------------------------------------------------------------------------------------
// Drawing helpers (everything takes dp and converts with the screen density)
// ---------------------------------------------------------------------------------------------
private fun DrawScope.box(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 0f) {
    val u = density
    if (r > 0f) {
        drawRoundRect(color, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(r * u, r * u))
    } else {
        drawRect(color, Offset(x * u, y * u), Size(w * u, h * u))
    }
}

private fun DrawScope.dot(color: Color, x: Float, y: Float, r: Float) {
    drawCircle(color, r * density, Offset(x * density, y * density))
}

private fun DrawScope.line(color: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float) {
    val u = density
    drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), w * u)
}

/** Dashed outline that marks an empty slot where an upgrade will be installed. */
private fun DrawScope.slot(x: Float, y: Float, w: Float, h: Float) {
    val u = density
    drawRoundRect(
        Color(0x668A949E),
        Offset(x * u, y * u),
        Size(w * u, h * u),
        CornerRadius(6f * u, 6f * u),
        style = Stroke(width = 2f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * u, 6f * u), 0f)),
    )
}

private fun DrawScope.drawScene(npc: Npc, time: Float, scroll: Float, tier: TrainTier, look: Look, boosting: Boolean) {
    val u = density
    val w = size.width
    val h = size.height
    val z = (w / u * 0.50f / CAR_W).coerceIn(0.8f, 1.5f)
    npc.viewH = h / u / z
    val trainPx = CAR_W * u * z
    val originX = (w - trainPx) / 2f
    val centerOff = max(0f, (npc.viewH - npc.trainLen) / 2f)
    drawRect(Color(0xFFE6EFF6), size = size)
    drawDecor(scroll, originX, trainPx, h, u)
    translate(left = originX, top = (centerOff - npc.camY) * u * z) {
        scale(scaleX = z, scaleY = z, pivot = Offset.Zero) {
            drawTrack(scroll, npc.trainLen)
            drawTrain(tier, look, npc, time, boosting)
            drawNpc(npc)
        }
    }
    drawSnowfall(time, w, h, u)
    if (boosting) drawSpeedLines(time, originX, trainPx, w, h, u)
}

private fun DrawScope.drawDecor(scroll: Float, originX: Float, trainPx: Float, h: Float, u: Float) {
    if (originX < 24f * u) return
    for (d in DECOR) {
        val r = d.r * u
        val span = max(0f, originX - 2f * r)
        val x = if (d.side == 0) r + d.fx * span else originX + trainPx + r + d.fx * span
        val base = (d.y + scroll) % DECOR_PERIOD
        var k = -1
        while (true) {
            val y = (base + k * DECOR_PERIOD) * u
            if (y > h + r) break
            if (y > -r) drawDecorItem(d.kind, x, y, r)
            k++
        }
    }
}

private fun DrawScope.drawDecorItem(kind: Int, x: Float, y: Float, r: Float) {
    if (kind == 2) {
        drawOval(Color(0xFFB8C4CE), Offset(x - r, y - r * 0.7f), Size(2f * r, 1.4f * r))
        drawOval(Color(0xFFD7E0E7), Offset(x - r * 0.6f, y - r * 0.5f), Size(1.2f * r, 0.8f * r))
    } else {
        drawCircle(Color(0xFF3F6B7A), r, Offset(x, y))
        drawCircle(Color(0xFF5D8FA0), r * 0.68f, Offset(x, y))
        drawCircle(Color(0xFFF2F8FC), r * 0.3f, Offset(x - r * 0.1f, y - r * 0.1f))
    }
}

private fun DrawScope.drawTrack(scroll: Float, len: Float) {
    val cx = CAR_W / 2f
    box(Color(0xFF9AA7B3), cx - 26f, -900f, 52f, len + 1800f)
    val off = scroll % 14f
    var y = -902f + off
    while (y < len + 900f) {
        box(Color(0xFF6B5B4B), cx - 24f, y, 48f, 4f)
        y += 14f
    }
    box(Color(0xFF3D4A57), cx - 15f, -900f, 3f, len + 1800f)
    box(Color(0xFF3D4A57), cx + 12f, -900f, 3f, len + 1800f)
}

private fun DrawScope.drawTrain(tier: TrainTier, look: Look, npc: Npc, time: Float, boosting: Boolean) {
    val body = Color(tier.body)
    val accent = Color(tier.accent)
    val cars = 2 + look.cargo
    drawNose(body, accent, look.plow)
    for (i in 0 until cars) {
        val top = NOSE + i * PITCH
        val floor: Color
        val plank: Color
        when (i) {
            0 -> { floor = Color(0xFF98A3AD); plank = Color(0xFF8A949E) }
            1 -> { floor = Color(0xFFE3DCCB); plank = Color(0xFFD3CBB8) }
            else -> { floor = Color(0xFFC49A6C); plank = Color(0xFFAF8559) }
        }
        drawCarShell(top, body, accent, floor, plank)
        when (i) {
            0 -> drawEngineCar(top, look, npc, time, boosting)
            1 -> drawUtilityCar(top, look, npc, time)
            else -> drawCargoCar(top, i, npc)
        }
        if (i < cars - 1) drawVestibule(top + CAR_H)
    }
}

private fun DrawScope.drawNose(body: Color, accent: Color, plow: Int) {
    val u = density
    val p = Path().apply {
        moveTo(16f * u, (NOSE + 2f) * u)
        lineTo(46f * u, 4f * u)
        lineTo(144f * u, 4f * u)
        lineTo(174f * u, (NOSE + 2f) * u)
        close()
    }
    drawPath(p, body)
    dot(Color(0xFFFFE680), 56f, 15f, 5f)
    dot(Color(0xFFFFE680), 134f, 15f, 5f)
    box(accent, 70f, 10f, 50f, 4f, 2f)
    if (plow > 0) box(Color(0xFF9FD8F0), 38f, 0f, 114f, 6f, 3f)
}

private fun DrawScope.drawCarShell(top: Float, body: Color, accent: Color, floor: Color, plank: Color) {
    box(Color(0x33000000), 3f, top + 4f, CAR_W, CAR_H, 12f)
    box(body, 0f, top, CAR_W, CAR_H, 12f)
    box(floor, 7f, top + 7f, CAR_W - 14f, CAR_H - 14f, 4f)
    var y = top + 19f
    while (y < top + CAR_H - 8f) {
        box(plank, 7f, y, CAR_W - 14f, 1.2f)
        y += 12f
    }
    var wy = top + 14f
    while (wy < top + CAR_H - 20f) {
        box(Color(0xFFCFE9FF), 1.5f, wy, 4f, 14f, 1.5f)
        box(Color(0xFFCFE9FF), CAR_W - 5.5f, wy, 4f, 14f, 1.5f)
        wy += 24f
    }
    box(accent, 12f, top + 1.5f, CAR_W - 24f, 2.5f, 1f)
    box(accent, 12f, top + CAR_H - 4f, CAR_W - 24f, 2.5f, 1f)
}

private fun DrawScope.drawVestibule(y: Float) {
    box(Color(0xFF6E7781), AISLE_X - 14f, y, 28f, GAP)
    line(Color(0xFF59626B), AISLE_X - 14f, y + GAP / 2f, AISLE_X + 14f, y + GAP / 2f, 1f)
}

private fun DrawScope.drawEngineCar(top: Float, look: Look, npc: Npc, time: Float, boosting: Boolean) {
    val act = npc.act

    // Boiler with a glowing furnace door
    box(Color(0xFF3A3F47), 10f, top + 10f, 60f, 54f, 8f)
    box(Color(0xFF4B525C), 14f, top + 14f, 30f, 46f, 6f)
    val flicker = 0.5f + 0.5f * sin(time * 9f)
    val heatAmt = (0.35f + 0.25f * flicker + (if (boosting) 0.35f else 0f) + (if (act == Act.SHOVEL_DROP) 0.25f else 0f))
        .coerceIn(0f, 1f)
    dot(Color(0x44FF8A2B), 68f, top + 34f, 10f + 8f * heatAmt)
    box(lerp(Color(0xFF5A1A0A), Color(0xFFFF8A2B), heatAmt), 60f, top + 22f, 10f, 24f, 3f)
    dot(Color(0xFFE9E4D8), 28f, top + 38f, 8f)
    val needle = -2.2f + 1.0f * sin(time * 1.5f)
    line(Color(0xFFD64545), 28f, top + 38f, 28f + 6f * cos(needle), top + 38f + 6f * sin(needle), 1.4f)

    // Coal bin (fuller with Coal Stoker upgrades)
    box(Color(0xFF5B4636), 128f, top + 10f, 54f, 40f, 6f)
    box(Color(0xFF22262B), 132f, top + 14f, 46f, 32f, 5f)
    val coals = 6 + min(look.coal, 8)
    for (i in 0 until coals) {
        val cx = 138f + (i % 5) * 9f
        val cy = top + 22f + (i / 5) * 9f
        dot(Color(0xFF0F1113), cx, cy, 4.6f)
        dot(Color(0xFF3A3F47), cx - 1.4f, cy - 1.4f, 1.5f)
    }

    // Driver console with a lever
    box(Color(0xFF50606E), 120f, top + 82f, 62f, 26f, 5f)
    box(Color(0xFF2B2F36), 124f, top + 85f, 24f, 9f, 2f)
    box(lerp(Color(0xFF2E6F8E), Color(0xFF7FD8FF), 0.5f + 0.5f * sin(time * 2f)), 126f, top + 87f, 20f, 5f, 1f)
    if (look.booster > 0) {
        val glow = if (boosting) 1f else 0.5f + 0.3f * sin(time * 3f)
        dot(lerp(Color(0xFF3B2A12), Color(0xFFFFB347), glow), 168f, top + 95f, 7f)
        dot(Color(0xAAFFFFFF), 166f, top + 93f, 2f)
    }
    val knobY = top + 94f + (if (act == Act.DRIVE) 5f * sin(npc.phase) else 1.5f * sin(time))
    line(Color(0xFF2B2F36), 130f, top + 102f, 127f, knobY, 2.4f)
    dot(Color(0xFFFF9F43), 127f, knobY, 3.2f)

    // Locker, or a bunk once the Night Conductor is hired
    if (look.conductor > 0) {
        box(Color(0xFF8B5E3C), 8f, top + 80f, 50f, 28f, 4f)
        box(Color(0xFFE9E4D8), 10f, top + 82f, 14f, 24f, 4f)
        box(Color(0xFF4A6FA5), 26f, top + 82f, 30f, 24f, 4f)
        if (act == Act.SLEEP) {
            for (k in 0 until 3) {
                val t = (time * 0.6f + k / 3f) % 1f
                dot(Color.White.copy(alpha = (1f - t) * 0.85f), 30f + t * 12f, top + 80f - t * 16f, 2f + k * 1.2f)
            }
        }
    } else {
        box(Color(0xFF7B8794), 8f, top + 80f, 50f, 28f, 4f)
        line(Color(0xFF5E6873), 24.6f, top + 80f, 24.6f, top + 108f, 1.2f)
        line(Color(0xFF5E6873), 41.3f, top + 80f, 41.3f, top + 108f, 1.2f)
    }
}

private fun DrawScope.drawUtilityCar(top: Float, look: Look, npc: Npc, time: Float) {
    val act = npc.act

    // Heat generator with a valve wheel the worker turns
    if (look.heat > 0) {
        val pulse = 0.5f + 0.5f * sin(time * 3f)
        box(Color(0xFF4A6FA5), 8f, top + 8f, 54f, 54f, 8f)
        box(Color(0xFF3A5782), 12f, top + 12f, 46f, 46f, 6f)
        dot(lerp(Color(0xFF2B4B7A), Color(0xFFFFB347), pulse), 33f, top + 35f, 14f)
        dot(Color(0x66FFFFFF), 29f, top + 31f, 4f)
        val ang = if (act == Act.TURN_VALVE) npc.phase * 1.2f else time * 0.4f
        val wy = top + 34f
        drawCircle(Color(0xFFC8CED4), 8f * density, Offset(62f * density, wy * density), style = Stroke(2.4f * density))
        for (k in 0 until 4) {
            val a = ang + k * PI_F / 2f
            line(Color(0xFFC8CED4), 62f, wy, 62f + 8f * cos(a), wy + 8f * sin(a), 1.8f)
        }
        for (k in 0 until 3) {
            val t = (time * 0.5f + k / 3f) % 1f
            dot(Color(0xFFFFB347).copy(alpha = (1f - t) * 0.35f), 20f + k * 14f, top + 30f - t * 16f, 5f + t * 4f)
        }
    } else {
        slot(8f, top + 8f, 54f, 54f)
    }

    if (look.food > 0) {
        // Kitchen counter with two burners and a pot
        val cooking = act == Act.COOK
        box(Color(0xFFBDB6A6), 128f, top + 8f, 54f, 66f, 4f)
        box(Color(0xFFD9D2C3), 131f, top + 11f, 48f, 60f, 3f)
        dot(Color(0xFF2B2F36), 138f, top + 30f, 10f)
        dot(Color(0xFF50606E), 138f, top + 30f, 6.5f)
        dot(Color(0xFF2B2F36), 138f, top + 56f, 10f)
        dot(Color(0xFF50606E), 138f, top + 56f, 6.5f)
        if (cooking) dot(Color(0x66FF8A2B), 138f, top + 30f, 9f)
        dot(Color(0xFF9AA5AF), 138f, top + 30f, 8f)
        dot(Color(0xFFE0A21B), 138f, top + 30f, 5.6f)
        val sa = if (cooking) npc.phase * 2f else 0f
        line(Color(0xFF8B5E3C), 138f, top + 30f, 138f + 7f * cos(sa), top + 30f + 7f * sin(sa), 1.6f)
        for (k in 0 until 3) {
            val t = (time * 0.7f + k / 3f) % 1f
            val strength = if (cooking) 0.7f else 0.25f
            dot(Color.White.copy(alpha = (1f - t) * strength), 138f + (k - 1) * 3f, top + 30f - t * 14f, 3f + t * 4f)
        }
        box(Color(0xFFC49A6C), 160f, top + 18f, 16f, 22f, 2f)
        dot(Color(0xFFE05A47), 168f, top + 26f, 3f)

        // Dining table
        box(Color(0xFF6E4A2E), 8f, top + 76f, 62f, 32f, 6f)
        box(Color(0xFF8B5E3C), 11f, top + 79f, 56f, 26f, 4f)
        dot(Color(0xFFF2F2F2), 62f, top + 93f, 7f)
        dot(Color(0xFFE0A21B), 62f, top + 93f, 3.4f)
        dot(Color(0xFFCFE9FF), 24f, top + 88f, 3f)
    } else {
        slot(128f, top + 8f, 54f, 66f)
        slot(8f, top + 76f, 62f, 32f)
    }
}

private fun DrawScope.crate(x: Float, y: Float) {
    box(Color(0xFF8B5E3C), x, y, 20f, 20f, 2f)
    box(Color(0xFFB98B5E), x + 2f, y + 2f, 16f, 16f, 1.5f)
    line(Color(0xFF8B5E3C), x + 2f, y + 2f, x + 18f, y + 18f, 1.4f)
    line(Color(0xFF8B5E3C), x + 18f, y + 2f, x + 2f, y + 18f, 1.4f)
}

private fun DrawScope.drawCargoCar(top: Float, idx: Int, npc: Npc) {
    crate(12f, top + 14f)
    crate(34f, top + 14f)
    crate(12f, top + 36f)
    crate(132f, top + 64f)
    crate(154f, top + 64f)
    val st = npc.step
    val placing = npc.working && npc.act == Act.CRATE_PLACE && npc.carIdx == idx &&
        st != null && npc.actT >= st.dur * 0.5f
    if (placing) crate(132f, top + 86f)
}

private fun DrawScope.drawSnowfall(time: Float, w: Float, h: Float, u: Float) {
    for (i in 0 until 26) {
        val fx = ((i * 0.6180339f) % 1f) * w
        val fy = ((i * 0.4142136f) % 1f) * h
        val sx = (((fx + sin(time * 0.8f + i) * 10f * u) % w) + w) % w
        val sy = (fy + time * 26f * u + i * 11f) % h
        drawCircle(Color.White.copy(alpha = 0.85f), (1.4f + (i % 3) * 0.6f) * u, Offset(sx, sy))
    }
}

private fun DrawScope.drawSpeedLines(time: Float, originX: Float, trainPx: Float, w: Float, h: Float, u: Float) {
    for (i in 0 until 8) {
        val x = if (i % 2 == 0) {
            originX * (0.2f + 0.12f * i)
        } else {
            originX + trainPx + (w - originX - trainPx) * (0.2f + 0.1f * i)
        }
        val y = ((time * 900f * u + i * 97f * u) % (h + 60f * u)) - 30f * u
        drawLine(Color.White.copy(alpha = 0.7f), Offset(x, y), Offset(x, y + 34f * u), 2.5f * u)
    }
}

// ---------------------------------------------------------------------------------------------
// The worker, drawn from above. Local coordinates: forward is -y, right is +x.
// ---------------------------------------------------------------------------------------------
private fun DrawScope.drawNpc(npc: Npc) {
    val u = density
    val cx = npc.x * u
    val cy = npc.y * u
    val ph = npc.phase
    val st = npc.step
    val act = npc.act
    val walking = !npc.working
    val s = if (walking) sin(ph) else 0f
    val carrying = npc.carrying
    val coal = npc.holdingCoal
    val hasShovel = st != null && (st.act == Act.SHOVEL_PICK || st.act == Act.SHOVEL_DROP)

    var lx = -9f
    var ly = s * 3.5f
    var rx = 9f
    var ry = -s * 3.5f
    var tipY = -17f
    val a = ph * 1.2f

    if (npc.working) {
        when (act) {
            Act.SHOVEL_PICK -> {
                lx = -5f; ly = -7f; rx = 5f; ry = -3f
                tipY = -17f - 5f * (0.5f + 0.5f * sin(ph * 1.5f))
            }
            Act.SHOVEL_DROP -> {
                lx = -5f; ly = -7f; rx = 5f; ry = -3f
                tipY = -17f - 6f * (0.5f + 0.5f * sin(ph * 1.5f))
            }
            Act.DRIVE -> {
                lx = -6f; ly = -9f; rx = 2f + 5f * sin(ph); ry = -14f
            }
            Act.TURN_VALVE -> {
                lx = cos(a) * 6f; ly = -12f + sin(a) * 6f
                rx = cos(a + PI_F) * 6f; ry = -12f + sin(a + PI_F) * 6f
            }
            Act.COOK -> {
                lx = -7f; ly = -6f
                rx = cos(ph * 2f) * 3f; ry = -12f + sin(ph * 2f) * 3f
            }
            Act.EAT -> {
                val t = 0.5f + 0.5f * sin(ph * 1.5f)
                lx = -6f; ly = -7f; rx = 3f; ry = -12f + 8f * t
            }
            Act.CRATE_PICK -> {
                val b = 2f * sin(ph * 2f)
                lx = -6f; ly = -14f + b; rx = 6f; ry = -14f + b
            }
            Act.CRATE_PLACE -> {
                if (carrying) { lx = -6f; ly = -9f; rx = 6f; ry = -9f } else { lx = -6f; ly = -13f; rx = 6f; ry = -13f }
            }
            Act.SLEEP -> {
                lx = -7f; ly = -2f; rx = 7f; ry = -2f
            }
            else -> {}
        }
    } else if (hasShovel) {
        lx = -6f; ly = -6f; rx = 6f; ry = -3f
    } else if (carrying) {
        lx = -6f; ly = -9f; rx = 6f; ry = -9f
    }

    val jacket = Color(0xFFFF9F43)
    val skin = Color(0xFFF2C9A0)
    val boots = Color(0xFF2B2F36)

    drawOval(Color(0x33000000), Offset(cx - 11f * u, cy - 5f * u), Size(22f * u, 16f * u))
    rotate(degrees = npc.faceDeg, pivot = Offset(cx, cy)) {
        // feet
        drawOval(boots, Offset(cx - 7f * u, cy + (s * 3f - 3f) * u), Size(5f * u, 8f * u))
        drawOval(boots, Offset(cx + 2f * u, cy + (-s * 3f - 3f) * u), Size(5f * u, 8f * u))
        // arms
        drawLine(jacket, Offset(cx - 8f * u, cy), Offset(cx + lx * u, cy + ly * u), 3.6f * u)
        drawLine(jacket, Offset(cx + 8f * u, cy), Offset(cx + rx * u, cy + ry * u), 3.6f * u)
        if (hasShovel) {
            drawLine(Color(0xFF8B5E3C), Offset(cx, cy - 3f * u), Offset(cx, cy + tipY * u), 2.4f * u)
        }
        // body
        drawRoundRect(jacket, Offset(cx - 10f * u, cy - 5.5f * u), Size(20f * u, 11f * u), CornerRadius(5f * u, 5f * u))
        drawRoundRect(Color(0xFFE0762B), Offset(cx - 10f * u, cy - 1f * u), Size(20f * u, 3f * u), CornerRadius(1.5f * u, 1.5f * u))
        // hands
        drawCircle(skin, 3f * u, Offset(cx + lx * u, cy + ly * u))
        drawCircle(skin, 3f * u, Offset(cx + rx * u, cy + ry * u))
        // head: hat with a pompom, face peeking out at the front
        drawCircle(Color(0xFF7FD8FF), 6.5f * u, Offset(cx, cy - 0.5f * u))
        drawCircle(skin, 3.2f * u, Offset(cx, cy - 6f * u))
        drawCircle(Color.White, 2f * u, Offset(cx, cy + 0.5f * u))
        // shovel blade, with coal on it while delivering
        if (hasShovel) {
            drawCircle(Color(0xFF9AA5AF), 4.2f * u, Offset(cx, cy + tipY * u))
            if (coal) drawCircle(Color(0xFF15181B), 2.8f * u, Offset(cx, cy + tipY * u))
            if (npc.working && act == Act.SHOVEL_DROP) {
                for (k in 0 until 3) {
                    val t = (npc.actT * 2f + k / 3f) % 1f
                    drawCircle(Color(0xFF15181B), 1.8f * u, Offset(cx + (k - 1) * 1.5f * u, cy + (tipY - 2f - t * 8f) * u))
                }
            }
        }
        // crate held in front
        if (carrying) {
            drawRoundRect(Color(0xFF8B5E3C), Offset(cx - 7f * u, cy - 25f * u), Size(14f * u, 14f * u), CornerRadius(2f * u, 2f * u))
            drawRoundRect(Color(0xFFB98B5E), Offset(cx - 5.5f * u, cy - 23.5f * u), Size(11f * u, 11f * u), CornerRadius(1.5f * u, 1.5f * u))
        }
    }
}
