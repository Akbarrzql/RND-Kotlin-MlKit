package com.example.facelandmarkrecognition.fragment

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.facelandmarkrecognition.FaceLandmarkerHelper
import com.example.facelandmarkrecognition.R
import com.example.facelandmarkrecognition.viewmodel.MainViewModel
import com.example.facelandmarkrecognition.adapter.FaceBlendshapesResultAdapter
import com.example.facelandmarkrecognition.customview.OverlayView
import com.example.facelandmarkrecognition.data.model.FaceDetection
import com.example.facelandmarkrecognition.databinding.FragmentGalleryBinding
import com.example.facelandmarkrecognition.viewmodel.FaceDetectionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class GalleryFragment : Fragment(), FaceLandmarkerHelper.LandmarkerListener {

    enum class MediaType {
        IMAGE,
        VIDEO,
        UNKNOWN
    }

    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
    private lateinit var faceDetectionViewModel: FaceDetectionViewModel
    private lateinit var landmark: String
    private val fragmentGalleryBinding
        get() = _fragmentGalleryBinding!!
    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private val faceBlendshapesResultAdapter by lazy {
        FaceBlendshapesResultAdapter()
    }

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ScheduledExecutorService

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            // Handle the returned Uri
            uri?.let { mediaUri ->
                when (val mediaType = loadMediaType(mediaUri)) {
                    MediaType.IMAGE -> runDetectionOnImage(mediaUri)
                    MediaType.VIDEO -> runDetectionOnVideo(mediaUri)
                    MediaType.UNKNOWN -> {
                        updateDisplayView(mediaType)
                        Toast.makeText(
                            requireContext(),
                            "Unsupported data type.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding = FragmentGalleryBinding.inflate(inflater, container, false)

        faceDetectionViewModel = ViewModelProvider(this)[FaceDetectionViewModel::class.java]

        return fragmentGalleryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val overlayView = view.findViewById<OverlayView>(R.id.overlay)
        overlayView.setViewModelStoreOwner(requireActivity())

        fragmentGalleryBinding.fabGetContent.setOnClickListener {
            getContent.launch(arrayOf("image/*", "video/*"))
        }
        fragmentGalleryBinding.fabPostImage.setOnClickListener {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.custom_show_dialog_face)

            dialog.window?.setBackgroundDrawableResource(R.color.transparent)

            val noBtn = dialog.findViewById(R.id.btn_no) as Button
            val yesBtn = dialog.findViewById(R.id.btn_yes) as Button
            val ivFace = dialog.findViewById(R.id.face) as ImageView
            val etName = dialog.findViewById(R.id.et_name) as TextView

            if (fragmentGalleryBinding.imageResult.drawable != null) {
                // Use BitmapFactory to decode the image and set it to the ImageView
                // Run face landmarker on the input image

                //manruh gambarnya ke imageview dan terdapat landmarker
                val bitmap = fragmentGalleryBinding.imageResult.drawable.toBitmap()

                // Run face landmarker on the input image
                backgroundExecutor.execute {

                    faceLandmarkerHelper =
                        FaceLandmarkerHelper(
                            context = requireContext(),
                            runningMode = RunningMode.IMAGE,
                            minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                            minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                            minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                            maxNumFaces = viewModel.currentMaxFaces,
                            currentDelegate = viewModel.currentDelegate
                        )

                    faceLandmarkerHelper.detectImage(bitmap)?.let { result ->
                        activity?.runOnUiThread {
                            if (fragmentGalleryBinding.recyclerviewResults.scrollState != ViewPager2.SCROLL_STATE_DRAGGING) {
                                faceBlendshapesResultAdapter.updateResults(result.result)
                                landmark = result.result.faceLandmarks().toString()
                                faceBlendshapesResultAdapter.notifyDataSetChanged()
                            }
                            fragmentGalleryBinding.overlay.setResults(
                                result.result,
                                bitmap.height,
                                bitmap.width,
                                RunningMode.IMAGE
                            )

                        }
                    } ?: run { Log.e(TAG, "Error running face landmarker.") }

                    faceLandmarkerHelper.clearFaceLandmarker()

                }
                ivFace.setImageBitmap(bitmap)

            }

            fragmentGalleryBinding.apply {
                yesBtn.setOnClickListener {
                    val name = etName.text.toString()
                    val face = fragmentGalleryBinding.imageResult.drawable.toBitmap().toString()
                    val faceDetection = FaceDetection(
                        0,
                        name,
                        face,
                        landmark
                    )
                    faceDetectionViewModel.addFace(faceDetection)
                    Log.d("X and Y coordinates to Database", landmark)
                    Log.d("Data in Room", faceDetection.toString())
                    Toast.makeText(requireContext(), "Berhasil menambahkan wajah", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                noBtn.setOnClickListener { dialog.dismiss() }
            }


            dialog.show()
        }
        with(fragmentGalleryBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = faceBlendshapesResultAdapter
        }

    }

    override fun onPause() {
        fragmentGalleryBinding.overlay.clear()
        if (fragmentGalleryBinding.videoView.isPlaying) {
            fragmentGalleryBinding.videoView.stopPlayback()
        }
        fragmentGalleryBinding.videoView.visibility = View.GONE
        fragmentGalleryBinding.imageResult.visibility = View.GONE
        fragmentGalleryBinding.tvPlaceholder.visibility = View.VISIBLE

        activity?.runOnUiThread {
            faceBlendshapesResultAdapter.updateResults(null)
            faceBlendshapesResultAdapter.notifyDataSetChanged()
        }
        super.onPause()
    }

    // Load and display the image.
    private fun runDetectionOnImage(uri: Uri) {
        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        updateDisplayView(MediaType.IMAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(
                requireActivity().contentResolver,
                uri
            )
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(
                requireActivity().contentResolver,
                uri
            )
        }
            .copy(Bitmap.Config.ARGB_8888, true)
            ?.let { bitmap ->
                fragmentGalleryBinding.imageResult.setImageBitmap(bitmap)

                // Run face landmarker on the input image
                backgroundExecutor.execute {

                    faceLandmarkerHelper =
                        FaceLandmarkerHelper(
                            context = requireContext(),
                            runningMode = RunningMode.IMAGE,
                            minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                            minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                            minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                            maxNumFaces = viewModel.currentMaxFaces,
                            currentDelegate = viewModel.currentDelegate
                        )

                    faceLandmarkerHelper.detectImage(bitmap)?.let { result ->
                        activity?.runOnUiThread {
                            if (fragmentGalleryBinding.recyclerviewResults.scrollState != ViewPager2.SCROLL_STATE_DRAGGING) {
                                faceBlendshapesResultAdapter.updateResults(result.result)
                                faceBlendshapesResultAdapter.notifyDataSetChanged()
                            }
                            fragmentGalleryBinding.overlay.setResults(
                                result.result,
                                bitmap.height,
                                bitmap.width,
                                RunningMode.IMAGE
                            )
                            landmark = result.result.faceLandmarks().toString()
                            Log.d("X and Y coordinates", landmark)

                            //menampilkan isi di room database ke logcat
                            faceDetectionViewModel.readAllData.observe(viewLifecycleOwner) { faceDetection ->
                                for (face in faceDetection) {

                                    //megencek apakah landmark yang didapat sama dengan landmark yang ada di database susuai dengan nama yang diinputkan
                                    if (landmark == face.faceLandmarks) {
                                        Log.d("Data in Room", face.toString())
                                        Toast.makeText(requireContext(), "Nama: ${face.name}", Toast.LENGTH_SHORT).show()

                                        fragmentGalleryBinding.overlay.setDetectedFace(face)
                                    }
                                }
                            }

                        }
                    } ?: run { Log.e(TAG, "Error running face landmarker.") }

                    faceLandmarkerHelper.clearFaceLandmarker()
                }
            }
    }

    private fun runDetectionOnVideo(uri: Uri) {
        updateDisplayView(MediaType.VIDEO)

        with(fragmentGalleryBinding.videoView) {
            setVideoURI(uri)
            // mute the audio
            setOnPreparedListener { it.setVolume(0f, 0f) }
            requestFocus()
        }

        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        backgroundExecutor.execute {

            faceLandmarkerHelper =
                FaceLandmarkerHelper(
                    context = requireContext(),
                    runningMode = RunningMode.VIDEO,
                    minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                    minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                    minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                    maxNumFaces = viewModel.currentMaxFaces,
                    currentDelegate = viewModel.currentDelegate
                )

            activity?.runOnUiThread {
                fragmentGalleryBinding.videoView.visibility = View.GONE
                fragmentGalleryBinding.progress.visibility = View.VISIBLE
            }

            faceLandmarkerHelper.detectVideoFile(uri, VIDEO_INTERVAL_MS)
                ?.let { resultBundle ->
                    activity?.runOnUiThread { displayVideoResult(resultBundle) }
                }
                ?: run { Log.e(TAG, "Error running face landmarker.") }

            faceLandmarkerHelper.clearFaceLandmarker()
        }
    }

    // Setup and display the video.
    private fun displayVideoResult(result: FaceLandmarkerHelper.VideoResultBundle) {

        fragmentGalleryBinding.videoView.visibility = View.VISIBLE
        fragmentGalleryBinding.progress.visibility = View.GONE

        fragmentGalleryBinding.videoView.start()
        val videoStartTimeMs = SystemClock.uptimeMillis()

        backgroundExecutor.scheduleAtFixedRate(
            {
                activity?.runOnUiThread {
                    val videoElapsedTimeMs =
                        SystemClock.uptimeMillis() - videoStartTimeMs
                    val resultIndex =
                        videoElapsedTimeMs.div(VIDEO_INTERVAL_MS).toInt()

                    if (resultIndex >= result.results.size || fragmentGalleryBinding.videoView.visibility == View.GONE) {
                        // The video playback has finished so we stop drawing bounding boxes
                        backgroundExecutor.shutdown()
                    } else {
                        fragmentGalleryBinding.overlay.setResults(
                            result.results[resultIndex],
                            result.inputImageHeight,
                            result.inputImageWidth,
                            RunningMode.VIDEO
                        )

                        if (fragmentGalleryBinding.recyclerviewResults.scrollState != ViewPager2.SCROLL_STATE_DRAGGING) {
                            faceBlendshapesResultAdapter.updateResults(result.results[resultIndex])
                            faceBlendshapesResultAdapter.notifyDataSetChanged()
                        }

//                        activity?.runOnUiThread {
//                            if (fragmentGalleryBinding.recyclerviewResults.scrollState != ViewPager2.SCROLL_STATE_DRAGGING) {
//                                faceBlendshapesResultAdapter.updateResults(result.results[resultIndex])
//                                faceBlendshapesResultAdapter.notifyDataSetChanged()
//                            }
//                            fragmentGalleryBinding.overlay.setResults(
//                                result.results[resultIndex],
//                                result.inputImageHeight,
//                                result.inputImageWidth,
//                                RunningMode.VIDEO
//                            )
//                            landmark = result.results[resultIndex].faceLandmarks().get(0).get(0).toString()
//                            Log.d("X and Y coordinates", landmark)
//
//                            //menampilkan isi di room database ke logcat
//                            faceDetectionViewModel.readAllData.observe(viewLifecycleOwner) { faceDetection ->
//                                for (face in faceDetection) {
//
//                                    //megencek apakah landmark yang didapat sama dengan landmark yang ada di database susuai dengan nama yang diinputkan
//                                    if (landmark == face.faceLandmarks) {
//                                        Log.d("Data in Face", face.toString())
//                                        Toast.makeText(requireContext(), "Nama: ${face.name}", Toast.LENGTH_SHORT).show()
//
//                                        fragmentGalleryBinding.overlay.setDetectedFace(face)
//                                    }
//                                }
//                            }
//
//                        }

                    }
                }
            },
            0,
            VIDEO_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )

    }

    private fun updateDisplayView(mediaType: MediaType) {
        fragmentGalleryBinding.imageResult.visibility =
            if (mediaType == MediaType.IMAGE) View.VISIBLE else View.GONE
        fragmentGalleryBinding.videoView.visibility =
            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
        fragmentGalleryBinding.tvPlaceholder.visibility =
            if (mediaType == MediaType.UNKNOWN) View.VISIBLE else View.GONE
    }

    // Check the type of media that user selected.
    private fun loadMediaType(uri: Uri): MediaType {
        val mimeType = context?.contentResolver?.getType(uri)
        mimeType?.let {
            if (mimeType.startsWith("image")) return MediaType.IMAGE
            if (mimeType.startsWith("video")) return MediaType.VIDEO
        }

        return MediaType.UNKNOWN
    }

    private fun classifyingError() {
        activity?.runOnUiThread {
            fragmentGalleryBinding.progress.visibility = View.GONE
            updateDisplayView(MediaType.UNKNOWN)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        classifyingError()
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == FaceLandmarkerHelper.GPU_ERROR) {
            }
        }
    }

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        // no-op
    }

    companion object {
        private const val TAG = "GalleryFragment"

        // Value used to get frames at specific intervals for inference (e.g. every 300ms)
        private const val VIDEO_INTERVAL_MS = 300L
    }
}