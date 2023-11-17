package com.example.objectdetectionplate

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.objectdetectionplate.databinding.ActivityObjectDetectionPlateBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ObjectDetectionPlateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObjectDetectionPlateBinding
    private lateinit var cameraExcutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObjectDetectionPlateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExcutor = Executors.newSingleThreadExecutor()
        requestForPermission()
    }


    private fun requestForPermission() {
        requestCameraPermission { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestCameraPermission(onResult: (Boolean) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
        } else {    // Permission has already been granted
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                onResult(isGranted)
            }.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }
}