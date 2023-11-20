package com.example.facedetection.camera

import android.graphics.Rect
import android.util.Log
import com.example.facedetection.data.FaceInfo
import com.example.facedetection.graphic.GraphicOverlay
import com.example.facedetection.graphic.RectangleOverlay
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class CameraAnalyzer(
    private val overlay: GraphicOverlay<*>
) : BaseCameraAnalyzer<List<Face>>(){

    override val graphicOverlay: GraphicOverlay<*>
        get() = overlay

    private val detectedFaces = mutableListOf<FaceInfo>()

    // build FaceDetectorOptions
    private val cameraOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    // set options in client
    private val detector = FaceDetection.getClient(cameraOptions)

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e : Exception) {
            Log.e(TAG , "stop : $e")
        }
    }

    override fun onSuccess(results: List<Face>, graphicOverlay: GraphicOverlay<*>, rect: Rect) {
        graphicOverlay.clear()
        results.forEach {
//            val faceGraphic = RectangleOverlay(graphicOverlay, it, rect, "New Face")
//            graphicOverlay.add(faceGraphic)

            val existingFace = detectedFaces.find { faceInfo -> faceInfo.faceId == it.trackingId }
            val faceGraphic = RectangleOverlay(graphicOverlay, it, rect, existingFace?.name ?: "New Face")
            graphicOverlay.add(faceGraphic)
        }
        graphicOverlay.postInvalidate()
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "onFailure : $e")
    }

    companion object {
        private const val TAG = "CameraAnalyzer"
    }
}