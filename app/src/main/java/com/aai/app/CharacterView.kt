package com.aai.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lightweight animated 2D stand-in for the future layered Live2D artwork.
 * Keeps the character visible while the production layered model is prepared.
 */
class CharacterView(context: Context) : View(context) {
    enum class Expression { NORMAL, HAPPY, SAD, SURPRISED, CRYING }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var expression = Expression.NORMAL
    private var talking = false
    private var expressionToken = 0
    private val startedAt = SystemClock.uptimeMillis()

    private val hair = Color.rgb(48, 59, 79)
    private val hairLight = Color.rgb(111, 137, 157)
    private val blue = Color.rgb(42, 112, 190)
    private val dark = Color.rgb(27, 37, 56)
    private val skin = Color.rgb(255, 224, 218)
    private val cyan = Color.rgb(45, 205, 239)

    fun setExpression(value: Expression, resetAfterMs: Long = 3600L) {
        expression = value
        val token = ++expressionToken
        invalidate()
        handler.postDelayed({
            if (expressionToken == token) {
                expression = Expression.NORMAL
                invalidate()
            }
        }, resetAfterMs)
    }

    fun setTalking(value: Boolean) {
        talking = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = (SystemClock.uptimeMillis() - startedAt) / 1000f
        val breath = sin(t * 1.35f) * h * 0.008f
        val blinkPhase = (t % 4.1f)
        val blink = if (blinkPhase in 3.82f..4.02f) {
            val p = (blinkPhase - 3.82f) / 0.20f
            if (p < 0.5f) p * 2f else (1f - p) * 2f
        } else 0f
        val cx = w * 0.5f
        val scale = minOf(w / 330f, h / 420f)
        canvas.save()
        canvas.translate(cx, h * 0.04f + breath)
        canvas.scale(scale, scale)

        // Long hair behind the shoulders.
        fill(canvas, hair, 70f, 78f, 260f, 420f, 125f)
        fill(canvas, hairLight, 82f, 230f, 110f, 405f, 48f)
        fill(canvas, hairLight, 220f, 230f, 248f, 405f, 48f)

        // Jacket and upper body.
        val torso = Path().apply {
            moveTo(-72f, 238f); lineTo(-145f, 330f); lineTo(-112f, 420f)
            lineTo(112f, 420f); lineTo(145f, 330f); lineTo(72f, 238f); close()
        }
        paint.color = blue
        canvas.drawPath(torso, paint)
        fill(canvas, dark, -145f, 292f, -92f, 420f, 22f)
        fill(canvas, dark, 92f, 292f, 145f, 420f, 22f)
        fill(canvas, Color.WHITE, -82f, 264f, 82f, 420f, 12f)

        // Collar and cyan tie.
        val collarL = Path().apply { moveTo(-46f, 248f); lineTo(-8f, 270f); lineTo(-28f, 305f); lineTo(-66f, 270f); close() }
        val collarR = Path().apply { moveTo(46f, 248f); lineTo(8f, 270f); lineTo(28f, 305f); lineTo(66f, 270f); close() }
        paint.color = Color.rgb(235, 241, 249); canvas.drawPath(collarL, paint); canvas.drawPath(collarR, paint)
        val tie = Path().apply { moveTo(-13f, 270f); lineTo(13f, 270f); lineTo(9f, 350f); lineTo(0f, 364f); lineTo(-9f, 350f); close() }
        paint.color = cyan; canvas.drawPath(tie, paint)

        // Neck and face.
        fill(canvas, skin, -25f, 205f, 25f, 282f, 15f)
        fill(canvas, skin, -86f, 72f, 86f, 255f, 72f)
        fill(canvas, hair, -95f, 36f, 95f, 135f, 76f)

        // Side locks and bangs.
        fill(canvas, hair, -103f, 80f, -65f, 370f, 28f)
        fill(canvas, hair, 65f, 72f, 105f, 360f, 28f)
        val fringe = Path().apply {
            moveTo(-86f, 111f); quadTo(-45f, 4f, 2f, 72f); quadTo(55f, 5f, 88f, 116f)
            quadTo(42f, 78f, 13f, 120f); quadTo(-28f, 78f, -86f, 111f); close()
        }
        paint.color = hair; canvas.drawPath(fringe, paint)
        val centerBang = Path().apply {
            moveTo(-18f, 65f); quadTo(5f, 52f, 20f, 86f); lineTo(48f, 230f)
            quadTo(8f, 194f, -18f, 65f); close()
        }
        paint.color = hairLight; canvas.drawPath(centerBang, paint)

        // Hair clip and side ponytail loops.
        fill(canvas, Color.rgb(17, 53, 83), 79f, 80f, 92f, 172f, 5f)
        stroke(canvas, hairLight, 4f, 90f, 70f, 124f, 104f)
        stroke(canvas, hairLight, 4f, 116f, 100f, 145f, 130f)

        // Eyes: expression changes are drawn directly; blink compresses the eye height.
        val eyeH = maxOf(2f, 24f * (1f - blink))
        drawEye(canvas, -43f, 165f, eyeH)
        drawEye(canvas, 43f, 165f, eyeH)
        when (expression) {
            Expression.HAPPY -> {
                stroke(canvas, hair, 5f, -62f, 157f, -25f, 157f)
                stroke(canvas, hair, 5f, 25f, 157f, 62f, 157f)
            }
            Expression.SAD, Expression.CRYING -> {
                stroke(canvas, hair, 4f, -63f, 145f, -27f, 154f)
                stroke(canvas, hair, 4f, 27f, 154f, 63f, 145f)
                if (expression == Expression.CRYING) {
                    fill(canvas, cyan, -52f, 180f, -43f, 205f, 5f)
                    fill(canvas, cyan, 43f, 180f, 52f, 205f, 5f)
                }
            }
            Expression.SURPRISED -> {
                fill(canvas, hair, -8f, 194f, 8f, 218f, 9f)
            }
            else -> Unit
        }

        // Mouth animation / emotion.
        when (expression) {
            Expression.HAPPY -> stroke(canvas, Color.rgb(177, 83, 106), 5f, -13f, 220f, 0f, 228f, 13f, 220f)
            Expression.SAD, Expression.CRYING -> stroke(canvas, Color.rgb(177, 83, 106), 4f, -13f, 229f, 0f, 219f, 13f, 229f)
            Expression.SURPRISED -> fill(canvas, Color.rgb(177, 83, 106), -8f, 214f, 8f, 235f, 9f)
            else -> if (talking && (t * 8f % 2f) > 0.8f) {
                fill(canvas, Color.rgb(177, 83, 106), -11f, 217f, 11f, 233f, 8f)
            } else {
                stroke(canvas, Color.rgb(177, 83, 106), 3f, -12f, 224f, 0f, 227f, 12f, 224f)
            }
        }

        // Soft cheek blush.
        paint.color = 0x44F18C9C
        canvas.drawOval(RectF(-74f, 190f, -39f, 211f), paint)
        canvas.drawOval(RectF(39f, 190f, 74f, 211f), paint)

        canvas.restore()
        postInvalidateDelayed(45L)
    }

