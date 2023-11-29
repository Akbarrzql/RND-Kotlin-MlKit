package com.example.compareimage.data.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface FaceDetectionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFace(faceDetection: FaceDetection)

    @Query("UPDATE face_detection_compare SET faceContour = :detectedLandmarks WHERE id = :id")
    suspend fun updateFaceLandmarks(id: Int, detectedLandmarks: String)

    @Query("SELECT * FROM face_detection_compare ORDER BY id ASC")
    fun readAllData(): LiveData<List<FaceDetection>>

    @Query("SELECT * FROM face_detection_compare ORDER BY id ASC")
    suspend fun getAllFaces(): List<FaceDetection>
}