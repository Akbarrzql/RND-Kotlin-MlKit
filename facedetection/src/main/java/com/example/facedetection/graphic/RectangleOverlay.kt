package com.example.facedetection.graphic

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.facedetection.utils.CameraUtils

class RectangleOverlay(
    private val overlay: GraphicOverlay<*>,
    private val face: com.google.mlkit.vision.face.Face,
    private val rect: Rect,
    private val name: String
) : GraphicOverlay.Graphic(overlay) {

    private val boxPaint : Paint = Paint()

    init {
        boxPaint.color = Color.GREEN
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 3.0f
    }

    override fun draw(canvas: Canvas) {
        val rect = CameraUtils.calculateRect(
            overlay,
            rect.height().toFloat(),
            rect.width().toFloat(),
            face.boundingBox
        )

        // Draw the rectangle.
        canvas.drawRect(rect, boxPaint)

        // Draw the name inside the rectangle.
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
        }
        canvas.drawText(name, rect.left, rect.bottom, textPaint)
    }

}