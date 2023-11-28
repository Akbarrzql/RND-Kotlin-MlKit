package com.example.facedetection.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "face_detection")
data class FaceDetection (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val countour: String,
)