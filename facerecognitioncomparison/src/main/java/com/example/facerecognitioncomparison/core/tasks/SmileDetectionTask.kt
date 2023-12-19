package com.example.facerecognitioncomparison.core.tasks

import com.example.facerecognitioncomparison.core.DetectionUtils
import com.example.facerecognitioncomparison.core.DetectionTask
import com.google.mlkit.vision.face.Face

class SmileDetectionTask : DetectionTask {

    override fun process(face: Face): Boolean {
        val isSmile = (face.smilingProbability ?: 0f) > 0.67f
        return isSmile && DetectionUtils.isFacing(face)
    }
}