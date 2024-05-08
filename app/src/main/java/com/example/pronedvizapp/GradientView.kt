package com.example.pronedvizapp

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView


open class AnimatedGradientViewOnAuthorize(context: Context, attrs: AttributeSet) : View(context, attrs) {
    open val paint = Paint()
    open val colors = intArrayOf(Color.parseColor("#2A52BE"), Color.parseColor("#000000"), Color.parseColor("#0029FF"))
    open var colorPositions = floatArrayOf(0f, 0.5f, 1f)
    open var currentAngle = 0f

    init {
        val shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, colorPositions, Shader.TileMode.CLAMP)
        paint.shader = shader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    open fun animateGradientColors() {
        val animator = ValueAnimator.ofFloat(0f, 360f)
        animator.addUpdateListener { valueAnimator ->
            currentAngle = valueAnimator.animatedValue as Float
            updateGradientColors()
            invalidate()
        }
        animator.duration = 30000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.start()
    }

    open fun updateGradientColors() {
        val matrix = Matrix()
        matrix.setRotate(currentAngle, width.toFloat() / 2, height.toFloat() / 2)
        val shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, colorPositions, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(matrix)
        paint.shader = shader
    }
}

open class AnimatedGradientViewOnMain(context: Context, attrs: AttributeSet) : View(context, attrs) {
    open val paint = Paint()
    open val colors = intArrayOf(Color.parseColor("#2A52BE"), Color.parseColor("#0500FF"), Color.parseColor("#212224"))
    open var colorPositions = floatArrayOf(0f, 0.2f, 0.7f)
    open var currentAngle = 0f

    init {
        val shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, colorPositions, Shader.TileMode.CLAMP)
        paint.shader = shader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    open fun animateGradientColors() {
        val animator = ValueAnimator.ofFloat(0f, 360f)
        animator.addUpdateListener { valueAnimator ->
            currentAngle = valueAnimator.animatedValue as Float
            updateGradientColors()
            invalidate()
        }
        animator.duration = 30000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.start()
    }

    open fun updateGradientColors() {
        val matrix = Matrix()
        matrix.setRotate(currentAngle, width.toFloat() / 2, height.toFloat() / 2)
        val shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, colorPositions, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(matrix)
        paint.shader = shader
    }
}


class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var startColor: Int = 0xFF00FF00.toInt()
    private var endColor: Int = 0xFFFFFF00.toInt()

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GradientTextView, defStyleAttr, 0)
        startColor = typedArray.getColor(R.styleable.GradientTextView_startColor, startColor)
        endColor = typedArray.getColor(R.styleable.GradientTextView_endColor, endColor)
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        val textPaint = paint
        textPaint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            startColor, endColor,
            Shader.TileMode.CLAMP
        )
        super.onDraw(canvas)
    }
}