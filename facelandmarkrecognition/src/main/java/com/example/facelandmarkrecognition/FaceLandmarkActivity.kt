package com.example.facelandmarkrecognition

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.facelandmarkrecognition.adapter.ViewPagerAdapter
import com.example.facelandmarkrecognition.databinding.ActivityFaceLandmarkBinding
import com.example.facelandmarkrecognition.fragment.CameraFragment
import com.example.facelandmarkrecognition.fragment.GalleryFragment
import com.google.android.material.tabs.TabLayoutMediator
import org.opencv.android.OpenCVLoader

class FaceLandmarkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceLandmarkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceLandmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OpenCVLoader.initDebug()

        val adapter = ViewPagerAdapter(supportFragmentManager)

        adapter.addFragment(CameraFragment(), "Camera")
        adapter.addFragment(GalleryFragment(), "Gallery")

        binding.viewPager.adapter = adapter

        binding.tabLayout.setupWithViewPager(binding.viewPager)

    }
}