package com.example.objectdetectionplate

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.objectdetectionplate.databinding.ActivityObjectDetectionPlateBinding
import com.example.textrecognize.utils.setProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.IOException
import kotlin.math.abs

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

        //openCV
        OpenCVLoader.initDebug()

        //init text recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


        checkForPermission()
        onClick()
    }

    private fun processImageWithOpenCV(imageUri: Uri) {
        try {
            val inputBitmap: Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

            val inputImage: Mat = Mat()
            Utils.bitmapToMat(inputBitmap, inputImage)

            if (!inputImage.empty()) {

                val grayscaleImage = Mat()
                Imgproc.cvtColor(inputImage, grayscaleImage, Imgproc.COLOR_BGR2GRAY)

                val yellowChannel = Mat()
                Core.extractChannel(inputImage, yellowChannel, 1) // Extract channel hijau

                // Ambang batas (threshold) untuk menghasilkan gambar hitam putih (biner)
                val thresholdValue = 180.0
                Imgproc.threshold(yellowChannel, yellowChannel, thresholdValue, 255.0, Imgproc.THRESH_BINARY)

                // membuat latar belakang putih dan objek hitam
                Core.bitwise_not(yellowChannel, yellowChannel)

                // menghilangkan noise kecil
                val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
                Imgproc.morphologyEx(yellowChannel, yellowChannel, Imgproc.MORPH_OPEN, kernel)

                val outputBitmap = Bitmap.createBitmap(yellowChannel.cols(), yellowChannel.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(yellowChannel, outputBitmap)

                binding.imageView.setImageBitmap(outputBitmap)

                // Clean OpenCV
                inputImage.release()
                grayscaleImage.release()
                yellowChannel.release()
            } else {
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
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
                            binding.tvRecognizedText.text = formattedText
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
                return abs(elementAspectRatio - licensePlateAspectRatio) < aspectRatioThreshold
            }
        }
        return false
    }

    private fun extractFormattedText(fullText: String): String {
        // Pola regex untuk format plat nomor seperti "B 3345 BKJ" dan "B 3345 0ZZ"
        val pattern = Regex("[A-Z]{1,2} [0-9]{1,4} [A-Z]{1,3}|[A-Z]{1,2} [0-9]{1,4} [0-9]{1,3}")

        // Pola regex untuk format plat nomor seperti "B3345BKJ" atau "B33450ZZ" atau "B PKD"
        val pattern1 = Regex("[A-Z]{1,2} \\d{4} [A-Z\\d]{1,3}|[A-Z]{1,2} \\d{4} (?:[A-Z]{1,3}|\\d{1,3})")

        // Pola regex untuk format plat nomor seperti "12 3445 89" atau "71-03" atau "71-03"
        val pattern2 = Regex("\\d{2,4} \\d{4,8} \\d{2,4}|\\d{2,4}[-]\\d{2,4}|\\d{2,4}[ ]\\d{2,4}")

        // Pola regex untuk format plat nomor seperti "3555-07" atau "355-111" atau "70834-IX" atau "1-00"
        val pattern3 = Regex("\\d{3,5}[-]\\d{3,4}|\\d{3,5}[-]\\d{2,3}|\\d{1}[-]\\d{2}|\\d{1}[-][A-Z]{1,3}")

        // Pola regex untuk format plat nomor seperti "H 1 P"
        val pattern4 = Regex("[A-Z]{1,2} \\d [A-Z]{1,4}")

        //Pola regex untuk format plat nomor seperti "RI 1" atau "RI 123"
        val pattern5 = Regex("RI \\d{1,3}")

        // Cari pola regex dalam teks yang diberikan
        val matchResult = pattern.find(fullText)
        val matchResult1 = pattern1.find(fullText)
        val matchResult2 = pattern2.find(fullText)
        val matchResult3 = pattern3.find(fullText)
        val matchResult4 = pattern4.find(fullText)
        val matchResult5 = pattern5.find(fullText)


        return when {
            matchResult != null -> matchResult.value
            matchResult1 != null -> matchResult1.value
            matchResult2 != null -> matchResult2.value
            matchResult3 != null -> matchResult3.value
            matchResult4 != null -> matchResult4.value
            matchResult5 != null -> matchResult5.value
            else -> "Format tidak ditemukan atau tidak didukung"
        }
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
            //proses gambar dengan openCV
            processImageWithOpenCV(imageUri!!)

//            binding.imageView.setImageURI(imageUri)
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
//            binding.imageView.setImageURI(imageUri)

            // Proses gambar dengan OpenCV
            if (imageUri != null) {
                processImageWithOpenCV(imageUri!!)
            }
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