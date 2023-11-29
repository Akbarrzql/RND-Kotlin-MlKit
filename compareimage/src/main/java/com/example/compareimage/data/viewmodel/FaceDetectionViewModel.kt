package com.example.compareimage.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.compareimage.data.model.FaceDetection
import com.example.compareimage.data.model.FaceDetectionDatabase
import com.example.compareimage.data.repository.FaceDetectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.core.Point

class FaceDetectionViewModel(application: Application) : AndroidViewModel(application) {

    val readAllData: LiveData<List<FaceDetection>>
    private val repository: FaceDetectionRepository

    init {
        val faceDetectionDao = FaceDetectionDatabase.getDatabase(application).faceDetectionDao()
        repository = FaceDetectionRepository(faceDetectionDao)
        readAllData = repository.readAllData
    }

    fun addFace(faceDetection: FaceDetection) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addFace(faceDetection)
        }
    }

    fun updateFaceLandmarks(id: Int, detectedLandmarks: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFaceLandmarks(id, detectedLandmarks)
        }
    }

    suspend fun getMatchingFace(detectedContour: String): FaceDetection? {
        // Mendapatkan semua wajah dari database
        val allFaces = repository.getAllFaces()

        // Mencocokkan kontur wajah yang baru dengan yang sudah ada di database
        for (face in allFaces) {
            val savedContour = face.faceContour

            // Lakukan perbandingan atau pemrosesan lainnya sesuai kebutuhan
            if (isContoursMatch(detectedContour, savedContour)) {
                return face
            }
        }

        return null // Tidak ada wajah yang cocok ditemukan
    }

    private fun isContoursMatch(detectedContour: String, savedContour: String): Boolean {
        // Ubah string kontur menjadi daftar titik
        val detectedPoints = stringToPoints(detectedContour)
        val savedPoints = stringToPoints(savedContour)

        // Perbandingan berdasarkan jarak Euclidean
        val distance = calculateEuclideanDistance(detectedPoints, savedPoints)

        // Sesuaikan nilai ambang sesuai dengan karakteristik data Anda
        val threshold = 10.0

        // Kontur dianggap cocok jika jarak Euclidean kurang dari ambang
        return distance < threshold
    }

    private fun stringToPoints(contourString: String): List<Point> {
        val points = mutableListOf<Point>()
        // Menghilangkan karakter kurung siku dan kata PointF
        val cleanedString = contourString.replace("PointF(", "").replace(")", "")
        val pointStrings = cleanedString.split(",")

        for (i in 0 until pointStrings.size step 2) {
            val x = pointStrings[i].toFloatOrNull()
            val y = pointStrings[i].toFloatOrNull()

            if (x != null && y != null) {
                points.add(Point(x.toDouble(), y.toDouble()))
            }
        }

        return points
    }


    private fun calculateEuclideanDistance(points1: List<Point>, points2: List<Point>): Double {
        var sum = 0.0

        for (i in points1.indices) {
            val dx = points1[i].x - points2[i].x
            val dy = points1[i].y - points2[i].y
            sum += Math.sqrt(dx * dx + dy * dy)
        }

        return sum
    }


}