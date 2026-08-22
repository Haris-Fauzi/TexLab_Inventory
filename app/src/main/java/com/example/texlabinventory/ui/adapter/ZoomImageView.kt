package com.example.texlabinventory.utils

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var matrix = Matrix()
    private var mode = NONE

    private var last = PointF()
    private var start = PointF()
    private var minScale = 1f
    private var maxScale = 4f
    private var m: FloatArray = FloatArray(9)

    private var viewWidth = 0
    private var viewHeight = 0
    private var saveScale = 1f
    private var origWidth = 0f
    private var origHeight = 0f

    private var mScaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var mGestureDetector: GestureDetector = GestureDetector(context, GestureListener())

    init {
        super.setClickable(true)
        imageMatrix = matrix
        scaleType = ScaleType.MATRIX
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (saveScale == 1f) {
            fitToScreen()
        }
    }

    // Detector Gestur untuk menangani Double Tap dan Single Tap (Klik)
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Jika gambar sedang di-zoom, reset ke skala default (1.0x)
            if (saveScale > minScale) {
                resetZoom()
            } else {
                // Jika gambar ukuran normal, zoom in cepat ke 2.5x di titik ketukan
                val targetScale = 2.5f
                val scaleFactor = targetScale / saveScale
                saveScale = targetScale
                matrix.postScale(scaleFactor, scaleFactor, e.x, e.y)
                fixTranslation()
            }
            imageMatrix = matrix
            invalidate()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            performClick()
            return true
        }
    }

    // Reset posisi dan skala gambar ke semula
    fun resetZoom() {
        saveScale = 1f
        fitToScreen()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= mScaleFactor
            if (saveScale > maxScale) {
                saveScale = maxScale
                mScaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                mScaleFactor = minScale / origScale
            }

            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
                matrix.postScale(mScaleFactor, mScaleFactor, (viewWidth / 2).toFloat(), (viewHeight / 2).toFloat())
            } else {
                matrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
            }

            fixTranslation()
            return true
        }
    }

    private fun fixTranslation() {
        matrix.getValues(m)
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]
        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), origHeight * saveScale)
        if (fixTransX != 0f || fixTransY != 0f) {
            matrix.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float

        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }

        if (trans < minTrans) return -trans + minTrans
        if (trans > maxTrans) return -trans + maxTrans
        return 0f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        mGestureDetector.onTouchEvent(event) // Deteksi gesture Double Tap & Single Tap

        matrix.getValues(m)
        val x = event.x
        val y = event.y

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                last.set(x, y)
                start.set(last)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val deltaX = x - last.x
                    val deltaY = y - last.y
                    val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), origWidth * saveScale)
                    val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), origHeight * saveScale)
                    matrix.postTranslate(fixTransX, fixTransY)
                    fixTranslation()
                    last.set(x, y)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> mode = NONE
            MotionEvent.ACTION_UP -> {
                mode = NONE
            }
        }
        imageMatrix = matrix
        invalidate()
        return true
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) 0f else delta
    }

    private fun fitToScreen() {
        val drawable = drawable ?: return
        if (drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) return

        val bmWidth = drawable.intrinsicWidth
        val bmHeight = drawable.intrinsicHeight

        val scaleX = viewWidth.toFloat() / bmWidth.toFloat()
        val scaleY = viewHeight.toFloat() / bmHeight.toFloat()
        val scale = Math.min(scaleX, scaleY)

        matrix.setScale(scale, scale)

        var redundantYSpace = viewHeight.toFloat() - scale * bmHeight.toFloat()
        var redundantXSpace = viewWidth.toFloat() - scale * bmWidth.toFloat()
        redundantYSpace /= 2f
        redundantXSpace /= 2f

        matrix.postTranslate(redundantXSpace, redundantYSpace)

        origWidth = viewWidth - 2 * redundantXSpace
        origHeight = viewHeight - 2 * redundantYSpace
        imageMatrix = matrix
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}