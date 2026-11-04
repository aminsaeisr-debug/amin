package com.srooyesh.seedcounter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Interactive, centered scan rectangle. Width/height are exact 1% increments. */
class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        private const val MIN_WIDTH = 0.56f
        private const val MAX_WIDTH = 0.96f
        private const val MIN_HEIGHT = 0.12f
        private const val MAX_HEIGHT = 0.58f
        private const val DEFAULT_WIDTH = 0.84f
        private const val DEFAULT_HEIGHT = 0.28f
    }

    fun interface OnFrameChangedListener {
        fun onChanged(widthFraction: Float, heightFraction: Float, finished: Boolean)
    }

    var onFrameChangedListener: OnFrameChangedListener? = null

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xA9000000.toInt() }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(2.2f)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF53C96B.toInt()
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = dp(13f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private var widthFraction = DEFAULT_WIDTH
    private var heightFraction = DEFAULT_HEIGHT
    private var activeHandle = Handle.NONE
    private var downX = 0f
    private var downY = 0f
    private var gestureStartWidth = DEFAULT_WIDTH
    private var gestureStartHeight = DEFAULT_HEIGHT
    private val touchRadius by lazy { dp(42f) }
    private val rect = RectF()

    enum class Handle { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    fun setFractions(widthFraction: Float, heightFraction: Float, notify: Boolean = false) {
        this.widthFraction = widthFraction.coerceIn(MIN_WIDTH, MAX_WIDTH)
        this.heightFraction = heightFraction.coerceIn(MIN_HEIGHT, MAX_HEIGHT)
        invalidate()
        if (notify) onFrameChangedListener?.onChanged(this.widthFraction, this.heightFraction, true)
    }

    fun resetToDefault(notify: Boolean = true) = setFractions(DEFAULT_WIDTH, DEFAULT_HEIGHT, notify)

    fun getWidthPercent(): Int = (widthFraction * 100f).roundToInt()
    fun getHeightPercent(): Int = (heightFraction * 100f).roundToInt()

    fun scanRectInPreview(): Rect {
        updateRect()
        return Rect(
            rect.left.roundToInt().coerceAtLeast(0),
            rect.top.roundToInt().coerceAtLeast(0),
            rect.right.roundToInt().coerceAtMost(width),
            rect.bottom.roundToInt().coerceAtMost(height)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateRect()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateRect()

        // Dim only outside the ROI so the scan area remains fully visible.
        canvas.drawRect(0f, 0f, width.toFloat(), rect.top, dimPaint)
        canvas.drawRect(0f, rect.bottom, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRect(0f, rect.top, rect.left, rect.bottom, dimPaint)
        canvas.drawRect(rect.right, rect.top, width.toFloat(), rect.bottom, dimPaint)

        canvas.drawRoundRect(rect, dp(12f), dp(12f), linePaint)
        val thirdW = rect.width() / 3f
        val thirdH = rect.height() / 3f
        canvas.drawLine(rect.left + thirdW, rect.top, rect.left + thirdW, rect.bottom, gridPaint)
        canvas.drawLine(rect.left + thirdW * 2f, rect.top, rect.left + thirdW * 2f, rect.bottom, gridPaint)
        canvas.drawLine(rect.left, rect.top + thirdH, rect.right, rect.top + thirdH, gridPaint)
        canvas.drawLine(rect.left, rect.top + thirdH * 2f, rect.right, rect.top + thirdH * 2f, gridPaint)

        val handleSize = dp(7f)
        arrayOf(
            rect.left to rect.top,
            rect.right to rect.top,
            rect.left to rect.bottom,
            rect.right to rect.bottom
        ).forEach { (x, y) -> canvas.drawCircle(x, y, handleSize, handlePaint) }

        val label = "کادر اسکن  ${getWidthPercent()}٪ × ${getHeightPercent()}٪"
        val labelY = (rect.top - dp(12f)).coerceAtLeast(dp(26f))
        canvas.drawText(label, width / 2f, labelY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                updateRect()
                activeHandle = nearestHandle(event.x, event.y)
                downX = event.x
                downY = event.y
                gestureStartWidth = widthFraction
                gestureStartHeight = heightFraction
                return activeHandle != Handle.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeHandle == Handle.NONE) return false
                val dx = event.x - downX
                val dy = event.y - downY
                val widthDeltaFraction = when (activeHandle) {
                    Handle.TOP_LEFT, Handle.BOTTOM_LEFT -> -2f * dx / max(1, width).toFloat()
                    Handle.TOP_RIGHT, Handle.BOTTOM_RIGHT -> 2f * dx / max(1, width).toFloat()
                    else -> 0f
                }
                val heightDeltaFraction = when (activeHandle) {
                    Handle.TOP_LEFT, Handle.TOP_RIGHT -> -2f * dy / max(1, height).toFloat()
                    Handle.BOTTOM_LEFT, Handle.BOTTOM_RIGHT -> 2f * dy / max(1, height).toFloat()
                    else -> 0f
                }
                widthFraction = quantize(
                    (gestureStartWidth + widthDeltaFraction),
                    MIN_WIDTH,
                    MAX_WIDTH
                )
                heightFraction = quantize(
                    (gestureStartHeight + heightDeltaFraction),
                    MIN_HEIGHT,
                    MAX_HEIGHT
                )
                invalidate()
                onFrameChangedListener?.onChanged(widthFraction, heightFraction, false)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeHandle == Handle.NONE) return false
                activeHandle = Handle.NONE
                invalidate()
                onFrameChangedListener?.onChanged(widthFraction, heightFraction, true)
                return true
            }
        }
        return false
    }

    private fun nearestHandle(x: Float, y: Float): Handle {
        val handles = listOf(
            Handle.TOP_LEFT to Pair(rect.left, rect.top),
            Handle.TOP_RIGHT to Pair(rect.right, rect.top),
            Handle.BOTTOM_LEFT to Pair(rect.left, rect.bottom),
            Handle.BOTTOM_RIGHT to Pair(rect.right, rect.bottom)
        )
        return handles.minByOrNull { (_, point) ->
            val dx = x - point.first
            val dy = y - point.second
            dx * dx + dy * dy
        }?.takeIf { (_, point) -> abs(x - point.first) <= touchRadius && abs(y - point.second) <= touchRadius }?.first
            ?: Handle.NONE
    }

    private fun updateRect() {
        val w = width * widthFraction
        val h = height * heightFraction
        rect.set(
            (width - w) / 2f,
            (height - h) / 2f,
            (width + w) / 2f,
            (height + h) / 2f
        )
    }

    private fun quantize(value: Float, minValue: Float, maxValue: Float): Float =
        ((value * 100f).roundToInt() / 100f).coerceIn(minValue, maxValue)

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
