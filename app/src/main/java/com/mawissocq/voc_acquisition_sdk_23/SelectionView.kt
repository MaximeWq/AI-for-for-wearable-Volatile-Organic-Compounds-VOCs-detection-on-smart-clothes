package com.mawissocq.voc_acquisition_sdk_23

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

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
        val x = event.x.toString()
        val y = event.y.toString()
        //val coordinatesTextView = findViewById<TextView>(R.id.coordinatesTextView)
        //coordinatesTextView.setText("Coordonnéesfeur1 : ${x} feur ${y}")
        //findViewById<TextView>(R.id.coordinatesTextView)?.text = "Coordonnéesfeur2 : ${x} feur ${y}"
        Log.d("Coordonnées", "Coordonnées : (${x}, ${y})")

        //val coordinatesTextView = findViewById<TextView>(R.id.coordinatesTextView)
        //coordinatesTextView.setText("Coordonnéesfeur1 :")


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
                println("Coordinates of selection rectangle: ($left, $top) - ($right, $bottom)")
                invalidate()
            }
        }
        return true
    }

    fun isSelectionRectVisible(): Boolean {
        return triangleArea != null
    }


    fun getSelectionRect(): Rect? {
        return triangleArea
    }

    private fun drawSelectionRect(canvas: Canvas) {
        triangleArea?.let { canvas.drawRect(it, paint) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        triangleArea?.let {
            val coordinates = "Coordonnéesfeur3 : (${it.left}, ${it.top}) - (${it.right}, ${it.bottom})"
            findViewById<TextView>(R.id.coordinatesTextView)?.text = coordinates
        }
        drawSelectionRect(canvas)
    }
}