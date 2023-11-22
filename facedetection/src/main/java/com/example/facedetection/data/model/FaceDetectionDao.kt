package com.example.facedetection.data.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FaceDetectionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFace(faceDetection: FaceDetection)

    @Query("SELECT * FROM face_detection ORDER BY id ASC")
    fun readAllData(): LiveData<List<FaceDetection>>
}