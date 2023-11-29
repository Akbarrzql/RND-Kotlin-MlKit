package com.example.compareimage.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.mlkit.vision.face.FaceContour

@Entity (tableName = "face_detection_compare")
data class FaceDetection (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val idKaryawan: String,
    val nfcId: String,
    val namaKaryawan: String,
    val faceContour: String
)