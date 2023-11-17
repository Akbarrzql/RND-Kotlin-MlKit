package com.example.objectdetectionplate

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.objectdetectionplate.databinding.ActivityObjectDetectionPlateBinding
import com.example.textrecognize.utils.setProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ObjectDetectionPlateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObjectDetectionPlateBinding

    //text recognizer
    private var imageUri: Uri? = null
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObjectDetectionPlateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init text recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


        checkForPermission()
        onClick()
    }

    private fun checkForPermission(){
        cameraPermission = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun onClick(){
        binding.apply {
            btnCamera.setOnClickListener {
                showInputImageDialog()
            }
            btnRecognizeText.setOnClickListener {
                if (imageUri != null){
                    recognizeText()
                } else{
                    Toast.makeText(this@ObjectDetectionPlateActivity, "Silahkan pilih gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun recognizeText() {
        val dialog = setProgressDialog(this, "Mohon tunggu", "Sedang mengenali teks")
        dialog.show()

        val image = com.google.mlkit.vision.common.InputImage.fromFilePath(this, imageUri!!)
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                dialog.dismiss()

                // Loop melalui elemen teks yang terdeteksi
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            val resultText = element.text
                            val resultText1 = visionText.text
                            val formattedText = extractFormattedText(resultText1)
                            val formattedText1 = extractFormattedTextBottom(resultText)
                            val mergedText = "$formattedText\n$formattedText1"
                            binding.tvRecognizedText.text = mergedText
                        }
                    }
                }

            }
            .addOnFailureListener { e ->
                dialog.dismiss()
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
    }


    // Fungsi untuk memeriksa apakah elemen memiliki rasio aspek yang mendekati plat nomor
    private fun isPotentialLicensePlate(element: Text.Element, aspectRatioThreshold: Double): Boolean {
        val elementBoundingBox = element.boundingBox

        // Periksa apakah bounding box elemen tidak null
        if (elementBoundingBox != null) {
            val elementWidth = elementBoundingBox.width().toFloat()
            val elementHeight = elementBoundingBox.height().toFloat()

            // Periksa apakah lebar atau tinggi elemen tidak nol
            if (elementWidth != 0f && elementHeight != 0f) {
                // Hitung rasio aspek elemen
                val elementAspectRatio = elementWidth / elementHeight

                // Sesuaikan dengan rasio aspek plat nomor yang umum
                val licensePlateAspectRatio = 4.0

                // Periksa apakah rasio aspek elemen mendekati rasio aspek plat nomor
                return Math.abs(elementAspectRatio - licensePlateAspectRatio) < aspectRatioThreshold
            }
        }
        return false
    }
    private fun extractFormattedText(fullText: String): String {
        // Pola regex yang umum untuk berbagai macam format plat nomor
        val pattern = Regex("[A-Z]{1,2} ?\\d{1,4} [A-Z]{1,3}[A-Z\\d ]*")
        val matchResult = pattern.find(fullText)            // Cari pola regex dalam teks yang diberikan

        return matchResult?.value ?: "Format tidak ditemukan"
    }

    private fun extractFormattedTextBottom(fullText: String): String {
        // Pola regex yang umum untuk mendeteksi 4 angka kadaluarsa plat nomor
        val pattern = Regex("\\d{4}")
        val matchResult = pattern.find(fullText)            // Cari pola regex dalam teks yang diberikan

        return matchResult?.value ?: "Format tidak ditemukan"
    }



    private fun showInputImageDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Gambar")
            .setItems(
                arrayOf("Kamera", "Galeri")
            ) { _, which ->
                if (which == 0){
                    if (checkCameraPermission()){
                        pickCamera()
                    } else{
                        requestCameraPermission()
                    }
                } else{
                    if (checkStoragePermission()){
                        pickGallery()
                    } else{
                        requestStoragePermission()
                    }
                }
            }
            .show()

    }

    private fun pickCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(cameraIntent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            binding.imageView.setImageURI(imageUri)
        } else{
            Toast.makeText(this, "Membatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            imageUri = data?.data
            binding.imageView.setImageURI(imageUri)
        } else{
            Toast.makeText(this, "Membatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission(): Boolean {
        val result = checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val result1 = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return result && result1
    }

    private fun requestStoragePermission() {
        requestPermissions(storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun requestCameraPermission() {
        requestPermissions(cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()){
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && storageAccepted){
                        pickCamera()
                    } else{
                        Toast.makeText(this, "Camera & Storage permission are required", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()){
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (storageAccepted){
                        pickGallery()
                    } else{
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }

    private companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }
}