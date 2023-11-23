package com.example.facedetection.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.facedetection.data.FaceInfo
import com.example.facedetection.utils.CameraUtils
import com.example.facedetection.graphic.GraphicOverlay
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context : Context,
    private val previewView : PreviewView,
    private val graphicOverlay: GraphicOverlay<*>,
    private val lifecycleOwner : LifecycleOwner
) {

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview : Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var camera : Camera
    private var cameraExecutor : ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var latestCapturedImage: Image? = null
    private val PREF_NAME = "DetectedFaces"

    // Start Camera
    fun cameraStart() {
        val cameraProcessProvider = ProcessCameraProvider.getInstance(context)

        cameraProcessProvider.addListener(
            {
                cameraProvider = cameraProcessProvider.get()
                preview = Preview.Builder().build()
                // ImageAnalysis
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, CameraAnalyzer(graphicOverlay, context))
                    }
                // Camera Selector for options.
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraOption)
                    .build()

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture, preview, imageAnalysis)

                setCameraConfig(cameraProvider, cameraSelector)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    // Set Camera Config
    private fun setCameraConfig(cameraProvider: ProcessCameraProvider, cameraSelector: CameraSelector) {
        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e : Exception) {
            Log.e(TAG, "setCameraConfig : $e")
        }
    }

    // Return Camera
    fun changeCamera() {
        cameraStop()
        cameraOption = if (cameraOption == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
        else CameraSelector.LENS_FACING_BACK
        CameraUtils.toggleSelector()
        cameraStart()
    }

    // Stop Camera
    fun cameraStop () {
        cameraProvider.unbindAll()
    }

    // Add Face
    fun addFace(){
        Toast.makeText(context, "Add Face", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG : String = "CameraManager"
        var cameraOption : Int = CameraSelector.LENS_FACING_FRONT
    }
}