package com.example.compareimage.data.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FaceDetection::class], version = 1, exportSchema = false)
abstract class FaceDetectionDatabase : RoomDatabase() {
    abstract fun faceDetectionDao(): FaceDetectionDao

    companion object {
        @Volatile
        private var INSTANCE: FaceDetectionDatabase? = null

        fun getDatabase(context: Context): FaceDetectionDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceDetectionDatabase::class.java,
                    "face_detection_compare_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }

}