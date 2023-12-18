package com.example.testfacerecognition

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.testfacerecognition.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val result = intent.getStringExtra("name")
        binding.tvResult.text = "Mendeteksi sebagai: \n $result"

    }
}