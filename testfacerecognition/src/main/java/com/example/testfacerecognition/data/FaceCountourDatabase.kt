    package com.example.testfacerecognition.data

    import android.content.Context
    import androidx.room.Database
    import androidx.room.Room
    import androidx.room.RoomDatabase

    @Database(entities = [FaceCountour::class], version = 1, exportSchema = false)
    abstract class FaceCountourDatabase: RoomDatabase() {
        abstract fun faceCountourDao(): FaceCountourDao

        companion object {
            @Volatile
            private var INSTANCE: FaceCountourDatabase? = null

            fun getDatabase(context: Context): FaceCountourDatabase {
                val tempInstance = INSTANCE
                if(tempInstance != null) {
                    return tempInstance
                }
                synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        FaceCountourDatabase::class.java,
                        "face_countour_database"
                    ).build()
                    INSTANCE = instance
                    return instance
                }
            }
        }

    }