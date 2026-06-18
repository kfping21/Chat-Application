package com.zjgsu.treehole.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import kotlin.random.Random

class ParticleShatterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null

    class Particle(
        var x: Float,
        var y: Float,
        val color: Int,
        val radius: Float,
        var alpha: Float,
        var vx: Float,
        var vy: Float,
        val gravity: Float
    ) {
        fun update() {
            x += vx
            y += vy
            vy += gravity
            alpha -= 0.015f // Fade out rate
        }
    }

    fun explode(targetView: View, onAnimationEnd: () -> Unit) {
        // 1. Capture targetView's bitmap
        val bitmap = getBitmapFromView(targetView) ?: return
        
        // 2. Get global coordinates of the target view to position particles correctly
        val location = IntArray(2)
        targetView.getLocationInWindow(location)
        
        // We also need our own coordinates to offset correctly
        val myLocation = IntArray(2)
        this.getLocationInWindow(myLocation)
        
        val offsetX = location[0] - myLocation[0]
        val offsetY = location[1] - myLocation[1]

        // 3. Generate particles (sample pixels)
        val particleSize = 8 // Sampling step (1 particle every 8 pixels)
        for (i in 0 until bitmap.width step particleSize) {
            for (j in 0 until bitmap.height step particleSize) {
                val color = bitmap.getPixel(i, j)
                // Ignore transparent pixels
                if (android.graphics.Color.alpha(color) > 10) {
                    // Random velocity outward from center
                    val centerX = bitmap.width / 2f
                    val centerY = bitmap.height / 2f
                    val dx = i - centerX
                    val dy = j - centerY
                    val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    val normalizedDx = if (distance == 0f) 0f else dx / distance
                    val normalizedDy = if (distance == 0f) 0f else dy / distance
                    
                    // Explosion force
                    val force = Random.nextFloat() * 15f + 10f
                    val vx = normalizedDx * force + (Random.nextFloat() - 0.5f) * 5f
                    val vy = normalizedDy * force + (Random.nextFloat() - 0.5f) * 5f - 10f // Initial upward burst

                    particles.add(
                        Particle(
                            x = offsetX + i.toFloat(),
                            y = offsetY + j.toFloat(),
                            color = color,
                            radius = Random.nextFloat() * 4f + 3f, // 3 to 7 px radius
                            alpha = 1f,
                            vx = vx,
                            vy = vy,
                            gravity = 0.8f // Gravity pulling down
                        )
                    )
                }
            }
        }
        
        bitmap.recycle()

        // 4. Start Animation
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000 // 1 second explosion
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                for (p in particles) {
                    if (p.alpha > 0) {
                        p.update()
                    }
                }
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onAnimationEnd()
                    val parent = parent as? ViewGroup
                    parent?.removeView(this@ParticleShatterView)
                }
            })
        }
        animator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in particles) {
            if (p.alpha > 0) {
                paint.color = p.color
                paint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
                canvas.drawCircle(p.x, p.y, p.radius, paint)
            }
        }
    }

    private fun getBitmapFromView(view: View): Bitmap? {
        if (view.width == 0 || view.height == 0) return null
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        particles.clear()
    }
}
