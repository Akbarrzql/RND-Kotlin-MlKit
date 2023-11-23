    package com.example.facelandmarkrecognition.customview

    import android.content.Context
    import android.graphics.Canvas
    import android.graphics.Color
    import android.graphics.Paint
    import android.util.AttributeSet
    import android.util.Log
    import android.view.View
    import androidx.core.content.ContextCompat
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.ViewModelStoreOwner
    import com.example.facelandmarkrecognition.R
    import com.example.facelandmarkrecognition.data.repository.FaceDetectionRepository
    import com.example.facelandmarkrecognition.viewmodel.FaceDetectionViewModel
    import com.google.mediapipe.tasks.vision.core.RunningMode
    import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
    import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
    import kotlin.math.max
    import kotlin.math.min

    class OverlayView(context: Context?, attrs: AttributeSet?) :
        View(context, attrs) {

        private var results: FaceLandmarkerResult? = null
        private var linePaint = Paint()
        private var pointPaint = Paint()
        private var faceDetectionRepository: FaceDetectionRepository? = null
        private var viewModelStoreOwner: ViewModelStoreOwner? = null

        private var scaleFactor: Float = 1f
        private var imageWidth: Int = 1
        private var imageHeight: Int = 1

        private lateinit var faceDetectionViewModel: FaceDetectionViewModel

        init {
            initPaints()
        }

        fun setFaceDetectionRepository(repository: FaceDetectionRepository) {
            faceDetectionRepository = repository
        }

        fun setViewModelStoreOwner(owner: ViewModelStoreOwner) {
            viewModelStoreOwner = owner
            faceDetectionViewModel = ViewModelProvider(viewModelStoreOwner!!)[FaceDetectionViewModel::class.java]
        }

        fun clear() {
            results = null
            linePaint.reset()
            pointPaint.reset()
            invalidate()
            initPaints()
        }

        private fun initPaints() {
            linePaint.color =
                ContextCompat.getColor(context!!, R.color.mp_color_primary)
            linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
            linePaint.style = Paint.Style.STROKE

            pointPaint.color = Color.YELLOW
            pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
            pointPaint.style = Paint.Style.FILL
        }

          override fun draw(canvas: Canvas) {
            super.draw(canvas)
            if (results == null || results!!.faceLandmarks().isEmpty()) {
                clear()
                return
            }

              results?.let { faceLandmarkerResult ->
                  val faceLandmarksList = faceLandmarkerResult.faceLandmarks()
                  if (faceLandmarksList.isNotEmpty()) {
                      val faceLandmarks = faceLandmarksList[0]

                      // Calculate bounding box around the face
                      var minX = Float.MAX_VALUE
                      var minY = Float.MAX_VALUE
                      var maxX = Float.MIN_VALUE
                      var maxY = Float.MIN_VALUE

                      for (landmark in faceLandmarks) {
                          val x = landmark.x() * imageWidth * scaleFactor
                          val y = landmark.y() * imageHeight * scaleFactor

                          minX = min(minX, x)
                          minY = min(minY, y)
                          maxX = max(maxX, x)
                          maxY = max(maxY, y)
                      }

                      // Draw bounding box
                      linePaint.color = Color.GREEN
                      canvas.drawRect(minX, minY, maxX, maxY, linePaint)

                      // Draw landmarks
                      pointPaint.color = Color.YELLOW
                      for (landmark in faceLandmarks) {
                          val x = landmark.x() * imageWidth * scaleFactor
                          val y = landmark.y() * imageHeight * scaleFactor
                          canvas.drawPoint(x, y, pointPaint)
                      }

                      // Draw connectors
                      linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
                      FaceLandmarker.FACE_LANDMARKS_CONNECTORS.forEach { connector ->
                          val startLandmark = faceLandmarks[connector.start()]
                          val endLandmark = faceLandmarks[connector.end()]

                          canvas.drawLine(
                              startLandmark.x() * imageWidth * scaleFactor,
                              startLandmark.y() * imageHeight * scaleFactor,
                              endLandmark.x() * imageWidth * scaleFactor,
                              endLandmark.y() * imageHeight * scaleFactor,
                              linePaint
                          )
                      }

                      // Convert detected landmarks to Normalized Landmark format
                      val singleLandmark = faceLandmarks[0]
                      Log.d("Single Landmark", singleLandmark.toString())

                      val normalizedLandmark = "<Normalized Landmark (x=${singleLandmark.x()} y=${singleLandmark.y()} z=${singleLandmark.z()})>"

                      Log.d("Detected Landmark", normalizedLandmark)

                      // Match landmarks with database and get name
                        val matchedName = faceDetectionRepository?.matchFaceLandmarks(faceLandmarks)
                      Log.d("Matched Name", matchedName.toString())

                      // Draw name in the bounding box
                      if (matchedName != null) {
                          val nameX = minX
                          val nameY = minY - 20 // Adjust the position of the name
                          val namePaint = Paint().apply {
                              color = Color.WHITE
                              textSize = 30F // Adjust the text size
                          }
                            canvas.drawText(matchedName.name, nameX, nameY, namePaint)
                      } else {
                          val nameX = minX
                          val nameY = minY - 20 // Adjust the position of the name
                          val namePaint = Paint().apply {
                              color = Color.WHITE
                              textSize = 30F // Adjust the text size
                          }
                          canvas.drawText("Wajah Tidak Terdeteksi", nameX, nameY, namePaint)
                      }


                  }
              }

          }

        fun setResults(
            faceLandmarkerResults: FaceLandmarkerResult,
            imageHeight: Int,
            imageWidth: Int,
            runningMode: RunningMode = RunningMode.IMAGE
        ) {
            results = faceLandmarkerResults

            this.imageHeight = imageHeight
            this.imageWidth = imageWidth

            scaleFactor = when (runningMode) {
                RunningMode.IMAGE,
                RunningMode.VIDEO -> {
                    min(width * 1f / imageWidth, height * 1f / imageHeight)
                }
                RunningMode.LIVE_STREAM -> {
                    // PreviewView is in FILL_START mode. So we need to scale up the
                    // landmarks to match with the size that the captured images will be
                    // displayed.
                    max(width * 1f / imageWidth, height * 1f / imageHeight)
                }
            }
            invalidate()
        }

        companion object {
            private const val LANDMARK_STROKE_WIDTH = 8F
            private const val TAG = "Face Landmarker Overlay"
        }
    }
