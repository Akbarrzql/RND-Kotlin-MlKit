package com.example.livenessfacedetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import com.example.livenessfacedetection.core.DetectionTask
import com.example.livenessfacedetection.core.FaceAnalyzer
import com.example.livenessfacedetection.core.LivenessDetector
import com.example.livenessfacedetection.core.tasks.FacingDetectionTask
import com.example.livenessfacedetection.core.tasks.MouthOpenDetectionTask
import com.example.livenessfacedetection.core.tasks.ShakeDetectionTask
import com.example.livenessfacedetection.core.tasks.SmileDetectionTask
import com.example.livenessfacedetection.databinding.ActivityDetectionBinding
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import java.io.File

class DetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetectionBinding
    private lateinit var cameraController: LifecycleCameraController
    private var listCountour = arrayListOf<String>()

    private val handler = Handler(Looper.getMainLooper())
    private val TIMEOUT_DELAY = 5000L
    private val TIMEOUT_CHECK_INTERVAL = 5000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Tidak dapat mengakses kamera", Toast.LENGTH_LONG).show()
                finish()
            }
        }.launch(Manifest.permission.CAMERA)

        binding.apply {
            toolbar.setNavigationOnClickListener { finish() }
            cameraPreview.clipToOutline = true
            cameraPreview.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, view.height / 2.0f)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Remove all callbacks to stop the toast messages when the activity is paused
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove all callbacks to stop the toast messages when the activity is destroyed
        handler.removeCallbacksAndMessages(null)
    }

    @SuppressLint("SetTextI18n")
    private fun startCamera() {
        cameraController = LifecycleCameraController(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            FaceAnalyzer(buildLivenessDetector())
        )
        cameraController.bindToLifecycle(this)
        binding.cameraPreview.controller = cameraController
    }

    private fun buildLivenessDetector(): LivenessDetector {
        val listener = object : LivenessDetector.Listener {
            private var taskStartTime = 0L

            @SuppressLint("SetTextI18n")
            override fun onTaskStarted(task: DetectionTask) {
                when (task) {
                    is FacingDetectionTask ->
                        binding.guide.text = "Harap menghadap ke kamera"
                    is ShakeDetectionTask ->
                        binding.guide.text = "Gerakkan kepala Anda ke kiri dan ke kanan"
                    is MouthOpenDetectionTask ->
                        binding.guide.text = "Buka mulut Anda"
                    is SmileDetectionTask ->
                        binding.guide.text = "Senyum"
                }
            }

            override fun onTaskCompleted(task: DetectionTask, isLastTask: Boolean, face: Face?) {
                val faceContours = face?.getContour(FaceContour.FACE)?.points
                listCountour.add(faceContours.toString())
                Log.d("Countour", "onTaskCompleted: $listCountour")
                if (isLastTask){
                    finishForResult()
                }
//                val file = File.createTempFile("liveness", ".jpg", cacheDir)
//                takePhoto(file) {
//                    imageFiles.add(it.absolutePath)
//                    if (isLastTask) {
//                        finishForResult()
//                    }
//                }

                handler.removeCallbacksAndMessages(null)
            }

            override fun onTaskFailed(task: DetectionTask, code: Int) {
                if (code == LivenessDetector.ERROR_MULTI_FACES) {
                    Toast.makeText(
                        this@DetectionActivity,
                        "Tolong pastikan hanya ada satu wajah di layar",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            fun checkTimeout() {
                val currentTime = System.currentTimeMillis()
                if (currentTime - taskStartTime > TIMEOUT_DELAY) {
                    Toast.makeText(
                        this@DetectionActivity,
                        "Anda bukan manusia",
                        Toast.LENGTH_LONG
                    ).show()
                    finishForResult()
                }
            }

        }

        return LivenessDetector(
            FacingDetectionTask(),
            ShakeDetectionTask(),
            MouthOpenDetectionTask(),
            SmileDetectionTask()
        ).also {
            it.setListener(listener)

            // Schedule a delayed task to show toast after TIMEOUT_DELAY milliseconds
            handler.postDelayed(object : Runnable {
                override fun run() {
                    listener.checkTimeout()
                    handler.postDelayed(this, TIMEOUT_CHECK_INTERVAL)
                }
            }, TIMEOUT_CHECK_INTERVAL)
        }
    }

    private fun finishForResult() {
        val result = ArrayList(listCountour)
        setResult(RESULT_OK, Intent().putStringArrayListExtra(ResultContract.RESULT_KEY, result))
        finish()
    }


    private fun takePhoto(file: File, onSaved: (File) -> Unit) {
        cameraController.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(e: ImageCaptureException) {
                    e.printStackTrace()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved(file)
                }
            }
        )
    }

    class ResultContract : ActivityResultContract<Any?, List<String>?>() {

        companion object {
            const val RESULT_KEY = "result"
        }

        override fun createIntent(context: Context, input: Any?): Intent {
            return Intent(context, DetectionActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): List<String>? {
            if (resultCode == RESULT_OK && intent != null) {
                return intent.getStringArrayListExtra(RESULT_KEY)
            }
            return null
        }
    }
}