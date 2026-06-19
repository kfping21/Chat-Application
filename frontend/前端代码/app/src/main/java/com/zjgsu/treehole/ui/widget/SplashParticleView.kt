package com.zjgsu.treehole.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SplashParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val ripples = mutableListOf<Ripple>()
    
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    
    private var animator: ValueAnimator? = null
    private var isAnimating = false

    private class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var radius: Float,
        var alpha: Float,
        val gravity: Float = 0.5f
    )
    
    private class Ripple(
        var x: Float,
        var y: Float,
        var radius: Float,
        var alpha: Float,
        val maxRadius: Float
    )

    fun splash(centerX: Float, centerY: Float) {
        particles.clear()
        ripples.clear()
        
        // Generate 30 water droplet particles
        for (i in 0..30) {
            val angle = Random.nextDouble(Math.PI).toFloat() + Math.PI.toFloat() // Upward hemisphere
            val speed = Random.nextFloat() * 15f + 5f // Random speed
            val radius = Random.nextFloat() * 6f + 2f // Random size
            
            particles.add(
                Particle(
                    x = centerX,
                    y = centerY,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    radius = radius,
                    alpha = 255f
                )
            )
        }
        
        // Generate 5 prominent ripples with delays
        ripples.add(Ripple(centerX, centerY, 10f, 255f, 200f))
        ripples.add(Ripple(centerX, centerY, -40f, 255f, 250f)) 
        ripples.add(Ripple(centerX, centerY, -80f, 255f, 300f))
        ripples.add(Ripple(centerX, centerY, -120f, 255f, 350f))
        ripples.add(Ripple(centerX, centerY, -160f, 255f, 400f))
        
        startAnimationLoop()
    }
    
    fun drip(centerX: Float, centerY: Float) {
        // Generate drops falling downwards
        for (i in 0..5) {
            val vx = (Random.nextFloat() - 0.5f) * 2f
            val vy = Random.nextFloat() * 5f + 2f
            val radius = Random.nextFloat() * 4f + 2f
            
            particles.add(
                Particle(
                    x = centerX + (Random.nextFloat() - 0.5f) * 50f, // Randomize starting X
                    y = centerY,
                    vx = vx,
                    vy = vy,
                    radius = radius,
                    alpha = 255f,
                    gravity = 0.2f
                )
            )
        }
        
        startAnimationLoop()
    }

    private fun startAnimationLoop() {
        if (isAnimating) return
        isAnimating = true
        
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10000 // practically infinite until stopped
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                updatePhysics()
                invalidate()
            }
            start()
        }
    }
    
    private fun updatePhysics() {
        var activeItems = 0
        
        // Update particles
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.5f // gravity
            p.alpha -= 5f
            if (p.alpha <= 0) {
                pIter.remove()
            } else {
                activeItems++
            }
        }
        
        // Update ripples
        val rIter = ripples.iterator()
        while (rIter.hasNext()) {
            val r = rIter.next()
            r.radius += 3f // slower expansion
            if (r.radius > 0) {
                val progress = r.radius / r.maxRadius
                r.alpha = (1f - progress) * 255f
            }
            if (r.alpha <= 0 || r.radius >= r.maxRadius) {
                rIter.remove()
            } else {
                activeItems++
            }
        }
        
        if (activeItems == 0 && isAnimating) {
            isAnimating = false
            animator?.cancel()
            // Force one last draw to clear the screen
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw ripples
        ripples.forEach { r ->
            if (r.radius > 0 && r.alpha > 0) {
                ripplePaint.alpha = r.alpha.toInt().coerceIn(0, 255)
                // Flatten the ripple vertically to simulate 3D perspective on water
                canvas.save()
                canvas.scale(1f, 0.4f, r.x, r.y)
                canvas.drawCircle(r.x, r.y, r.radius, ripplePaint)
                canvas.restore()
            }
        }
        
        // Draw particles
        particles.forEach { p ->
            if (p.alpha > 0) {
                particlePaint.alpha = p.alpha.toInt().coerceIn(0, 255)
                canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
            }
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
