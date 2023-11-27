package com.example.facelandmarkrecognition.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Camera
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import com.example.facelandmarkrecognition.FaceLandmarkerHelper
import com.example.facelandmarkrecognition.viewmodel.MainViewModel
import com.example.facelandmarkrecognition.adapter.FaceBlendshapesResultAdapter
import com.example.facelandmarkrecognition.databinding.FragmentCameraBinding
import com.example.facelandmarkrecognition.viewmodel.FaceDetectionViewModel
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class CameraFragment : Fragment(), FaceLandmarkerHelper.LandmarkerListener {

    companion object {
        private const val TAG = "Face Landmarker"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private val faceBlendshapesResultAdapter by lazy {
        FaceBlendshapesResultAdapter()
    }

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var faceDetectionViewModel: FaceDetectionViewModel
    private lateinit var landmark: String

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.

        // Start the FaceLandmarkerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute {
            if (faceLandmarkerHelper.isClose()) {
                faceLandmarkerHelper.setupFaceLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if(this::faceLandmarkerHelper.isInitialized) {
            viewModel.setMaxFaces(faceLandmarkerHelper.maxNumFaces)
            viewModel.setMinFaceDetectionConfidence(faceLandmarkerHelper.minFaceDetectionConfidence)
            viewModel.setMinFaceTrackingConfidence(faceLandmarkerHelper.minFaceTrackingConfidence)
            viewModel.setMinFacePresenceConfidence(faceLandmarkerHelper.minFacePresenceConfidence)
            viewModel.setDelegate(faceLandmarkerHelper.currentDelegate)

            // Close the FaceLandmarkerHelper and release resources
            backgroundExecutor.execute { faceLandmarkerHelper.clearFaceLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        faceDetectionViewModel = ViewModelProvider(this)[FaceDetectionViewModel::class.java]

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(fragmentCameraBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = faceBlendshapesResultAdapter
        }

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Create the FaceLandmarkerHelper that will handle the inference
        backgroundExecutor.execute {
            faceLandmarkerHelper = FaceLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                maxNumFaces = viewModel.currentMaxFaces,
                currentDelegate = viewModel.currentDelegate,
                faceLandmarkerHelperListener = this
            )
        }

    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectFace(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectFace(imageProxy: ImageProxy) {
        faceLandmarkerHelper.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after face have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    override fun onResults(
        resultBundle: FaceLandmarkerHelper.ResultBundle
    ) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                if (fragmentCameraBinding.recyclerviewResults.scrollState != SCROLL_STATE_DRAGGING) {
                    faceBlendshapesResultAdapter.updateResults(resultBundle.result)
                    faceBlendshapesResultAdapter.notifyDataSetChanged()
                }

                // Pass necessary information to OverlayView for drawing on the canvas
                fragmentCameraBinding.overlay.setResults(
                    resultBundle.result,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                landmark = resultBundle.result.faceLandmarks().toString()
                Log.d("landmrark Camera", landmark)

                //menampilkan isi di room database dan mencocolkan dengan landmark yang didapat dengan toleransi hingga 0.1


//                faceDetectionViewModel.readAllData.observe(viewLifecycleOwner){
//                    for (i in it.indices){
//                        Log.d("landmarkkkk", it[i].faceLandmarks)
//                        if (landmark == it[i].faceLandmarks){
//                            faceDetectionViewModel.updateFaceLandmarks(it[i].id, landmark)
//                            Toast.makeText(requireContext(), "Nama : ${it[i].name}", Toast.LENGTH_SHORT).show()
//                        }
//                    }
////                    val face = it.find { face -> face.faceLandmarks == landmark }
////                    if (face != null) {
////                        faceDetectionViewModel.updateFaceLandmarks(face.id, landmark)
////                        Toast.makeText(requireContext(), "Berhasil Update", Toast.LENGTH_SHORT).show()
////                    }else{
////                        Log.d("landmarkkkk", "tidak ada")
////                    }
//
//                }

                faceDetectionViewModel.readAllData.observe(viewLifecycleOwner) { faceDetectionList ->
                    for (face in faceDetectionList) {
                        Log.d("landmark in DB", face.faceLandmarks)

                        // Compare the detected landmark with landmarks in the database with tolerance
                        if (areLandmarksSimilar(landmark, face.faceLandmarks)) {
                            // Update the face landmarks in the database
                            fragmentCameraBinding.overlay.setDetectedFace(face)
                            Log.d("landmark face DB", "Nama : ${face.name} landmrk : ${face.faceLandmarks}")
//                            faceDetectionViewModel.updateFaceLandmarks(face.id, landmark)
//                            Toast.makeText(requireContext(), "Nama : ${face.name}", Toast.LENGTH_SHORT).show()
                        }else{
                            fragmentCameraBinding.overlay.setDetectedFace(null)
                        }
                    }
                }


                // Force a redraw
                fragmentCameraBinding.overlay.invalidate()
            }
        }
    }

    private fun areLandmarksSimilar(
        landmark1: String,
        landmark2: String,
        tolerance: Double = 0.06
    ): Boolean {
        // Parse the landmark strings to extract x, y, and z values
        val regex = Regex("x=([\\d.-]+) y=([\\d.-]+) z=([\\d.-]+)")
        val match1 = regex.find(landmark1)
        val match2 = regex.find(landmark2)

        if (match1 != null && match2 != null) {
            // Extract x, y, and z values
            val (x1, y1, z1) = match1.destructured
            val (x2, y2, z2) = match2.destructured

            // Convert string values to Double
            val x1Double = x1.toDouble()
            val y1Double = y1.toDouble()
            val z1Double = z1.toDouble()

            val x2Double = x2.toDouble()
            val y2Double = y2.toDouble()
            val z2Double = z2.toDouble()

            // Compare each component (x, y, z) with tolerance
            val xDiff = Math.abs(x1Double - x2Double)
            val yDiff = Math.abs(y1Double - y2Double)
            val zDiff = Math.abs(z1Double - z2Double)

            // Return true if all components are within the specified tolerance
            return xDiff <= tolerance && yDiff <= tolerance && zDiff <= tolerance


//            return x1Double == x2Double && y1Double == y2Double && z1Double == z2Double
        }

        // Return false if parsing fails or components are not comparable
        return false
    }


    override fun onEmpty() {
        fragmentCameraBinding.overlay.clear()
        activity?.runOnUiThread {
            faceBlendshapesResultAdapter.updateResults(null)
            faceBlendshapesResultAdapter.notifyDataSetChanged()
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            faceBlendshapesResultAdapter.updateResults(null)
            faceBlendshapesResultAdapter.notifyDataSetChanged()
        }
    }
}
