package com.example.testfacerecognition

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.testfacerecognition.databinding.ActivityMainBinding
import com.example.testfacerecognition.viewmodel.FaceCountourViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.pow

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var detector: FaceDetector
    private lateinit var faceCountourViewModel: FaceCountourViewModel

    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequest: CaptureRequest.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        faceCountourViewModel = ViewModelProvider(this)[FaceCountourViewModel::class.java]

        getPermissions()
        initialize()
        camera()
        mlKit()
        onClick()
    }

    override fun onResume() {
        super.onResume()
        getPermissions()
        initialize()
        camera()
        mlKit()
        onClick()
    }

    override fun onPause() {
        super.onPause()
        cameraCaptureSession.close()
        cameraDevice.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.quitSafely()
    }

    private fun onClick() {
        binding.fabAddPhoto.setOnClickListener {
            Intent(this, AddFaceActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    private fun mlKit() {
        val cameraOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(cameraOptions)

        Log.d("FaceDetection", "ML Kit initialized successfully")
    }

    private fun camera() {
        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: android.graphics.SurfaceTexture, p1: Int, p2: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: android.graphics.SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: android.graphics.SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: android.graphics.SurfaceTexture) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val cameraId = getFrontCameraId()

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surfaceTexture = binding.textureView.surfaceTexture
                surfaceTexture?.setDefaultBufferSize(binding.textureView.width, binding.textureView.height)

                val rotation = windowManager.defaultDisplay.rotation
                val sensorOrientation = getCameraSensorOrientation(cameraId)

                val surface = Surface(surfaceTexture)
                val imageReader = ImageReader.newInstance(
                    binding.textureView.width,
                    binding.textureView.height,
                    ImageFormat.JPEG,  // Change the format to JPEG
                    2
                )

                val surfaces = listOf(surface, imageReader.surface)

                captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
//                captureRequest.set(CaptureRequest.SENSOR_SENSITIVITY, 100)
                captureRequest.addTarget(surface)
                captureRequest.addTarget(imageReader.surface)
                captureRequest.set(CaptureRequest.JPEG_ORIENTATION, calculateJpegOrientation(sensorOrientation, rotation))

                cameraDevice.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        cameraCaptureSession.setRepeatingRequest(captureRequest.build(), null, handler)

                        // Set up ImageReader listener
                        imageReader.setOnImageAvailableListener({ reader ->
                            val image = reader.acquireLatestImage()
                            processCameraFrame(image)
                            image.close()
                        }, handler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                    }
                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {
            }

            override fun onError(camera: CameraDevice, error: Int) {
            }
        }, handler)
    }

    private fun getCameraSensorOrientation(cameraId: String): Int {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    }

    private fun calculateJpegOrientation(sensorOrientation: Int, rotation: Int): Int {
        Log.d("FaceDetection", "Sensor orientation: $sensorOrientation, rotation: $rotation")
        val roundedOrientation = when ((sensorOrientation + rotation * 90 + 360) % 360) {
            in 0..44, in 315..359 -> 0
            in 45..134 -> 90
            in 135..224 -> 180
            in 225..314 -> 270
            else -> 0  // Default to 0 if the value is outside the valid range
        }

        return roundedOrientation
    }

    private fun getFrontCameraId(): String {
        val cameraIds = cameraManager.cameraIdList
        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId
            }
        }
        return cameraIds[0]
    }

    private fun initialize() {
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("Camera2")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    private fun processCameraFrame(image: Image?) {
        Log.d("FaceDetection", "Processing camera frame")

        val inputImage = InputImage.fromMediaImage(image!!, calculateJpegOrientation(0, 0))
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                Log.d("FaceDetection", "Detected ${faces.size} faces")

                faceCountourViewModel.readAllData.observe(this) { faceCountour ->
                    for (face in faces) {
                        val convertedCameraLandmarks = convertLandmarksToList(face.allLandmarks)
                        Log.d("Face Matching", "Converted camera landmarks: $convertedCameraLandmarks")

                        //get data form database
                        Log.d("Face Matching", "data wajah dari database: $faceCountour")

                        for (storedLandmark in faceCountour) {
                            Log.d("Face Matching", "data landmark dari database: $storedLandmark")

                            val storedLandmarkValues = extractLandmarkValues(storedLandmark.faceContour)
                            Log.d("Face Matching", "Stored landmark values: $storedLandmarkValues")

                            val matchingScore = calculateMatchingScore(convertedCameraLandmarks, storedLandmarkValues)
                            Log.d("Face Matching", "Matching score: $matchingScore")

                            // Check if the matching score is above the threshold
                            val threshold = 0.47
                            if (matchingScore >= threshold) {
                                Log.d("Face Matching", "Face matching found! ${storedLandmark.name}")
                                break
                            } else {
                                Log.d("Face Matching", "Face matching not found!")
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceDetection", "Face detection failed $e")
            }
            .addOnCompleteListener {
                image.close()
            }
    }


    private fun convertLandmarksToList(landmarks: List<FaceLandmark>): List<Double> {
        return landmarks.flatMap { listOf(it.position.x.toDouble(), it.position.y.toDouble()) }
    }

    private fun extractLandmarkValues(landmarkString: String): List<Double> {
        return landmarkString
            .substringAfter("[")
            .substringBefore("]")
            .split(", ")
            .map { it.trim().substringAfter("(").substringBefore(")").toDouble() }
    }



    private fun calculateMatchingScore(cameraLandmarks: List<Double>, storedLandmarks: List<Double>): Double {
        // Perhitungan matching score dengan Cosine Similarity
        // https://en.wikipedia.org/wiki/Cosine_similarity
        val dotProduct = cameraLandmarks.zip(storedLandmarks).map { it.first * it.second }.sum()
        val cameraLandmarksMagnitude = cameraLandmarks.map { it.pow(2) }.sum().pow(0.5)
        val storedLandmarksMagnitude = storedLandmarks.map { it.pow(2) }.sum().pow(0.5)

        return dotProduct / (cameraLandmarksMagnitude * storedLandmarksMagnitude)
    }

    private fun isCoordinateWithinTolerance(cameraCoordinate: Double, storedCoordinate: Double): Boolean {
        // Adjust with your desired tolerance
        val tolerance = 0.1
        return abs(cameraCoordinate - storedCoordinate) <= tolerance
    }


    private fun getPermissions() {
        val permissionsList = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(android.Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionsList.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsList.toTypedArray(), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                getPermissions()
            }
        }
    }
}