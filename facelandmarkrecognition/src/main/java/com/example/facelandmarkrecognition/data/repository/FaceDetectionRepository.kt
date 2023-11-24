package com.example.facelandmarkrecognition.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.facelandmarkrecognition.data.model.FaceDetection
import com.example.facelandmarkrecognition.data.model.FaceDetectionDao
import com.example.facelandmarkrecognition.data.model.LandmarkPoint

class FaceDetectionRepository(private val faceDetectionDao: FaceDetectionDao) {

    val readAllData: LiveData<List<FaceDetection>> = faceDetectionDao.readAllData()

    suspend fun addFace(faceDetection: FaceDetection) {
        faceDetectionDao.addFace(faceDetection)
    }

    suspend fun updateFaceLandmarks(id: Int, detectedLandmarks: String) {
        faceDetectionDao.updateFaceLandmarks(id, detectedLandmarks)
    }

}