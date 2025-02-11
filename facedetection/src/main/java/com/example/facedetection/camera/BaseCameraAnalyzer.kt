package com.example.facedetection.camera

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.facedetection.graphic.GraphicOverlay
import com.example.facedetection.graphic.RectangleOverlay
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection

abstract class BaseCameraAnalyzer<T : List<Face>> : ImageAnalysis.Analyzer {

    abstract val graphicOverlay : GraphicOverlay<*>

    // ovverride analyze from ImageAnalysis.Analyzer
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        mediaImage?.let { image ->
            detectInImage(InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { results ->
                    onSuccess(results, graphicOverlay, image.cropRect)
                    imageProxy.close()
                }
                .addOnFailureListener {
                    onFailure(it)
                    imageProxy.close()
                }
        }
    }

    // use InputImage
    protected abstract fun detectInImage(image : InputImage) : Task<T>

    abstract fun stop()

    // function that will be affected if error occurs
    protected abstract fun onSuccess(
        results : List<Face>,
        graphicOverlay: GraphicOverlay<*>,
        rect : Rect
    )

    // function that will be affected if error occurs
    protected abstract fun onFailure(e : Exception)

}