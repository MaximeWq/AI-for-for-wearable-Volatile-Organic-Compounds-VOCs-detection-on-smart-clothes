package com.mawissocq.voc_acquisition3

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SelectionView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var endX: Float = 0f
    private var endY: Float = 0f
    private var triangleArea: Rect? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                endX = event.x
                endY = event.y
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                endX = event.x
                endY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val left = Math.min(startX, endX).toInt()
                val top = Math.min(startY, endY).toInt()
                val right = Math.max(startX, endX).toInt()
                val bottom = Math.max(startY, endY).toInt()
                triangleArea = Rect(left, top, right, bottom)
                invalidate()
            }
        }
        return true
    }

    fun getSelectionRect(): Rect? {
        return triangleArea
    }

    private fun drawSelectionRect(canvas: Canvas) {
        triangleArea?.let { canvas.drawRect(it, paint) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSelectionRect(canvas)
    }
}
