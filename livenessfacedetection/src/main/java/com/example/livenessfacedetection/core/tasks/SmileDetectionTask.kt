package com.example.livenessfacedetection.core.tasks

import com.example.livenessfacedetection.core.DetectionUtils
import com.example.livenessfacedetection.core.DetectionTask
import com.google.mlkit.vision.face.Face

class SmileDetectionTask : DetectionTask {

    override fun process(face: Face): Boolean {
        val isSmile = (face.smilingProbability ?: 0f) > 0.67f
        return isSmile && DetectionUtils.isFacing(face)
    }
}