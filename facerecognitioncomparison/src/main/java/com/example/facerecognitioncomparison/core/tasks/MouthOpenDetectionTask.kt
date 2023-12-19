package com.example.facerecognitioncomparison.core.tasks

import com.example.facerecognitioncomparison.core.DetectionTask
import com.example.facerecognitioncomparison.core.DetectionUtils
import com.google.mlkit.vision.face.Face

class MouthOpenDetectionTask : DetectionTask {

    override fun process(face: Face): Boolean {
        return DetectionUtils.isFacing(face) && DetectionUtils.isMouthOpened(face)
    }
}