package com.zjgsu.treehole.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class SphereSoul(
    val id: String,
    val nickname: String,
    val mood: String
)

private data class ProjectedNode(
    val index: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val z: Float,
    val color: Int
)

class StarrySkyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val density = context.resources.displayMetrics.density
    private val palette = intArrayOf(
        0xFFA5F3FC.toInt(), 0xFFBFDBFE.toInt(), 0xFFFBCFE8.toInt(),
        0xFFC4B5FD.toInt(), 0xFF86EFAC.toInt(), 0xFFFDE68A.toInt()
    )
    private val fallbackSouls = listOf(
        SphereSoul("f1", "小新", "今晚有点失眠，想找个能聊得来的人。"),
        SphereSoul("f2", "依彤", "白天很坚强，夜里还是会脆弱。"),
        SphereSoul("f3", "秋刀鱼", "最近压力很大，想找个温柔的人说说话。"),
        SphereSoul("f4", "可乐不加冰", "一个人久了，也会想被温柔回应。"),
        SphereSoul("f5", "婉婉", "想认真认识一些有趣的灵魂。"),
        SphereSoul("f6", "清颜", "有些话不想打扰朋友，只想安静倾诉。")
    )

    private var souls: List<SphereSoul> = fallbackSouls
    private var points: List<FloatArray> = buildFibonacciPoints(souls.size)
    private var projectedNodes: List<ProjectedNode> = emptyList()

    private var rotateY = 0.22f
    private var rotateX = -0.12f
    private var autoRotateSpeed = 0.0026f
    private var velocityY = 0f
    private var velocityX = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var dragging = false

    private var onOrbClickListener: ((Int, String) -> Unit)? = null

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * density
        color = 0x45A7B5FF
    }
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
    }

    private val stars = List(120) { i ->
        floatArrayOf(
            ((i * 73) % 1000) / 1000f,
            ((i * 211) % 1000) / 1000f,
            ((i * 37) % 10) / 10f + 0.4f
        )
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 16L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            updateRotation()
            invalidate()
        }
    }

    init {
        isClickable = true
    }

    fun setOnOrbClickListener(listener: (Int, String) -> Unit) {
        onOrbClickListener = listener
    }

    fun setSouls(list: List<SphereSoul>) {
        souls = if (list.isEmpty()) fallbackSouls else list
        points = buildFibonacciPoints(souls.size)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                lastTouchX = event.x
                lastTouchY = event.y
                velocityX = 0f
                velocityY = 0f
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y
                rotateY += dx * 0.006f
                rotateX += dy * 0.0046f
                rotateX = rotateX.coerceIn(-1.15f, 1.15f)
                velocityY = dx * 0.00025f
                velocityX = dy * 0.00018f
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                dragging = false
                val hit = findHitNode(event.x, event.y)
                if (hit != null) {
                    performClick()
                    onOrbClickListener?.invoke(hit.index, souls[hit.index].mood)
                    velocityX = 0f
                    velocityY = 0f
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w * 0.5f
        val cy = h * 0.54f
        val sphereR = min(w, h) * 0.38f

        drawBackgroundStars(canvas, w, h)
        drawSphere(canvas, cx, cy, sphereR)

        val projected = projectNodes(cx, cy, sphereR).sortedBy { it.z }
        projectedNodes = projected

        projected.forEach { node ->
            drawNode(canvas, node)
        }
        projected.forEach { node ->
            if (node.z > -0.08f) {
                val soul = souls[node.index]
                textPaint.alpha = (140 + ((node.z + 1f) * 55f)).toInt().coerceIn(100, 255)
                canvas.drawText(soul.nickname, node.x, node.y - node.radius - 7f * density, textPaint)
            }
        }
    }

    private fun updateRotation() {
        if (!dragging) {
            rotateY += autoRotateSpeed + velocityY
            rotateX += velocityX
            rotateX = rotateX.coerceIn(-1.15f, 1.15f)
            velocityY *= 0.95f
            velocityX *= 0.92f
            if (kotlin.math.abs(velocityY) < 0.00002f) velocityY = 0f
            if (kotlin.math.abs(velocityX) < 0.00002f) velocityX = 0f
        }
    }

    private fun buildFibonacciPoints(count: Int): List<FloatArray> {
        if (count <= 1) return listOf(floatArrayOf(0f, 0f, 1f))
        val goldenAngle = PI * (3 - sqrt(5.0))
        return List(count) { i ->
            val y = 1f - (2f * (i + 0.5f) / count.toFloat())
            val radius = sqrt(max(0f, 1f - y * y))
            val theta = (goldenAngle * i).toFloat()
            val x = cos(theta) * radius
            val z = sin(theta) * radius
            floatArrayOf(x.toFloat(), y, z.toFloat())
        }
    }

    private fun projectNodes(cx: Float, cy: Float, r: Float): List<ProjectedNode> {
        val sinY = sin(rotateY)
        val cosY = cos(rotateY)
        val sinX = sin(rotateX)
        val cosX = cos(rotateX)
        val camera = 2.8f

        return points.mapIndexed { index, p ->
            val x = p[0]
            val y = p[1]
            val z = p[2]

            val x1 = x * cosY + z * sinY
            val z1 = -x * sinY + z * cosY
            val y2 = y * cosX - z1 * sinX
            val z2 = y * sinX + z1 * cosX

            val perspective = camera / (camera - z2)
            val px = cx + x1 * r * perspective
            val py = cy + y2 * r * 0.78f * perspective
            val scale = (0.55f + (z2 + 1f) * 0.45f).coerceIn(0.55f, 1.6f)
            val size = (3.6f + scale * 3.4f) * density
            val color = palette[kotlin.math.abs(souls[index].id.hashCode()) % palette.size]
            ProjectedNode(index, px.toFloat(), py.toFloat(), size.toFloat(), z2.toFloat(), color)
        }
    }

    private fun drawBackgroundStars(canvas: Canvas, w: Float, h: Float) {
        stars.forEachIndexed { i, s ->
            val tw = (sin((rotateY * 8f) + i * 0.31f) + 1f) * 0.5f
            starPaint.alpha = (50 + tw * 145).toInt().coerceIn(40, 200)
            canvas.drawCircle(s[0] * w, s[1] * h, s[2] * density * 0.45f, starPaint)
        }
    }

    private fun drawSphere(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        spherePaint.shader = RadialGradient(
            cx,
            cy - r * 0.22f,
            r * 1.2f,
            intArrayOf(0x3C6EA8FF, 0x1C27418A, 0x00000000),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 1.1f, spherePaint)

        canvas.drawOval(cx - r, cy - r * 0.76f, cx + r, cy + r * 0.76f, ringPaint)
        for (i in 1..3) {
            val ratio = i / 4f
            val ry = r * (0.72f - ratio * 0.14f)
            ringPaint.alpha = 24 + (3 - i) * 12
            canvas.drawOval(cx - r * (1f - ratio * 0.12f), cy - ry, cx + r * (1f - ratio * 0.12f), cy + ry, ringPaint)
        }
        for (i in 1..3) {
            val ratio = i / 4f
            val rx = r * ratio
            ringPaint.alpha = 20
            canvas.drawOval(cx - rx, cy - r * 0.76f, cx + rx, cy + r * 0.76f, ringPaint)
        }
    }

    private fun drawNode(canvas: Canvas, node: ProjectedNode) {
        val haloR = node.radius * (1.9f + max(0f, node.z) * 0.4f)
        haloPaint.shader = RadialGradient(
            node.x, node.y, haloR,
            intArrayOf((node.color and 0x00FFFFFF) or (0x55 shl 24), Color.TRANSPARENT),
            floatArrayOf(0.1f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(node.x, node.y, haloR, haloPaint)

        nodePaint.shader = null
        nodePaint.color = node.color
        nodePaint.alpha = (130 + (node.z + 1f) * 60).toInt().coerceIn(120, 255)
        canvas.drawCircle(node.x, node.y, node.radius, nodePaint)

        nodePaint.color = blendWithWhite(node.color, 0.62f)
        nodePaint.alpha = 230
        canvas.drawCircle(node.x, node.y, node.radius * 0.43f, nodePaint)
    }

    private fun findHitNode(x: Float, y: Float): ProjectedNode? {
        return projectedNodes
            .sortedByDescending { it.z }
            .firstOrNull { node ->
                val d = sqrt((x - node.x).pow(2) + (y - node.y).pow(2))
                d <= node.radius * 1.9f
            }
    }

    private fun blendWithWhite(color: Int, ratio: Float): Int {
        val r = (Color.red(color) * (1f - ratio) + 255f * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - ratio) + 255f * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - ratio) + 255f * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
