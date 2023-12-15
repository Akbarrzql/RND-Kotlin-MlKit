package com.example.testfacerecognition.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.mlkit.vision.face.FaceContour

@Entity (tableName = "face_countour")
data class FaceCountour (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val image: String,
    val nfcId: String,
    val faceContour: String
)