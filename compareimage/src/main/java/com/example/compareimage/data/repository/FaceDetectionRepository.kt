package com.example.compareimage.data.repository

import androidx.lifecycle.LiveData
import com.example.compareimage.data.model.FaceDetection
import com.example.compareimage.data.model.FaceDetectionDao

class FaceDetectionRepository(private val faceDetectionDao: FaceDetectionDao) {

    val readAllData: LiveData<List<FaceDetection>> = faceDetectionDao.readAllData()

    suspend fun addFace(faceDetection: FaceDetection) {
        faceDetectionDao.addFace(faceDetection)
    }

    suspend fun updateFaceLandmarks(id: Int, detectedLandmarks: String) {
        faceDetectionDao.updateFaceLandmarks(id, detectedLandmarks)
    }

    suspend fun getAllFaces(): List<FaceDetection> {
        return faceDetectionDao.getAllFaces()
    }

}