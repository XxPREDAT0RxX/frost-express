package com.chetna.frostexpress

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.roundToInt

/** One animation of a Blender NPC: frames laid side by side in [img] (see [NpcAtlas]). */
class NpcAnim(val img: ImageBitmap, val info: NpcStripInfo)

/** A Blender-rendered passenger: idle (breathing, blinking), front walk and three-quarter side walk. */
class NpcLook(val idle: NpcAnim, val walk: NpcAnim, val side: NpcAnim)

/** Frame rates of the strips: the idle strip is 12 frames over 2 s, a walk strip is one full stride. */
private const val IDLE_FPS = 6f
private const val TWO_PI = 6.2831855f

/**
 * Draws frame [frame] of [a] with the render's feet line at ([cx],[feetY]).
 * [s] is screen px per strip px; [wf] narrows the figure about its centre (used while turning).
 */
private fun DrawScope.npcFrame(a: NpcAnim, frame: Int, cx: Float, feetY: Float, s: Float, flip: Boolean, alpha: Float, wf: Float) {
    val inf = a.info
    val f = Math.floorMod(frame, inf.frames)
    val xs = s * wf / spriteXScale
    val left = cx + (inf.x0 - NpcAtlas.FRAME_W / 2f) * xs
    val top = feetY + (inf.y0 - NpcAtlas.FEET_Y) * s
    val w = inf.w * xs
    val h = inf.h * s
    withTransform({ if (flip) scale(-1f, 1f, Offset(cx, feetY)) }) {
        drawImage(
            a.img,
            srcOffset = IntOffset(f * inf.w, 0), srcSize = IntSize(inf.w, inf.h),
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)),
            alpha = alpha.coerceIn(0f, 1f), filterQuality = FilterQuality.Medium,
        )
    }
}

/**
 * A Blender NPC standing or walking. [height] is how tall a grown-up is drawn (px; children come out smaller).
 * [facing] runs from -1 (walking left) through 0 (towards us) to +1 (walking right); in between the figure narrows
 * and swaps to the side view, which reads as a quick turn. [phase] advances with the walk (one stride per 2 PI),
 * [moving] above 0.5 plays the walk, below it the idle; [t] (s) drives the idle.
 */
fun DrawScope.drawNpc(look: NpcLook, cx: Float, feetY: Float, height: Float, t: Float, phase: Float, moving: Float, facing: Float, alpha: Float) {
    val s = height / NpcAtlas.ADULT_H
    val af = abs(facing.coerceIn(-1f, 1f))
    val walking = moving > 0.5f
    val step = Math.floorMod((phase / TWO_PI * 8f).toInt(), 8)
    if (af < 0.5f) {
        val wf = 0.35f + 0.65f * (1f - af * 2f)
        if (walking) npcFrame(look.walk, step, cx, feetY, s, false, alpha, wf)
        else npcFrame(look.idle, (t * IDLE_FPS).toInt(), cx, feetY, s, false, alpha, wf)
    } else {
        val wf = 0.35f + 0.65f * (af * 2f - 1f)
        npcFrame(look.side, if (walking) step else 0, cx, feetY, s, facing < 0f, alpha, wf)
    }
}

/**
 * A Blender NPC sitting in a train seat, seen from the front: the idle animation, cut off at [clipY] (px)
 * where the seat cushion starts, so only the upper body shows. [height] as in [drawNpc].
 */
fun DrawScope.drawNpcSeated(look: NpcLook, cx: Float, feetY: Float, height: Float, t: Float, clipY: Float, alpha: Float = 1f) {
    val s = height / NpcAtlas.ADULT_H
    clipRect(left = cx - height, top = feetY - height * 2f, right = cx + height, bottom = clipY) {
        npcFrame(look.idle, (t * IDLE_FPS).toInt(), cx, feetY, s, false, alpha, 1f)
    }
}
