package com.example.livenessfacedetection.core

import com.google.mlkit.vision.face.Face

interface DetectionTask {

    fun start() {}

    /**
     * @return ture if task completed
     */
    fun process(face: Face): Boolean
}