/*
 * Copyright 2023 Shubham Panchal
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.facerecognitioncomparison

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.camera.core.CameraSelector
import androidx.core.graphics.toRectF
import kotlin.math.max

// Defines an overlay on which the boxes and text will be drawn.
class BoundingBoxOverlay( context: Context , attributeSet: AttributeSet )
    : SurfaceView( context , attributeSet ) , SurfaceHolder.Callback {

    // Variables used to compute output2overlay transformation matrix
    // These are assigned in FrameAnalyser.kt
    var areDimsInit = false
    var frameHeight = 0
    var frameWidth = 0

    var cameraFacing : Int = CameraSelector.LENS_FACING_FRONT

    // This var is assigned in FrameAnalyser.kt
    var faceBoundingBoxes: ArrayList<Prediction>? = null

    // Determines whether or not "mask" or "no mask" should be displayed.
    var drawMaskLabel = true

    private var output2OverlayTransform: Matrix = Matrix()

    // Paint for boxes and text
    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        strokeWidth = 10.0f
    }
    private val textPaint = Paint().apply {
        strokeWidth = 2.0f
        textSize = 32f
        color = Color.WHITE
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        TODO("Not yet implemented")
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }


    override fun surfaceDestroyed(holder: SurfaceHolder) {
        TODO("Not yet implemented")
    }


    override fun onDraw(canvas: Canvas) {
        if (faceBoundingBoxes != null) {
            if (!areDimsInit) {
                val viewWidth = canvas!!.width.toFloat()
                val viewHeight = canvas.height.toFloat()
                val xFactor: Float = viewWidth / frameWidth.toFloat()
                val yFactor: Float = viewHeight / frameHeight.toFloat()
                // Scale and mirror the coordinates ( required for front lens )
                output2OverlayTransform.preScale(xFactor, yFactor)
                if( cameraFacing == CameraSelector.LENS_FACING_FRONT ) {
                    output2OverlayTransform.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)
                }
                areDimsInit = true
            }
            else {
                for (face in faceBoundingBoxes!!) {
                    val boundingBox = face.bbox.toRectF()

                    // Calculate the center of the bounding box
                    val centerX = boundingBox.centerX()
                    val centerY = boundingBox.centerY()

                    // Calculate the square bounding box size
                    val boxSize = max(boundingBox.width(), boundingBox.height()) * 1.8f

                    // Adjust the bounding box to be square and centered
                    boundingBox.left = centerX - boxSize / 2
                    boundingBox.top = centerY - boxSize / 2
                    boundingBox.right = centerX + boxSize / 2
                    boundingBox.bottom = centerY + boxSize / 2

                    // Map the transformed bounding box
                    output2OverlayTransform.mapRect(boundingBox)

                    // Set the style of the boxPaint to STROKE for drawing only the outline
                    boxPaint.style = Paint.Style.STROKE

                    // Draw the rectangular bounding box outline
                    canvas?.drawRect(boundingBox, boxPaint)

                    // Draw the text (name) above the top-left corner of the bounding box
                    canvas?.drawText(
                        face.label,
                        boundingBox.left,
                        boundingBox.top - 16f,
                        textPaint
                    )

                    if (drawMaskLabel) {
                        // Draw the mask label below the bottom-center of the bounding box
                        canvas?.drawText(
                            face.maskLabel,
                            boundingBox.centerX(),
                            boundingBox.bottom + 32f,
                            textPaint
                        )
                    }

                    // Reset the style to FILL for subsequent drawings
                    boxPaint.style = Paint.Style.FILL
                }
            }
        }
    }
}