    private fun drawEye(c: Canvas, x: Float, y: Float, eyeHeight: Float) {
        fill(c, Color.WHITE, x - 18f, y - eyeHeight / 2f, x + 18f, y + eyeHeight / 2f + 4f, 12f)
        if (eyeHeight > 5f) {
            fill(c, Color.rgb(24, 157, 220), x - 11f, y - eyeHeight / 2f, x + 11f, y + eyeHeight / 2f + 2f, 9f)
            fill(c, Color.rgb(8, 69, 125), x - 6f, y - eyeHeight / 2f + 5f, x + 6f, y + eyeHeight / 2f + 3f, 6f)
            fill(c, Color.WHITE, x - 5f, y - 5f, x + 1f, y + 2f, 3f)
        } else {
            stroke(c, hair, 3f, x - 17f, y, x, y + 2f, x + 17f, y)
        }
    }

    private fun fill(c: Canvas, color: Int, l: Float, t: Float, r: Float, b: Float, radius: Float) {
        paint.color = color; paint.style = Paint.Style.FILL
        c.drawRoundRect(l, t, r, b, radius, radius, paint)
    }

    private fun stroke(c: Canvas, color: Int, width: Float, vararg points: Float) {
        paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = width
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        val p = Path()
        if (points.size >= 4) {
            p.moveTo(points[0], points[1])
            var i = 2
            while (i + 1 < points.size) { p.lineTo(points[i], points[i + 1]); i += 2 }
            c.drawPath(p, paint)
        }
        paint.style = Paint.Style.FILL
    }
}
