package com.example.testfacerecognition.repository

import androidx.lifecycle.LiveData
import com.example.testfacerecognition.data.FaceCountour
import com.example.testfacerecognition.data.FaceCountourDao

class FaceCountourRepository(private val faceDetectionDao: FaceCountourDao) {

    val readAllData: LiveData<List<FaceCountour>> = faceDetectionDao.readAllData()

    suspend fun addFace(faceDetection: FaceCountour) {
        faceDetectionDao.addFace(faceDetection)
    }

    suspend fun updateFaceLandmarks(id: Int, detectedLandmarks: String) {
        faceDetectionDao.updateFaceLandmarks(id, detectedLandmarks)
    }

    suspend fun getAllFaces(): List<FaceCountour> {
        return faceDetectionDao.getAllFaces()
    }
}