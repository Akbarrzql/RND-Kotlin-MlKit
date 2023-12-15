package com.example.testfacerecognition.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FaceCountourDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFace(faceDetection: FaceCountour)

    @Query("UPDATE face_countour SET faceContour = :detectCountour WHERE id = :id")
    suspend fun updateFaceLandmarks(id: Int, detectCountour: String)

    @Query("SELECT * FROM face_countour ORDER BY id ASC")
    fun readAllData(): LiveData<List<FaceCountour>>

    @Query("SELECT * FROM face_countour ORDER BY id ASC")
    suspend fun getAllFaces(): List<FaceCountour>

}