package com.example.testfacerecognition

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.testfacerecognition.data.FaceCountour
import com.example.testfacerecognition.databinding.ActivityAddFaceBinding
import com.example.testfacerecognition.viewmodel.FaceCountourViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class AddFaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFaceBinding
    private lateinit var faceCountourViewModel: FaceCountourViewModel
    private var faceContourHuman: String = ""


    private var imageUri: Uri? = null
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private lateinit var detector: FaceDetector


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkForPermission()
        if (imageUri == null) {
            showInputImageDialog()
        }

        faceCountourViewModel = ViewModelProvider(this)[FaceCountourViewModel::class.java]

        mlKit()
        onClick()
    }

    private fun onClick() {
        binding.fabAddPhoto.setOnClickListener {
            showInputImageDialog()
        }
        binding.fabSavePhoto.setOnClickListener {
            if (imageUri != null){
                detectFace()
                saveCountour()
            } else{
                Toast.makeText(this, "Silahkan pilih gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mlKit() {
        val cameraOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(cameraOptions)
    }

    private fun detectFace() {
        val smallerBitmap = Bitmap.createScaledBitmap(
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri),
            binding.imageView.width,
            binding.imageView.height,
            false
        )

        val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(smallerBitmap, 0)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.size == 0) {
                    Toast.makeText(this, "No face detected", Toast.LENGTH_SHORT).show()
                } else {
                    val bitmapWithBoundingBox = drawBoundingBoxesOnBitmap(originalBitmap, faces, "Belum Terdaftar")
                    binding.imageView.setImageBitmap(bitmapWithBoundingBox)

                    Toast.makeText(this, "Face detected", Toast.LENGTH_SHORT).show()
                    val faceContour = faces[0].getContour(FaceContour.FACE)
                    for (face in faces) {
                        val faceContour = face.getContour(FaceContour.FACE)
                        Log.d("face landmark", faceContour.toString())
                        faceContourHuman = faceContour.toString()
                    }

                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun saveCountour() {
        val dialog = Dialog(this@AddFaceActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_face_countour)

        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        val image = dialog.findViewById(R.id.face) as ImageView
        val noBtn = dialog.findViewById(R.id.btn_no) as Button
        val yesBtn = dialog.findViewById(R.id.btn_yes) as Button
        val etName = dialog.findViewById(R.id.et_name) as TextView
        val etNFCID = dialog.findViewById(R.id.et_nfc) as TextView

        image.setImageURI(imageUri)

        noBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this@AddFaceActivity, "Membatalkan", Toast.LENGTH_SHORT).show()
        }

        yesBtn.setOnClickListener {
            val name = etName.text.toString()
            val nfcId = etNFCID.text.toString()

            val faceCountour = FaceCountour(
                0,
                name,
                imageUri.toString(),
                nfcId,
                faceContourHuman
            )
            faceCountourViewModel.addFace(faceCountour)
            Toast.makeText(this@AddFaceActivity, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun drawBoundingBoxesOnBitmap(
        originalBitmap: Bitmap,
        faces: List<Face>,
        nama: String
    ): Bitmap {
        val bitmapWithBoundingBox = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmapWithBoundingBox)
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5.0f
        paint.textSize = 50.0f
        paint.isFakeBoldText = false

        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        val imageViewWidth = binding.imageView.width
        val imageViewHeight = binding.imageView.height

        for (face in faces) {
            // Koordinat bounding box pada gambar yang diubah ukurannya
            val boundingBox = face.boundingBox

            // Menghitung faktor skala untuk mengubah koordinat bounding box ke gambar asli
            val scaleX = originalWidth.toFloat() / imageViewWidth.toFloat()
            val scaleY = originalHeight.toFloat() / imageViewHeight.toFloat()

            // Mengubah koordinat bounding box ke gambar asli
            val scaledBoundingBox = Rect(
                (boundingBox.left * scaleX).toInt(),
                (boundingBox.top * scaleY).toInt(),
                (boundingBox.right * scaleX).toInt(),
                (boundingBox.bottom * scaleY).toInt()
            )

            // Menggambar bounding box di atas gambar asli
            paint.style = Paint.Style.STROKE
            paint.color = Color.RED
            canvas.drawRect(scaledBoundingBox, paint)

            // Menambahkan nama di atas bounding box
            val name = nama  // Ganti dengan nama yang sesuai
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.isAntiAlias = true
            val textX = scaledBoundingBox.centerX().toFloat()
            val textY =
                (scaledBoundingBox.top - 20).toFloat()  // Sesuaikan posisi teks di atas bounding box
            canvas.drawText(name, textX, textY, paint)
        }

        return bitmapWithBoundingBox
    }

    private fun checkForPermission() {
        cameraPermission = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun showInputImageDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Gambar")
            .setItems(
                arrayOf("Kamera", "Galeri")
            ) { _, which ->
                if (which == 0) {
                    if (checkCameraPermission()) {
                        pickCamera()
                    } else {
                        requestCameraPermission()
                    }
                } else {
                    if (checkStoragePermission()) {
                        pickGallery()
                    } else {
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

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                binding.imageView.setImageURI(imageUri)
                detectFace()
            } else {
                Toast.makeText(this, "Membatalkan", Toast.LENGTH_SHORT).show()
            }
        }

    private fun pickGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                imageUri = data?.data
                binding.imageView.setImageURI(imageUri)
            } else {
                Toast.makeText(this, "Membatalkan", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkStoragePermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission(): Boolean {
        val result =
            checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val result1 =
            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
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
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && storageAccepted) {
                        pickCamera()
                    } else {
                        Toast.makeText(
                            this,
                            "Camera & Storage permission are required",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (storageAccepted) {
                        pickGallery()
                    } else {
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        }
    }

    private companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }
}