package com.revenco.scableableimageview

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ScaleGestureDetectorCompat
import kotlin.math.max
import kotlin.math.min

/**
 *  Copyright © 2021/6/2
 *  author: chenqi
 */
val Float.dp2px: Float
    get() {
        return TypedValue.applyDimension(
            COMPLEX_UNIT_DIP,
            this,
            Resources.getSystem().displayMetrics
        )
    }

val Int.dp2px: Float
    get() {
        return TypedValue.applyDimension(
            COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )
    }

private fun getAvatar(resources: Resources, width: Int): Bitmap {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeResource(resources, R.drawable.icon_dingdang, options)
    options.inTargetDensity = width
    options.inDensity = options.outWidth
    options.inJustDecodeBounds = false

    return BitmapFactory.decodeResource(resources, R.drawable.icon_dingdang, options)
}

class ScaleAbleImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    def: Int = 0
) : View(context, attributeSet, def) {

    private val bitmap: Bitmap
        get() = getAvatar(resources, 300)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    //手指相关
    private val gestureListener = ViewGestureListenerImpl()
    private val gestureDetectorCompat = GestureDetectorCompat(context, gestureListener)

    //双指缩放相关
    private val scaleGestureListener = ViewScaleGestureListenerImpl()
    private val scaleGestureDetector = ScaleGestureDetector(context, scaleGestureListener)

    // ---------------------放大图片相关--------------------
    private var big = false
    private var smallScale = 0f
    private var bigScale = 0f
    private var scaleCoefficient = 1.2f
    var currentScale = 0f
        set(value) {
            field = value
            invalidate()
        }

    //做一个放大的动画效果
    private val scaleAnimation: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(this, "currentScale", smallScale, bigScale)
    }

    // -------------------手指移动相关--------------------------
    private var moveX = 0f
    private var moveY = 0f


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetectorCompat.onTouchEvent(event)
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val bWidth = bitmap.width
        val bHeight = bitmap.height
        if (bWidth / width.toFloat() > bHeight / height.toFloat()) {
            smallScale = width / bWidth.toFloat()
            bigScale = height / bHeight.toFloat() * scaleCoefficient
        } else {
            smallScale = height / bHeight.toFloat()
            bigScale = width / bWidth.toFloat() * scaleCoefficient
        }
        currentScale = smallScale
    }

    override fun onDraw(canvas: Canvas) {
        //跟随手指移动逻辑
        canvas.translate(
            moveX * (currentScale - smallScale) / (bigScale - smallScale),
            moveY * (currentScale - smallScale) / (bigScale - smallScale)
        )
        //双击缩放逻辑
        canvas.scale(currentScale, currentScale, (width / 2).toFloat(), (height / 2).toFloat())
        canvas.drawBitmap(
            bitmap,
            (width / 2 - bitmap.width / 2.toFloat()),
            (height / 2 - bitmap.height / 2.toFloat()),
            paint
        )
    }

    private inner class ViewScaleGestureListenerImpl : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            currentScale *= detector.scaleFactor
            currentScale = min(currentScale, bigScale)
            currentScale = max(currentScale, smallScale)
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            //和双击缩放逻辑一致，捏撑跟手
            moveX = (detector.focusX - width / 2f) * (1 - bigScale / smallScale)
            moveY = (detector.focusY - height / 2f) * (1 - bigScale / smallScale)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
        }
    }

    private inner class ViewGestureListenerImpl : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            big = !big
            if (big) {
                moveX = (e.x - (width / 2)) * (1 - bigScale / smallScale)
                moveY = (e.y - (height / 2)) * (1 - bigScale / smallScale)
                fixImageBounds()
                scaleAnimation.setFloatValues(currentScale, bigScale)
                scaleAnimation.start()
            } else {
                scaleAnimation.setFloatValues(smallScale, currentScale)
                scaleAnimation.reverse()
            }
            return false
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            //跟随手指移动
            if (big) {
                moveX += -distanceX
                moveY += -distanceY
                fixImageBounds()
                invalidate()
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return false
        }
    }

    private fun fixImageBounds() {

        moveX = moveX.coerceAtLeast(-(bitmap.width * bigScale / 2 - width / 2))
        moveX = moveX.coerceAtMost(bitmap.width * bigScale / 2 - width / 2)

        moveY = moveY.coerceAtLeast(-(bitmap.height * bigScale / 2 - height / 2))
        moveY = moveY.coerceAtMost(bitmap.height * bigScale / 2 - height / 2)
    }
}