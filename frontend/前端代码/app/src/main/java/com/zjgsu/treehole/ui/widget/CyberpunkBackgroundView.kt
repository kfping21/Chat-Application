package com.zjgsu.treehole.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class CyberpunkBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint()
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    // 赛博朋克渐变背景色
    private val colors = intArrayOf(
        Color.parseColor("#090216"), // 极暗紫
        Color.parseColor("#1b0b3b"), // 霓虹深紫
        Color.parseColor("#081224")  // 深青蓝
    )

    data class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speedX: Float,
        var speedY: Float,
        var color: Int,
        var alpha: Float,
        var alphaSpeed: Float
    )

    private val particles = mutableListOf<Particle>()
    private val particleColors = intArrayOf(
        Color.parseColor("#FF007F"), // 霓虹粉红
        Color.parseColor("#00F0FF"), // 霓虹青色
        Color.parseColor("#7000FF"), // 亮紫色
        Color.parseColor("#FFD700")  // 金色
    )

    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgPaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            colors, null, Shader.TileMode.CLAMP
        )
        
        particles.clear()
        for (i in 0 until 60) {
            particles.add(createParticle(w, h, true))
        }
    }

    private fun createParticle(w: Int, h: Int, randomY: Boolean): Particle {
        return Particle(
            x = Random.nextFloat() * w,
            y = if (randomY) Random.nextFloat() * h else h.toFloat() + 20f,
            radius = Random.nextFloat() * 3f + 1.5f,
            speedX = (Random.nextFloat() - 0.5f) * 1.5f,
            speedY = -Random.nextFloat() * 4f - 1.5f, // 向上浮动
            color = particleColors[Random.nextInt(particleColors.size)],
            alpha = Random.nextInt(50, 255).toFloat(),
            alphaSpeed = (Random.nextFloat() - 0.5f) * 10f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制渐变背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 绘制粒子
        for (p in particles) {
            // 外层发光晕轮
            particlePaint.color = p.color
            particlePaint.alpha = (p.alpha * 0.3f).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.radius * 3f, particlePaint)
            
            // 内核高亮
            particlePaint.alpha = p.alpha.toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
        }
    }

    fun startAnimation() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    private fun updateParticles() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        for (p in particles) {
            p.x += p.speedX
            p.y += p.speedY
            
            // 闪烁效果
            p.alpha += p.alphaSpeed
            if (p.alpha > 255f) {
                p.alpha = 255f
                p.alphaSpeed = -Math.abs(p.alphaSpeed)
            } else if (p.alpha < 50f) {
                p.alpha = 50f
                p.alphaSpeed = Math.abs(p.alphaSpeed)
            }

            // 出界则重置到屏幕底部
            if (p.y < -p.radius * 4 || p.x < -p.radius * 4 || p.x > w + p.radius * 4) {
                val newP = createParticle(width.toInt(), height.toInt(), false)
                p.x = newP.x
                p.y = newP.y
                p.radius = newP.radius
                p.speedX = newP.speedX
                p.speedY = newP.speedY
                p.color = newP.color
                p.alpha = newP.alpha
                p.alphaSpeed = newP.alphaSpeed
            }
        }
    }
}
