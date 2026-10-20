package com.chetna.frostexpress

import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A tiny 2D skinned-mesh animator for the chibi passengers.
 *
 * The character picture is laid over a soft grid; every grid point belongs partly to a few "bones"
 * (torso, head, arms, legs). Each frame the bones get a pose (breathing, head sway, walking) and
 * the grid is bent by blending their transforms, so the drawing bends smoothly instead of being cut
 * into rigid parts. Landmarks are fractions of the character's picture (front view, full body).
 */
object MeshPuppet {
    private const val COLS = 10
    private const val ROWS = 22
    private const val NV = (COLS + 1) * (ROWS + 1)
    /** Transparent border (px) around every full-body chibi picture. */
    const val PAD = 8f

    // grid in content fractions (a little margin around the picture)
    private val gu = FloatArray(NV)
    private val gv = FloatArray(NV)
    private val wT = FloatArray(NV); private val wH = FloatArray(NV)
    private val wAL = FloatArray(NV); private val wAR = FloatArray(NV)
    private val wLL = FloatArray(NV); private val wLR = FloatArray(NV)
    private val wTop = FloatArray(NV)
    private val idx = ShortArray(COLS * ROWS * 6)

    // landmarks (fractions of the content box), measured on the front-view sheet
    private const val NECK_Y = 0.484f
    private const val SH_X = 0.235f; private const val SH_Y = 0.553f
    private const val HIP_Y = 0.787f; private const val HIP_XL = 0.358f
    private const val TOP_Y = 0.43f

