package com.zjgsu.treehole.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.zjgsu.treehole.R
import kotlin.math.*
import kotlin.random.Random

data class Orb(
    var cx: Float,
    var cy: Float,
    val radius: Float,
    val ringColor: Int,
    val glowColor: Int,
    val phase: Float,
    val speedX: Float,
    val speedY: Float,
    val pulsePhase: Float = Random.nextFloat() * PI.toFloat() * 2f
)

data class StarParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val twinkleSpeed: Float,
    val phase: Float
)

data class ShootingStar(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    var length: Float,
    var alpha: Float,
    var active: Boolean = false,
    val tailColor: Int
)

class StarrySkyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private var onOrbClickListener: ((Int) -> Unit)? = null

    // Enhanced orbs - more variety and positions
    private val orbs = listOf(
        Orb(0.20f, 0.12f, 22f, 0xFF60A5FA.toInt(), 0x2060A5FA, 0f, 0.4f, 0.6f),
        Orb(0.75f, 0.08f, 24f, 0xFFFBBF24.toInt(), 0x20FBBF24, 1.2f, -0.5f, 0.4f),
        Orb(0.55f, 0.22f, 20f, 0xFF34D399.toInt(), 0x2034D399, 2.5f, 0.3f, -0.5f),
        Orb(0.12f, 0.45f, 22f, 0xFFF472B6.toInt(), 0x20F472B6, 0.8f, -0.4f, 0.45f),
        Orb(0.85f, 0.40f, 24f, 0xFFA78BFA.toInt(), 0x20A78BFA, 3.1f, 0.45f, -0.35f),
        Orb(0.35f, 0.65f, 22f, 0xFF8B5CF6.toInt(), 0x208B5CF6, 1.7f, -0.3f, 0.55f),
        Orb(0.68f, 0.72f, 20f, 0xFFFBBF24.toInt(), 0x20FBBF24, 4.0f, 0.35f, -0.45f),
        Orb(0.45f, 0.85f, 22f, 0xFFEC4899.toInt(), 0x20EC4899, 2.0f, 0.25f, 0.4f),
        Orb(0.88f, 0.58f, 18f, 0xFF34D399.toInt(), 0x2034D399, 3.5f, -0.35f, 0.3f)
    )

    // Stars - more visible and twinkling
    private val stars = List(150) {
        StarParticle(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            size = Random.nextFloat() * 2.5f + 1f,
            alpha = Random.nextFloat() * 0.5f + 0.3f,
            twinkleSpeed = Random.nextFloat() * 2f + 0.5f,
            phase = Random.nextFloat() * PI.toFloat() * 2f
        )
    }

    // Shooting stars - reduced frequency
    private val shootingStars = List(2) {
        ShootingStar(
            x = Random.nextFloat(),
            y = Random.nextFloat() * 0.3f,
            speedX = Random.nextFloat() * 3f + 2f,
            speedY = Random.nextFloat() * 2f + 1f,
            length = Random.nextFloat() * 60f + 40f,
            alpha = 0f,
            tailColor = when (Random.nextInt(4)) {
                0 -> 0xFFFFD700.toInt() // Gold
                1 -> 0xFFFFFFFF.toInt() // White
                2 -> 0xFF60A5FA.toInt() // Blue
                else -> 0xFFFBBF24.toInt() // Yellow
            }
        )
    }

    // Nebula clouds for background atmosphere - more visible
    private val nebulae = List(3) {
        Nebula(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            radiusX = Random.nextFloat() * 200f + 100f,
            radiusY = Random.nextFloat() * 150f + 80f,
            color = when (Random.nextInt(4)) {
                0 -> 0x2560A5FA
                1 -> 0x25F472B6
                2 -> 0x2534D399
                else -> 0x25A78BFA
            },
            phase = Random.nextFloat() * PI.toFloat() * 2f,
            speed = Random.nextFloat() * 0.3f + 0.1f
        )
    }

    data class Nebula(
        val x: Float,
        val y: Float,
        val radiusX: Float,
        val radiusY: Float,
        val color: Int,
        val phase: Float,
        val speed: Float
    )

    private val sparkleDrawable = ContextCompat.getDrawable(context, R.drawable.ic_sparkle)

    // Paints
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        maskFilter = BlurMaskFilter(8f * density, BlurMaskFilter.Blur.NORMAL)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val shootingStarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val nebulaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var animProgress = 0f
    private var lastShootingStarTime = 0L
    private var orbPulseMap = mutableMapOf<Int, Float>() // For tap feedback

    private val animator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
        duration = 8000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { anim ->
            animProgress = anim.animatedValue as Float
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    fun setOnOrbClickListener(listener: (Int) -> Unit) {
        onOrbClickListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val touchX = event.x
            val touchY = event.y
            orbs.forEachIndexed { index, orb ->
                val ox = orb.cx * width + sin(animProgress + orb.phase) * orb.speedX * 15f * density
                val oy = orb.cy * height + cos(animProgress + orb.phase) * orb.speedY * 15f * density
                val r = orb.radius * density
                val dist = sqrt((touchX - ox).pow(2) + (touchY - oy).pow(2))
                if (dist <= r * 1.6f) {
                    // Trigger pulse animation
                    orbPulseMap[index] = 1f
                    // Vibrate feedback
                    performClick()
                    onOrbClickListener?.invoke(index)
                    return true
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateShootingStars() {
        val currentTime = System.currentTimeMillis()
        shootingStars.forEach { star ->
            if (!star.active && currentTime - lastShootingStarTime > Random.nextLong(6000, 12000)) {
                // Activate a shooting star
                star.x = Random.nextFloat() * 0.5f
                star.y = Random.nextFloat() * 0.2f
                star.speedX = Random.nextFloat() * 4f + 3f
                star.speedY = Random.nextFloat() * 3f + 2f
                star.length = Random.nextFloat() * 80f + 50f
                star.alpha = 1f
                star.active = true
                lastShootingStarTime = currentTime
            }

            if (star.active) {
                star.x += star.speedX * density * 0.016f
                star.y += star.speedY * density * 0.016f
                star.alpha -= 0.008f

                if (star.alpha <= 0f || star.x > 1.5f || star.y > 1.2f) {
                    star.active = false
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Draw nebula clouds (background atmosphere)
        nebulae.forEach { nebula ->
            val nebulaX = nebula.x * w + sin(animProgress * nebula.speed + nebula.phase) * 20f * density
            val nebulaY = nebula.y * h + cos(animProgress * nebula.speed + nebula.phase) * 15f * density
            nebulaPaint.shader = RadialGradient(
                nebulaX, nebulaY, nebula.radiusX * density,
                intArrayOf(nebula.color, Color.TRANSPARENT),
                floatArrayOf(0.1f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(
                nebulaX - nebula.radiusX * density,
                nebulaY - nebula.radiusY * density,
                nebulaX + nebula.radiusX * density,
                nebulaY + nebula.radiusY * density,
                nebulaPaint
            )
        }

        // Draw shooting stars
        updateShootingStars()
        shootingStars.forEach { star ->
            if (star.active && star.alpha > 0) {
                val startX = star.x * w
                val startY = star.y * h
                val endX = startX - star.speedX * star.length * density * 0.1f
                val endY = startY - star.speedY * star.length * density * 0.1f

                shootingStarPaint.color = star.tailColor
                shootingStarPaint.alpha = (star.alpha * 255).toInt().coerceIn(0, 255)
                shootingStarPaint.strokeWidth = 2f * density

                // Draw trail
                canvas.drawLine(startX, startY, endX, endY, shootingStarPaint)

                // Draw head glow
                fillPaint.color = star.tailColor
                fillPaint.alpha = (star.alpha * 200).toInt().coerceIn(0, 255)
                canvas.drawCircle(startX, startY, 3f * density, fillPaint)
            }
        }

        // Draw star particles with enhanced twinkle
        stars.forEach { star ->
            val twinkle = (sin(animProgress * star.twinkleSpeed + star.phase) + 1f) / 2f
            val alpha = (star.alpha * (0.3f + twinkle * 0.7f) * 255).toInt().coerceIn(0, 255)
            starPaint.alpha = alpha
            val sx = star.x * w
            val sy = star.y * h
            val size = star.size * density
            canvas.drawCircle(sx, sy, size / 2f, starPaint)
        }

        // Draw each orb with enhanced effects
        orbs.forEachIndexed { index, orb ->
            val floatX = sin(animProgress + orb.phase) * orb.speedX * 12f * density
            val floatY = cos(animProgress + orb.phase) * orb.speedY * 12f * density
            val cx = orb.cx * w + floatX
            val cy = orb.cy * h + floatY

            // Get pulse factor if this orb was tapped
            val pulseFactor = orbPulseMap[index] ?: 0f
            val r = orb.radius * density * (1f + pulseFactor * 0.3f)

            // 1. Soft outer glow - subtle
            outerGlowPaint.shader = RadialGradient(
                cx, cy, r * 1.8f,
                intArrayOf(orb.glowColor, Color.TRANSPARENT),
                floatArrayOf(0.2f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r * 1.8f, outerGlowPaint)

            // 2. Dark background
            fillPaint.shader = null
            fillPaint.color = 0xFF0D1117.toInt()
            fillPaint.alpha = 120
            canvas.drawCircle(cx, cy, r, fillPaint)

            // 3. Blurred neon halo
            glowPaint.color = orb.ringColor
            glowPaint.alpha = 180
            canvas.drawCircle(cx, cy, r, glowPaint)

            // 4. Thin opaque ring
            ringPaint.color = orb.ringColor
            ringPaint.alpha = 240
            canvas.drawCircle(cx, cy, r, ringPaint)

            // 5. Sparkle vector
            sparkleDrawable?.let { drawable ->
                val size = (r * 0.85f).toInt()
                val left = (cx - size / 2f).toInt()
                val top = (cy - size / 2f).toInt()
                val right = (cx + size / 2f).toInt()
                val bottom = (cy + size / 2f).toInt()

                drawable.setBounds(left, top, right, bottom)
                val blendColor = blendColors(Color.WHITE, orb.ringColor, 0.4f)
                drawable.setTint(blendColor)
                drawable.draw(canvas)
            }

            // 6. Companion dots
            fillPaint.color = blendColors(Color.WHITE, orb.ringColor, 0.6f)
            fillPaint.alpha = 220
            val dotDist = r * 0.42f
            val dotSize = 1.2f * density

            val diffs = arrayOf(
                floatArrayOf(-1f, -1f),
                floatArrayOf(1f, -1f),
                floatArrayOf(-1f, 1f),
                floatArrayOf(1f, 1f)
            )
            for (d in diffs) {
                val offsetDist = dotDist * (0.85f + Random(orb.ringColor.toLong() + index).nextFloat() * 0.3f)
                val dotX = cx + d[0] * offsetDist * 0.707f
                val dotY = cy + d[1] * offsetDist * 0.707f
                canvas.drawCircle(dotX, dotY, dotSize, fillPaint)
            }

            // Update pulse animation
            if (pulseFactor > 0f) {
                orbPulseMap[index] = pulseFactor - 0.05f
                if (orbPulseMap[index]!! <= 0f) {
                    orbPulseMap.remove(index)
                }
            }
        }
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = (Color.red(color1) * ratio + Color.red(color2) * inverseRatio).toInt()
        val g = (Color.green(color1) * ratio + Color.green(color2) * inverseRatio).toInt()
        val b = (Color.blue(color1) * ratio + Color.blue(color2) * inverseRatio).toInt()
        return Color.rgb(r, g, b)
    }
}
