package com.zjgsu.treehole.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

class AudioWaveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF") // Cyan color
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }

    private val barCount = 3
    private var phase = 0f
    private var animator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator = ValueAnimator.ofFloat(0f, 2 * Math.PI.toFloat()).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        
        val barWidth = w / (barCount * 1.5f + 0.5f)
        val spacing = barWidth * 0.5f
        
        val maxBarHeight = h
        val minBarHeight = h * 0.3f
        
        paint.strokeWidth = barWidth

        for (i in 0 until barCount) {
            val x = barWidth / 2 + i * (barWidth + spacing)
            // Different speed and offset for each bar to make it look random
            val speedMultiplier = if (i == 1) 1.5f else 1.0f
            val offset = i * (Math.PI / 1.5)
            
            val scale = (sin(phase * speedMultiplier + offset) + 1) / 2.0 // 0 to 1
            val currentHeight = minBarHeight + (maxBarHeight - minBarHeight) * scale.toFloat()
            
            val top = (h - currentHeight) / 2
            val bottom = top + currentHeight
            
            canvas.drawLine(x, top, x, bottom, paint)
        }
    }
}
