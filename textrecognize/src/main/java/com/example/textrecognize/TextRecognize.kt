package com.example.textrecognize

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.textrecognize.databinding.RecognizeTextBinding
import com.example.textrecognize.utils.setProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognize: AppCompatActivity() {

    private lateinit var binding: RecognizeTextBinding

    //text recognizer
    private var imageUri: Uri? = null
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RecognizeTextBinding.inflate(layoutInflater)
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
                    Toast.makeText(this@TextRecognize, "Silahkan pilih gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun recognizeText() {
        val dialog = setProgressDialog(this, "Mohon tunggu", "Sedang mengenali teks")
        dialog.show()

        val image = com.google.mlkit.vision.common.InputImage.fromFilePath(this, imageUri!!)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                dialog.dismiss()
                val resultText = visionText.text
                binding.tvRecognizedText.text = resultText
            }
            .addOnFailureListener { e ->
                dialog.dismiss()
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
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