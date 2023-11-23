package com.example.facelandmarkrecognition.data.repository

import androidx.lifecycle.LiveData
import com.example.facelandmarkrecognition.data.model.FaceDetection
import com.example.facelandmarkrecognition.data.model.FaceDetectionDao
import com.example.facelandmarkrecognition.data.model.LandmarkPoint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class FaceDetectionRepository(private val faceDetectionDao: FaceDetectionDao) {

    val readAllData: LiveData<List<FaceDetection>> = faceDetectionDao.readAllData()

    suspend fun addFace(faceDetection: FaceDetection) {
        faceDetectionDao.addFace(faceDetection)
    }

    fun matchFaceLandmarks(detectedLandmarks: List<NormalizedLandmark>): FaceDetection? {
        val detectedLandmarksString = convertLandmarksToString(detectedLandmarks.map {
            LandmarkPoint(it.x(), it.y(), it.z())
        })
        return faceDetectionDao.matchFaceLandmarks(detectedLandmarksString)
    }


    private fun convertLandmarksToString(landmarks: List<LandmarkPoint>): String {
        return landmarks.joinToString(";") { "${it.x},${it.y},${it.z}" }
    }
}