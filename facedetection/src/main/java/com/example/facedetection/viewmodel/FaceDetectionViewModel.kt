package com.example.facedetection.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.facedetection.data.model.FaceDetection
import com.example.facedetection.data.model.FaceDetectionDatabase
import com.example.facedetection.data.repository.FaceDetectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FaceDetectionViewModel(application: Application) : AndroidViewModel(application) {

    private val readAllData: LiveData<List<FaceDetection>>
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

}