package com.example.livenessfacedetection.core.tasks

import com.example.livenessfacedetection.core.DetectionTask
import com.example.livenessfacedetection.core.DetectionUtils
import com.google.mlkit.vision.face.Face

class MouthOpenDetectionTask : DetectionTask {

    override fun process(face: Face): Boolean {
        return DetectionUtils.isFacing(face) && DetectionUtils.isMouthOpened(face)
    }
}