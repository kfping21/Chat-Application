package com.zjgsu.treehole.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator

/**
 * 按钮点击缩放动画工具类
 */
object ClickAnimations {

    /**
     * 为 FAB 添加按下缩放动画
     */
    fun addFabPressAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    animateScale(v, 0.9f, 80)
                    false
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    animateScale(v, 1.0f, 150)
                    false
                }
                else -> false
            }
        }
    }

    /**
     * 为按钮添加按下缩放动画
     */
    fun addButtonPressAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    animateScale(v, 0.95f, 60)
                    false
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    animateScale(v, 1.0f, 100)
                    false
                }
                else -> false
            }
        }
    }

    /**
     * 点赞动画 - 心形放大回弹效果
     */
    fun animateLike(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 0.9f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f, 0.9f, 1.1f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 400
            interpolator = OvershootInterpolator(2f)
            start()
        }
    }

    /**
     * 通用缩放动画
     */
    private fun animateScale(view: View, targetScale: Float, duration: Long) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", view.scaleX, targetScale)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", view.scaleY, targetScale)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            this.duration = duration
            interpolator = OvershootInterpolator(1.5f)
            start()
        }
    }
}
