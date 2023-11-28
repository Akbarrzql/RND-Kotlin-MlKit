package com.example.facedetection.camera

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
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
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.acos
import kotlin.math.sqrt

class CameraAnalyzer(
    private val overlay: GraphicOverlay<*>,
    private val context: Context
) : BaseCameraAnalyzer<List<Face>>(){

    private var hasShownToast = false

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

            val isSmile = (results[0].smilingProbability ?: 0f) > 0.67f
            val face = results[0]
            if (isSmile || isMouthOpened(face)) {
                Toast.makeText(context, "Human detected", Toast.LENGTH_SHORT).show()
            }

        }

        graphicOverlay.postInvalidate()
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "onFailure : $e")
    }

    private fun isMouthOpened(face: Face): Boolean {
        val left = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position ?: return false
        val right = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position ?: return false
        val bottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position ?: return false

        // Square of lengths be a2, b2, c2
        val a2 = lengthSquare(right, bottom)
        val b2 = lengthSquare(left, bottom)
        val c2 = lengthSquare(left, right)

        // length of sides be a, b, c
        val a = sqrt(a2)
        val b = sqrt(b2)

        // From Cosine law
        val gamma = acos((a2 + b2 - c2) / (2 * a * b))

        // Converting to degrees
        val gammaDeg = gamma * 180 / Math.PI
        return gammaDeg < 115f
    }

    private fun lengthSquare(a: PointF, b: PointF): Float {
        val x = a.x - b.x
        val y = a.y - b.y
        return x * x + y * y
    }

    companion object {
        private const val TAG = "CameraAnalyzer"
    }
}