    private fun ss(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f); return t * t * (3f - 2f * t)
    }

    init {
        var k = 0
        for (r in 0..ROWS) for (c in 0..COLS) {
            val u = -0.06f + 1.12f * c / COLS
            val v = -0.03f + 1.06f * r / ROWS
            gu[k] = u; gv[k] = v
            val head = ss(0.514f, 0.436f, v)
            val top = ss(0.124f, 0.039f, v) * head
            val aL = ss(0.235f, 0.182f, u) * ss(0.585f, 0.650f, v) * ss(0.832f, 0.787f, v)
            val aR = ss(0.765f, 0.818f, u) * ss(0.585f, 0.650f, v) * ss(0.832f, 0.787f, v)
            val legs = ss(0.791f, 0.832f, v) * (1f - aL) * (1f - aR)
            val lL = legs * ss(0.543f, 0.469f, u)
            val lR = legs * ss(0.457f, 0.531f, u)
            val t = max(0f, 1f - head - aL - aR - lL - lR)
            val sum = t + head + aL + aR + lL + lR + 1e-6f
            wT[k] = t / sum; wH[k] = head / sum; wAL[k] = aL / sum; wAR[k] = aR / sum; wLL[k] = lL / sum; wLR[k] = lR / sum
            wTop[k] = top
            k++
        }
        var i = 0
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val a = r * (COLS + 1) + c
            val b = a + 1; val d = a + COLS + 1; val e = d + 1
            idx[i++] = a.toShort(); idx[i++] = b.toShort(); idx[i++] = d.toShort()
            idx[i++] = b.toShort(); idx[i++] = e.toShort(); idx[i++] = d.toShort()
        }
    }

    private val verts = FloatArray(NV * 2)
    private val texs = HashMap<ImageBitmap, FloatArray>()
    private val paints = HashMap<ImageBitmap, Paint>()

    private fun texFor(img: ImageBitmap): FloatArray = texs.getOrPut(img) {
        val cw = img.width - 2 * PAD; val ch = img.height - 2 * PAD
        FloatArray(NV * 2).also { t -> for (k in 0 until NV) { t[2 * k] = PAD + gu[k] * cw; t[2 * k + 1] = PAD + gv[k] * ch } }
    }

    private fun paintFor(img: ImageBitmap): Paint = paints.getOrPut(img) {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(img.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
    }

    /**
     * Draws [img] (a padded full-body chibi) with its feet at ([cx],[feetY]) and the content [h] px tall.
     * [t] is time (s) for the idle motion, [phase] the walk phase (radians, one step per PI),
     * [moving] 0..1 blends idle into walking, [flip] mirrors the picture.
     */
    fun draw(scope: DrawScope, img: ImageBitmap, cx: Float, feetY: Float, h: Float, t: Float, phase: Float, moving: Float, flip: Boolean, alpha: Float, xScale: Float = 1f) {
        val cw = img.width - 2 * PAD; val ch = img.height - 2 * PAD
        val k = ch / 769f                   // the poses were tuned on a 769 px tall drawing
        val m = moving.coerceIn(0f, 1f)
        // ---- pose parameters: idle and walk, blended ----
        val br = sin(6.2832f * t / 3f); val brl = sin(6.2832f * t / 3f - 0.7f)
        val sway = sin(6.2832f * t / 5f)
        val step = sin(phase); val as_ = abs(step)
        fun mix(a: Float, b: Float) = a + (b - a) * m
        val bob = mix(0f, (-3f * as_ + 1.5f) * k)
        val rootRot = mix(0.6f * sway, 2.2f * step)
        val sy = mix(1f + 0.018f * br, 1f + 0.008f * as_)
        val sx = mix(1f - 0.006f * br, 1f)
        val headRot = mix(2.25f * sway + 1.0f * brl, -2f * sin(phase - 0.6f))
        val headLag = mix(2.5f * sin(6.2832f * (t - 0.35f) / 5f), -3.5f * sin(phase - 1.3f))
        val armRotL = (1f - m) * (-1.5f * brl); val armRotR = (1f - m) * (1.5f * brl)
        val fL = m * step; val fR = -m * step              // forward swing (+1) / back (-1)
        val liftL = m * max(0f, step) * 9f * k; val liftR = m * max(0f, -step) * 9f * k
        val legL = m * -3f * max(0f, step); val legR = m * 3f * max(0f, -step)

        // landmarks in content px
        val hipX = 0.5f * cw; val hipY = HIP_Y * ch
        val neckX = 0.5f * cw; val neckY = NECK_Y * ch
        val shLX = SH_X * cw; val shRX = (1f - SH_X) * cw; val shY = SH_Y * ch
        val hipLX = HIP_XL * cw; val hipRX = (1f - HIP_XL) * cw
        // torso transform of a point: scale about the hips, then bob
        fun tx(x: Float) = hipX + (x - hipX) * sx
        fun ty(y: Float) = hipY + (y - hipY) * sy + bob
        val neckNX = tx(neckX); val neckNY = ty(neckY)
        val hr = Math.toRadians(headRot.toDouble()); val hc = cos(hr).toFloat(); val hs = sin(hr).toFloat()
        // the hair bun / pom-pom trails the head a little
        val ex = Math.toRadians((headLag - headRot).toDouble()); val ec = cos(ex).toFloat(); val es = sin(ex).toFloat()
        val dyv = (TOP_Y - NECK_Y) * ch                       // head centre relative to the neck
        val topCX = neckNX - hs * dyv
        val topCY = neckNY + hc * dyv
        val rr = Math.toRadians(rootRot.toDouble()); val rc = cos(rr).toFloat(); val rs = sin(rr).toFloat()
        val feetCX = 0.5f * cw; val feetCY = 0.995f * ch

        val scale = h / ch
        val w2 = cw * scale / xScale
        val left = cx - w2 / 2f
        val top = feetY - h
        val sxOut = (w2 / cw)

        for (i in 0 until NV) {
            val px = gu[i] * cw; val py = gv[i] * ch
            var ox = 0f; var oy = 0f
            // torso
            var w = wT[i]
            if (w > 0f) { ox += w * tx(px); oy += w * ty(py) }
            // head: rotate about the neck, ride the torso
            w = wH[i]
            if (w > 0f) {
                val dx = px - neckX; val dy = py - neckY
                ox += w * (neckNX + dx * hc - dy * hs); oy += w * (neckNY + dx * hs + dy * hc)
            }
            // arms
            w = wAL[i]; if (w > 0f) { arm(px, py, shLX, shY, armRotL, fL, -1f, k, tx(shLX) - shLX, ty(shY) - shY); ox += w * rx; oy += w * ry }
            w = wAR[i]; if (w > 0f) { arm(px, py, shRX, shY, armRotR, fR, 1f, k, tx(shRX) - shRX, ty(shY) - shY); ox += w * rx; oy += w * ry }
            // legs
            w = wLL[i]; if (w > 0f) { leg(px, py, hipLX, hipY, legL, liftL, bob); ox += w * rx; oy += w * ry }
            w = wLR[i]; if (w > 0f) { leg(px, py, hipRX, hipY, legR, liftR, bob); ox += w * rx; oy += w * ry }
            // trailing hair / hat top
            val tw = wTop[i]
            if (tw > 0f) {
                val dx = ox - topCX; val dy = oy - topCY
                ox = ox * (1 - tw) + tw * (topCX + dx * ec - dy * es)
                oy = oy * (1 - tw) + tw * (topCY + dx * es + dy * ec)
            }
            // whole body sways over the feet
            val dx = ox - feetCX; val dy = oy - feetCY
            var fx = feetCX + dx * rc - dy * rs
            val fy = feetCY + dx * rs + dy * rc
            if (flip) fx = cw - fx
            verts[2 * i] = left + fx * sxOut
            verts[2 * i + 1] = top + fy * scale
        }
        val p = paintFor(img)
        p.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val tex = texFor(img)
        scope.drawIntoCanvas { c ->
            c.nativeCanvas.drawVertices(Canvas.VertexMode.TRIANGLES, verts.size, verts, 0, tex, 0, null, 0, idx, 0, idx.size, p)
        }
    }

    // result of arm()/leg(), kept in fields so the per-vertex loop does not allocate
    private var rx = 0f
    private var ry = 0f

    private fun arm(px: Float, py: Float, shX: Float, shY: Float, rot: Float, f: Float, side: Float, k: Float, offX: Float, offY: Float) {
        // idle: a small rotation about the shoulder; walking (front view): forward/back swing shown as foreshortening
        val r = Math.toRadians(rot.toDouble()); val c = cos(r).toFloat(); val s = sin(r).toFloat()
        val dx0 = px - shX; val dy0 = py - shY
        val qx = dx0 * c - dy0 * s; val qy = dx0 * s + dy0 * c
        val dx = qx * (1f + 0.07f * f); val dy = qy * (1f - 0.13f * abs(f) + 0.03f * f)
        rx = shX + dx - side * 3.5f * k * max(f, 0f) + offX
        ry = shY + dy - 2f * k * abs(f) + offY
    }

    private fun leg(px: Float, py: Float, hx: Float, hy: Float, rot: Float, lift: Float, bob: Float) {
        val r = Math.toRadians(rot.toDouble()); val c = cos(r).toFloat(); val s = sin(r).toFloat()
        val dx = px - hx; val dy = py - hy
        rx = hx + dx * c - dy * s
        ry = hy + dx * s + dy * c - lift + bob
    }
}
