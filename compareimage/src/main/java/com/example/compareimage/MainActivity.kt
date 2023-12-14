package com.example.compareimage

import android.app.Dialog
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
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.compareimage.data.viewmodel.FaceDetectionViewModel
import com.example.compareimage.databinding.ActivityMainBinding
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.cli.Options
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfRect
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.util.concurrent.ScheduledExecutorService
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageUri: Uri? = null
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private lateinit var faceDetectionViewModel: FaceDetectionViewModel
    private lateinit var detector: FaceDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OpenCVLoader.initDebug()

        faceDetectionViewModel = ViewModelProvider(this)[FaceDetectionViewModel::class.java]

        checkForPermission()
        mlKitInit()
        detectImage()
        onClick()
    }

    private fun detectImage() {
        if (imageUri != null) {
            detectFace()
        } else {
            Toast.makeText(this, "Please select image", Toast.LENGTH_SHORT).show()
        }
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

                    val isSmile = (faces[0].smilingProbability ?: 0f) > 0.67f
                    //get smile probability
                    Log.d("TAG", "detectFace: ${faces[0].smilingProbability}")

                    if(isSmile){
                        Toast.makeText(this, "Smile detected", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this, "Smile not detected", Toast.LENGTH_SHORT).show()
                    }

//                    if (matchingFace?.faceContour == detectedCountour) {
//                        val bitmapWithBoundingBox = drawBoundingBoxesOnBitmap(originalBitmap, faces, matchingFace.namaKaryawan)
//                        binding.imageView.setImageBitmap(bitmapWithBoundingBox)
//                        Toast.makeText(this, "Wajah sudah terdaftar", Toast.LENGTH_SHORT).show()
//                    } else {
//                        val bitmapWithBoundingBox = drawBoundingBoxesOnBitmap(originalBitmap, faces, "Belum Terdaftar")
//                        binding.imageView.setImageBitmap(bitmapWithBoundingBox)
//                        Toast.makeText(this, "Wajah belum terdaftar", Toast.LENGTH_SHORT).show()
//                    }

//                    faceDetectionViewModel.readAllData.observe(this) { faceDetection ->
//                        for (face in faceDetection) {
//                            val savedContour = face.faceContour
//                            if (savedContour == detectedCountour) {
//                                Toast.makeText(this, "Wajah sudah terdaftar", Toast.LENGTH_SHORT).show()
//                            } else {
//                                Toast.makeText(this, "Wajah belum terdaftar", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    }

                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun drawBoundingBoxesOnBitmap(originalBitmap: Bitmap, faces: List<Face>, nama: String): Bitmap {
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
            val textY = (scaledBoundingBox.top - 20).toFloat()  // Sesuaikan posisi teks di atas bounding box
            canvas.drawText(name, textX, textY, paint)
        }

        return bitmapWithBoundingBox
    }


    private fun mlKitInit() {
        val cameraOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(cameraOptions)
    }

    private fun onClick() {
        binding.apply {
            btnGallery.setOnClickListener {
                if (checkStoragePermission()) {
                    pickGallery()
                } else {
                    requestStoragePermission()
                }
            }

            btnDetect.setOnClickListener {
                detectImage()
            }

            btnSave.setOnClickListener {
                saveCountour()
            }
        }
    }

    private fun saveCountour() {
        val dialog = Dialog(this@MainActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_show_dialog_face)

        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        val noBtn = dialog.findViewById(R.id.btn_no) as Button
        val yesBtn = dialog.findViewById(R.id.btn_yes) as Button
        val etName = dialog.findViewById(R.id.et_name) as TextView
        val etNFCID = dialog.findViewById(R.id.et_nfcId) as TextView
        val etIdKaryawan = dialog.findViewById(R.id.et_idKaryawan) as TextView

        noBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this@MainActivity, "Membatalkan", Toast.LENGTH_SHORT).show()
        }

        yesBtn.setOnClickListener {
            val name = etName.text.toString()
            val nfcId = etNFCID.text.toString()
            val idKaryawan = etIdKaryawan.text.toString()

            val faceDetection = com.example.compareimage.data.model.FaceDetection(
                0,
                idKaryawan,
                nfcId,
                name,
                "detectedCountour"
            )
            faceDetectionViewModel.addFace(faceDetection)
            Toast.makeText(this@MainActivity, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getFaceContour(bitmap: Bitmap): String {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        // Anda dapat menyesuaikan parameter sesuai dengan kebutuhan
        val faceCascade = CascadeClassifier(
            File(
                Environment.getExternalStorageDirectory(),
                "lbpcascade_frontalface"
            ).absolutePath
        )

        if (faceCascade.empty()) {
            Log.e("getFaceContour", "Failed to load cascade classifier")
            return ""
        }

        val faces = MatOfRect()
        faceCascade.detectMultiScale(grayMat, faces)

        // Ambil kontur dari hasil deteksi wajah
        val contours = mutableListOf<MatOfPoint>()
        for (rect in faces.toArray()) {
            val roi = grayMat.submat(rect)
            val edges = Mat()
            Imgproc.Canny(roi, edges, 80.0, 100.0)

            val hierarchy = Mat()
            val contourList = mutableListOf<MatOfPoint>()
            Imgproc.findContours(edges, contourList, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            // Ambil kontur terbesar (wajah)
            val maxContour = contourList.maxByOrNull { it.toArray().size }

            maxContour?.let { contours.add(it) }
        }

        // Konversi kontur menjadi format string
        val contourString = contours.joinToString { matOfPoint ->
            matOfPoint.toArray().joinToString { point ->
                "${point.x},${point.y}"
            }
        }

        return contourString
    }

    fun getMatchingFace(detectedContour: String): com.example.compareimage.data.model.FaceDetection? {
        // Mendapatkan wajah dari database yang cocok dengan kontur yang baru
        val matchingFace = runBlocking {
            withContext(Dispatchers.IO) {
                faceDetectionViewModel.getMatchingFace(detectedContour)
            }
        }

        Log.d("TAG", "getMatchingFace: $matchingFace")

        return matchingFace
    }


    private fun checkForPermission() {
        cameraPermission = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
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

    private fun requestStoragePermission() {
        requestPermissions(storagePermission, STORAGE_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
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
        private const val STORAGE_REQUEST_CODE = 101
    }
}