package com.example.facedetection

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.facedetection.camera.CameraManager
import com.example.facedetection.data.model.FaceDetection
import com.example.facedetection.databinding.ActivityFaceRecognitionBinding
import com.example.facedetection.viewmodel.FaceDetectionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FaceRecognitionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceRecognitionBinding
    private lateinit var cameraManager: CameraManager
    private var imageUri: Uri? = null
    private lateinit var cameraPermission: Array<String>

    private lateinit var viewModel: FaceDetectionViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceRecognitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[FaceDetectionViewModel::class.java]

        cameraManager = CameraManager(
            this,
            binding.viewCameraPreview,
            binding.viewGraphicOverlay,
            this
        )
        askCameraPermission()
        buttonClicks()
    }

    private fun buttonClicks() {
        binding.buttonTurnCamera.setOnClickListener {
            cameraManager.changeCamera()
        }
        binding.buttonStopCamera.setOnClickListener {
            cameraManager.cameraStop()
            buttonVisibility(false)
        }
        binding.buttonStartCamera.setOnClickListener {
            cameraManager.cameraStart()
            buttonVisibility(true)
        }
        binding.addFace.setOnClickListener {
            pickCamera()
            buttonVisibility(true)
        }
    }

    private fun buttonVisibility(forStart : Boolean) {
        if (forStart) {
            binding.buttonStopCamera.visibility = View.VISIBLE
            binding.buttonStartCamera.visibility = View.INVISIBLE
        } else {
            binding.buttonStopCamera.visibility = View.INVISIBLE
            binding.buttonStartCamera.visibility = View.VISIBLE
        }
    }


    private fun askCameraPermission() {
        if (arrayOf(android.Manifest.permission.CAMERA).all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            cameraManager.cameraStart()
        } else {
            ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE)
        }
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
//            processImageWithOpenCV(imageUri!!)

            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.custom_show_dialog_face)

            dialog.window?.setBackgroundDrawableResource(R.color.transparent)

            val noBtn = dialog.findViewById(R.id.btn_no) as Button
            val yesBtn = dialog.findViewById(R.id.btn_yes) as Button
            val ivFace = dialog.findViewById(R.id.face) as ImageView
            val etName = dialog.findViewById(R.id.et_name) as TextView

            if (imageUri != null) {
                // Use BitmapFactory to decode the image and set it to the ImageView
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                ivFace.setImageBitmap(bitmap)
            }

            binding.apply {
                yesBtn.setOnClickListener {
                    val name = etName.text.toString()
                    val face = imageUri.toString()
                    val faceDetection = FaceDetection(0, name, face)
                    viewModel.addFace(faceDetection)
                    Toast.makeText(this@FaceRecognitionActivity, "Berhasil menambahkan wajah", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                noBtn.setOnClickListener { dialog.dismiss() }
            }

            dialog.show()
        } else{
            Toast.makeText(this, "Membatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.cameraStart()
        } else {
            Toast.makeText(this, "Camera Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }

    private companion object{
        private const val CAMERA_REQUEST_CODE = 100
    }
}