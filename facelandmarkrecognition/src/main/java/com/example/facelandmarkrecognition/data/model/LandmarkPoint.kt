package com.example.facelandmarkrecognition.data.model

data class LandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float
) {
    // Tambahkan fungsi untuk menghasilkan string format
    fun toFormattedString(): String {
        return "<Normalized Landmark (x=$x y=$y z=$z)>"
    }
}

