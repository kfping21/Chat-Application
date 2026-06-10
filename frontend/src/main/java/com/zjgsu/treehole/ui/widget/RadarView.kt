package com.zjgsu.treehole.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min
import kotlin.random.Random

class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4C8BFE")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.FILL
    }

    private var rotationAngle = 0f
    private var maxRadius = 0f
    private var centerX = 0f
    private var centerY = 0f
    
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationAngle = it.animatedValue as Float
            invalidate()
        }
    }
    
    private val points = mutableListOf<Point>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        maxRadius = min(centerX, centerY) * 0.9f
        
        val colors = intArrayOf(Color.TRANSPARENT, Color.parseColor("#804C8BFE"))
        val positions = floatArrayOf(0.5f, 1f)
        val sweepGradient = SweepGradient(centerX, centerY, colors, positions)
        sweepPaint.shader = sweepGradient
        
        generatePoints()
    }
    
    private fun generatePoints() {
        points.clear()
        for (i in 0..15) {
            val r = maxRadius * 0.2f + Random.nextFloat() * (maxRadius * 0.7f)
            val angle = Random.nextFloat() * 360f
            val alpha = Random.nextInt(100, 255)
            points.add(Point(r, angle, alpha))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw 3 concentric circles
        for (i in 1..4) {
            val radius = maxRadius * (i / 4f)
            circlePaint.alpha = (255 * (1 - i / 4f) + 50).toInt().coerceIn(0, 255)
            canvas.drawCircle(centerX, centerY, radius, circlePaint)
        }
        
        // Draw points (candidates)
        points.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            val px = centerX + p.radius * Math.cos(rad).toFloat()
            val py = centerY + p.radius * Math.sin(rad).toFloat()
            
            // Randomly pulsate alpha
            val currentAlpha = (p.alpha + Math.sin(System.currentTimeMillis() / 200.0 + p.angle).toFloat() * 50).toInt().coerceIn(0, 255)
            pointPaint.alpha = currentAlpha
            
            canvas.drawCircle(px, py, 6f, pointPaint)
        }

        // Draw sweep gradient
        canvas.save()
        canvas.rotate(rotationAngle, centerX, centerY)
        canvas.drawCircle(centerX, centerY, maxRadius, sweepPaint)
        canvas.restore()
    }
    
    fun start() {
        if (!animator.isRunning) {
            animator.start()
        }
    }
    
    fun stop() {
        if (animator.isRunning) {
            animator.cancel()
        }
    }

    data class Point(val radius: Float, val angle: Float, val alpha: Int)
}
