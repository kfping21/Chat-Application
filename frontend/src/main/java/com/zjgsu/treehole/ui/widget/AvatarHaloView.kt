package com.zjgsu.treehole.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class AvatarHaloView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * context.resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15FF4D4F") // very light red
        style = Paint.Style.FILL
    }

    private var sweepAngle = 0f
    private var startAngle = -90f
    private var expandRadius = 0f
    private var expandAlpha = 0f

    private var isPlaying = false
    private val rectF = RectF()

    private var staticAlpha = 0f
    private var baseRadius = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        baseRadius = w / 2f * 0.68f // Fits closely around the avatar
        rectF.set(w / 2f - baseRadius, h / 2f - baseRadius, w / 2f + baseRadius, h / 2f + baseRadius)
        
        // Multi-color sweep gradient matching the image
        val colors = intArrayOf(
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#FFD180"), // Orange
            Color.parseColor("#FF4D4F"), // Red
            Color.parseColor("#FF8A80"), // Pink
            Color.parseColor("#FFFFFF")
        )
        val positions = floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f)
        paint.shader = SweepGradient(w / 2f, h / 2f, colors, positions)
    }

    fun playAnimation() {
        if (isPlaying) return
        isPlaying = true
        alpha = 1f
        sweepAngle = 180f
        expandRadius = 0f
        staticAlpha = 0f

        val rotateAnimator = ValueAnimator.ofFloat(-90f, 270f).apply {
            duration = 700
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                startAngle = it.animatedValue as Float
                val matrix = Matrix()
                matrix.setRotate(startAngle + 90f, width / 2f, height / 2f)
                paint.shader.setLocalMatrix(matrix)
                invalidate()
            }
        }

        // Expand slightly for the small ripple (continuous)
        val expandAnimator = ValueAnimator.ofFloat(baseRadius, baseRadius * 1.35f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                expandRadius = it.animatedValue as Float
                val fraction = it.animatedFraction
                expandAlpha = 1f - fraction
                staticAlpha = 1f // Keep static circle fully visible while it pulses
                invalidate()
            }
        }

        rotateAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                sweepAngle = 360f // form full circle momentarily before fade
                staticAlpha = 1f
                expandAnimator.start()
            }
        })

        // We remove the onAnimationEnd listener for expandAnimator 
        // because it will now repeat infinitely and shouldn't stop itself.

        rotateAnimator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isPlaying) return

        val cx = width / 2f
        val cy = height / 2f

        if (expandRadius > 0) {
            // Phase 2: draw static full circle AND expanding small ripple
            
            // 1. Static full circle
            if (staticAlpha > 0f) {
                paint.alpha = (staticAlpha * 255).toInt()
                paint.strokeWidth = 2f * context.resources.displayMetrics.density
                canvas.drawCircle(cx, cy, baseRadius, paint)
            }
            
            // 2. Expanding small ripple
            if (expandAlpha > 0f) {
                paint.alpha = (expandAlpha * 255).toInt()
                paint.strokeWidth = 1f * context.resources.displayMetrics.density // thinner for ripple
                canvas.drawCircle(cx, cy, expandRadius, paint)
            }

        } else if (sweepAngle > 0) {
            // Phase 1: Rotating arc
            paint.alpha = 255
            paint.strokeWidth = 2f * context.resources.displayMetrics.density
            canvas.drawArc(rectF, startAngle, sweepAngle, false, paint)
        }
    }
}
