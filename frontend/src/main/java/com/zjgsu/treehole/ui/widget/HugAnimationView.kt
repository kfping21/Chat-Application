package com.zjgsu.treehole.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class HugAnimationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var keepVisibleAtEnd = false

    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF0000") // Classic red
        style = Paint.Style.FILL
    }

    private val whiteOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C0C0C0") // Gray outline
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#999999")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private var isPlaying = false
    private var progress = 0f
    
    // Paths for complex shapes
    private val redBodyPath = Path()
    private val whiteBodyPath = Path()
    private val redArmPath = Path()
    private val smilePath = Path()

    fun playAnimation() {
        if (isPlaying) return
        isPlaying = true
        visibility = View.VISIBLE
        alpha = 1f
        
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
        }
        
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (!keepVisibleAtEnd) {
                    animate().alpha(0f).setDuration(300).withEndAction {
                        visibility = View.INVISIBLE
                        isPlaying = false
                        progress = 0f
                    }.start()
                } else {
                    isPlaying = false
                }
            }
        })
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isPlaying && progress == 0f && !keepVisibleAtEnd) return

        val currentProgress = if (!isPlaying && keepVisibleAtEnd && progress == 0f) 1f else progress

        val cx = width / 2f
        val cy = height / 2f
        
        val size = width.toFloat() * 0.75f

        val whiteX = cx - size * 0.15f
        val redXBase = cx + size * 0.45f
        val redX = redXBase - (size * 0.45f * Math.min(currentProgress * 2f, 1f)) 

        val headRadius = size * 0.18f
        val bodyTop = cy + headRadius * 0.3f
        val bodyBottom = cy + size * 0.45f
        val whiteBodyWidth = size * 0.22f
        val redBodyWidth = size * 0.26f

        // 1. Draw White Figure (Behind)
        whiteBodyPath.reset()
        whiteBodyPath.moveTo(whiteX - whiteBodyWidth, bodyBottom)
        whiteBodyPath.lineTo(whiteX - whiteBodyWidth, bodyTop + size * 0.15f)
        whiteBodyPath.quadTo(whiteX, bodyTop - size * 0.1f, whiteX + whiteBodyWidth, bodyTop + size * 0.15f)
        whiteBodyPath.lineTo(whiteX + whiteBodyWidth, bodyBottom)
        canvas.drawPath(whiteBodyPath, whiteOutlinePaint)

        val whiteHeadY = cy - headRadius * 0.8f
        canvas.drawCircle(whiteX, whiteHeadY, headRadius, whiteOutlinePaint)

        val eyeY = cy - headRadius * 1.0f
        val eyeOffsetX = headRadius * 0.35f
        canvas.drawPoint(whiteX - eyeOffsetX, eyeY, facePaint) // Left eye
        canvas.drawPoint(whiteX + eyeOffsetX, eyeY, facePaint) // Right eye

        smilePath.reset()
        if (currentProgress > 0.4f) {
            val smileProgress = Math.min((currentProgress - 0.4f) * 4f, 1f)
            val mouthY = cy - headRadius * 0.6f
            smilePath.moveTo(whiteX - headRadius * 0.3f, mouthY)
            smilePath.quadTo(whiteX, mouthY + headRadius * 0.4f * smileProgress, whiteX + headRadius * 0.3f, mouthY)
            canvas.drawPath(smilePath, facePaint)
        } else {
            val mouthY = cy - headRadius * 0.6f
            canvas.drawLine(whiteX - headRadius * 0.2f, mouthY, whiteX + headRadius * 0.2f, mouthY, facePaint)
        }

        // 2. Draw Red Figure (In Front)
        redBodyPath.reset()
        redBodyPath.moveTo(redX - redBodyWidth, bodyBottom)
        redBodyPath.lineTo(redX - redBodyWidth, bodyTop + size * 0.1f)
        redBodyPath.quadTo(redX, bodyTop - size * 0.15f, redX + redBodyWidth, bodyTop + size * 0.1f)
        redBodyPath.lineTo(redX + redBodyWidth, bodyBottom)
        canvas.drawPath(redBodyPath, redPaint)
        
        canvas.drawCircle(redX, cy - headRadius * 0.9f, headRadius * 1.05f, redPaint)

        // 3. Draw Red Arm (smoothly connected)
        if (currentProgress > 0.1f) {
            val armProgress = Math.min((currentProgress - 0.1f) * 2.5f, 1f)
            // Start right at the edge of the red body so the connection is smooth
            val armStartX = redX - redBodyWidth + size * 0.05f 
            val armStartY = cy + size * 0.05f
            val armTargetX = whiteX - whiteBodyWidth * 1.2f
            val armTargetY = cy + size * 0.15f
            
            val armControlX = whiteX + size * 0.1f
            val armControlY = cy + size * 0.3f

            val currentArmX = armStartX + (armTargetX - armStartX) * armProgress
            val currentArmY = armStartY + (armTargetY - armStartY) * armProgress

            redArmPath.reset()
            redArmPath.moveTo(armStartX, armStartY)
            redArmPath.quadTo(armControlX, armControlY, currentArmX, currentArmY)
            
            val originalStrokeWidth = redPaint.strokeWidth
            val originalStyle = redPaint.style
            val originalCap = redPaint.strokeCap
            
            redPaint.style = Paint.Style.STROKE
            redPaint.strokeWidth = size * 0.12f // very thick arm
            redPaint.strokeCap = Paint.Cap.ROUND
            canvas.drawPath(redArmPath, redPaint)
            
            redPaint.style = originalStyle
            redPaint.strokeWidth = originalStrokeWidth
            redPaint.strokeCap = originalCap
        }

        // 4. Draw Floating hearts
        if (currentProgress > 0.5f) {
            val heartProgress = Math.min((currentProgress - 0.5f) * 2f, 1f)
            val heartY = cy - headRadius * 2.5f - size * 0.2f * heartProgress
            val heartAlpha = (255 * (1f - heartProgress)).toInt()
            
            val originalAlpha = redPaint.alpha
            redPaint.alpha = heartAlpha
            
            val heartSize = size * 0.08f
            val hx = cx - size * 0.1f
            val hy = heartY
            
            val heartPath = Path()
            heartPath.moveTo(hx, hy + heartSize * 0.3f)
            heartPath.cubicTo(hx - heartSize, hy - heartSize, hx - heartSize * 0.5f, hy - heartSize * 0.8f, hx, hy - heartSize * 0.2f)
            heartPath.cubicTo(hx + heartSize * 0.5f, hy - heartSize * 0.8f, hx + heartSize, hy - heartSize, hx, hy + heartSize * 0.3f)
            
            canvas.drawPath(heartPath, redPaint)
            redPaint.alpha = originalAlpha
        }
    }
}
