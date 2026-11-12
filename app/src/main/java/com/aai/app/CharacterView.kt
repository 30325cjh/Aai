package com.aai.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.View
import kotlin.math.max

/**
 * Still-image ARIS expression switcher.
 * The normal state uses the transparent full-body cut-out; expressions use
 * the exact face panels from the supplied ARIS model sheet.
 */
class CharacterView(context: Context) : View(context) {
    enum class Expression { NORMAL, HAPPY, SAD, SURPRISED, CRYING }

    private val handler = Handler(Looper.getMainLooper())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val artwork: Bitmap? = CharacterArtwork.bitmap(context)
    private var expression = Expression.NORMAL
    private var expressionToken = 0

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        contentDescription = "ARIS 캐릭터"
    }

    fun setExpression(value: Expression, resetAfterMs: Long = 3600L) {
        expression = value
        val token = ++expressionToken
        invalidate()
        handler.removeCallbacksAndMessages(null)
        if (value != Expression.NORMAL && resetAfterMs > 0L) {
            handler.postDelayed({
                if (expressionToken == token) {
                    expression = Expression.NORMAL
                    invalidate()
                }
            }, resetAfterMs)
        }
    }

    /** Voice and mouth animation are intentionally disabled; expressions are still images. */
    fun setTalking(value: Boolean) = Unit

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val expressionKey = when (expression) {
            Expression.HAPPY -> "happy"
            Expression.SURPRISED -> "surprised"
            Expression.SAD, Expression.CRYING -> "sad"
            Expression.NORMAL -> null
        }
        val portrait = expressionKey?.let { CharacterExpressions.bitmap(context, it) }
        val bitmap = portrait ?: artwork ?: return
        val dst = RectF(0f, 0f, width.toFloat(), height.toFloat())

        if (portrait == null) {
            val scale = minOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
            val w = bitmap.width * scale
            val h = bitmap.height * scale
            val left = (width - w) / 2f
            val top = (height - h) / 2f
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), paint)
        } else {
            // Face cards are square. Center-crop to the tall overlay window so the
            // selected expression becomes a clear, full-window portrait.
            val viewRatio = width.toFloat() / height.toFloat()
            val imageRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val src = if (imageRatio > viewRatio) {
                val cropW = (bitmap.height * viewRatio).toInt().coerceAtLeast(1)
                val left = (bitmap.width - cropW) / 2
                Rect(left, 0, left + cropW, bitmap.height)
            } else {
                val cropH = (bitmap.width / viewRatio).toInt().coerceAtLeast(1)
                val top = (bitmap.height - cropH) / 2
                Rect(0, top, bitmap.width, top + cropH)
            }
            canvas.drawBitmap(bitmap, src, dst, paint)
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }
}
