package com.example.facedetection.camera

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.facedetection.data.FaceInfo
import com.example.facedetection.data.model.FaceDetectionDatabase
import com.example.facedetection.graphic.GraphicOverlay
import com.example.facedetection.graphic.RectangleOverlay
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class CameraAnalyzer(
    private val overlay: GraphicOverlay<*>,
    private val context: Context
) : BaseCameraAnalyzer<List<Face>>(){

    override val graphicOverlay: GraphicOverlay<*>
        get() = overlay

    private val detectedFaces = mutableListOf<FaceInfo>()

    // build FaceDetectorOptions
    private val cameraOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    // set options in client
    private val detector = FaceDetection.getClient(cameraOptions)

    private val database = FaceDetectionDatabase.getDatabase(context)
    private val allFaces: LiveData<List<com.example.facedetection.data.model.FaceDetection>> = database.faceDetectionDao().readAllData()


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
        val faceDao = database.faceDetectionDao()
        val faceList = allFaces.value

        results.forEach { face ->
            Log.d(TAG, "Face contours: ${face.allContours}")
            Log.d(TAG, "Face landmarks: ${face.allLandmarks}")
            val existingFace = faceList?.find { it.id == (face.trackingId?.toInt() ?: -1) }
            val faceName = existingFace?.name ?: ""

            val faceGraphic = RectangleOverlay(graphicOverlay, face, rect, "")
            graphicOverlay.add(faceGraphic)

            if (existingFace != null) {
                Log.d(TAG, "Detected face with name: $faceName, and image path: ${existingFace.image}")
            }
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