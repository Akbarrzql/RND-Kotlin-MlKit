package com.example.facedetection.data.repository

import androidx.lifecycle.LiveData
import com.example.facedetection.data.model.FaceDetection
import com.example.facedetection.data.model.FaceDetectionDao

class FaceDetectionRepository(private val faceDetectionDao: FaceDetectionDao) {

    val readAllData: LiveData<List<FaceDetection>> = faceDetectionDao.readAllData()

    suspend fun addFace(faceDetection: FaceDetection) {
        faceDetectionDao.addFace(faceDetection)
    }

}