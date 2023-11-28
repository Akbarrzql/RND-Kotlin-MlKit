package com.example.livenessfacedetection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.livenessfacedetection.adapter.CountourAdapter
import com.example.livenessfacedetection.databinding.ActivityLivenessFaceBinding

class LivenessFaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLivenessFaceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLivenessFaceBinding.inflate(layoutInflater)
        lifecycle.currentState
        setContentView(binding.root)

        val livenessLauncher = registerForActivityResult(DetectionActivity.ResultContract()) {
            binding.recyclerView.adapter = CountourAdapter(it.orEmpty())
            binding.saveBtn.visibility = if (it.isNullOrEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }


        binding.startBtn.setOnClickListener {
            livenessLauncher.launch(null)
        }

    }
}