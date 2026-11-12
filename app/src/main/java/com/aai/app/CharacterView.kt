package com.aai.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import kotlin.math.min
import kotlin.math.sin

/**
 * Transparent image-based ARIS avatar. The supplied model-sheet cut-out is the
 * visual source; subtle motion is applied to the intact artwork to avoid
 * inventing replacement character art where no separated source layers exist.
 */
class CharacterView(context: Context) : View(context) {
    enum class Expression { NORMAL, HAPPY, SAD, SURPRISED, CRYING }

    private val handler = Handler(Looper.getMainLooper())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(37, 45, 64) }
    private val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(150, 75, 98) }
    private val artwork: Bitmap? = CharacterArtwork.bitmap(context)
    private var expression = Expression.NORMAL
    private var talking = false
    private var expressionToken = 0
    private val startedAt = SystemClock.uptimeMillis()

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(Color.TRANSPARENT)
        contentDescription = "ARIS 캐릭터"
    }

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
        val bitmap = artwork ?: return
        if (width <= 0 || height <= 0) return

        val elapsed = (SystemClock.uptimeMillis() - startedAt) / 1000f
        val fit = min(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val drawW = bitmap.width * fit
        val drawH = bitmap.height * fit
        val left = (width - drawW) * 0.5f
        val top = (height - drawH) * 0.5f
        val breath = sin(elapsed * 1.35f) * drawH * 0.004f
        val sway = sin(elapsed * 0.72f) * 0.65f

        canvas.save()
        canvas.translate(width * 0.5f, height * 0.5f + breath)
        canvas.rotate(sway)
        canvas.translate(-width * 0.5f, -height * 0.5f)
        val dst = RectF(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(bitmap, null, dst, paint)

        // Face-area overlays are deliberately small: original facial artwork remains visible.
        val faceLeft = left + drawW * 0.405f
        val faceTop = top + drawH * 0.075f
        val faceW = drawW * 0.255f
        val faceH = drawH * 0.16f
        val eyeY = faceTop + faceH * 0.48f
        val eyeW = faceW * 0.20f
        val eyeGap = faceW * 0.27f
        val blinkPhase = elapsed % 4.2f
        val blink = if (blinkPhase in 3.90f..4.08f) {
            val p = (blinkPhase - 3.90f) / 0.18f
            if (p < 0.5f) p * 2f else (1f - p) * 2f
        } else 0f

        if (blink > 0.08f) {
            overlayPaint.strokeWidth = maxOf(1.5f, drawW * 0.008f)
            overlayPaint.style = Paint.Style.STROKE
            overlayPaint.strokeCap = Paint.Cap.ROUND
            for (x in listOf(faceLeft + faceW * 0.34f, faceLeft + faceW * 0.69f)) {
                canvas.drawLine(x - eyeW, eyeY, x + eyeW, eyeY, overlayPaint)
            }
            overlayPaint.style = Paint.Style.FILL
        }

        val mouthX = faceLeft + faceW * 0.53f
        val mouthY = faceTop + faceH * 0.80f
        when (expression) {
            Expression.HAPPY -> {
                mouthPaint.style = Paint.Style.STROKE
                mouthPaint.strokeWidth = maxOf(1.4f, drawW * 0.006f)
                canvas.drawArc(RectF(mouthX - faceW * .10f, mouthY - faceH * .015f,
                    mouthX + faceW * .10f, mouthY + faceH * .10f), 10f, 160f, false, mouthPaint)
                mouthPaint.style = Paint.Style.FILL
            }
            Expression.SAD, Expression.CRYING -> {
                mouthPaint.style = Paint.Style.STROKE
                mouthPaint.strokeWidth = maxOf(1.4f, drawW * 0.006f)
                canvas.drawArc(RectF(mouthX - faceW * .10f, mouthY,
                    mouthX + faceW * .10f, mouthY + faceH * .09f), 190f, 160f, false, mouthPaint)
                mouthPaint.style = Paint.Style.FILL
                if (expression == Expression.CRYING) {
                    mouthPaint.color = Color.rgb(72, 190, 232)
                    canvas.drawOval(RectF(faceLeft + faceW * .24f, eyeY + faceH * .05f,
                        faceLeft + faceW * .29f, eyeY + faceH * .22f), mouthPaint)
                    canvas.drawOval(RectF(faceLeft + faceW * .72f, eyeY + faceH * .05f,
                        faceLeft + faceW * .77f, eyeY + faceH * .22f), mouthPaint)
                    mouthPaint.color = Color.rgb(150, 75, 98)
                }
            }
            Expression.SURPRISED -> {
                canvas.drawOval(RectF(mouthX - faceW * .045f, mouthY - faceH * .02f,
                    mouthX + faceW * .045f, mouthY + faceH * .12f), mouthPaint)
            }
            else -> if (talking && (elapsed * 7f % 1f) > 0.48f) {
                canvas.drawOval(RectF(mouthX - faceW * .065f, mouthY - faceH * .015f,
                    mouthX + faceW * .065f, mouthY + faceH * .10f), mouthPaint)
            }
        }
        canvas.restore()
        postInvalidateDelayed(40L)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }
}
