package com.example.ondevicechap3

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.ondevicechap3.databinding.ActivityStartBinding
import com.example.ondevicechap3.livedetection.LiveDetectActivity

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFaceDetect.setOnClickListener {
            val intent = Intent(this, FaceDetectActivity::class.java)
            startActivity(intent)
        }

        binding.btnImageLabel.setOnClickListener {
            val intent = Intent(this, ImageLabelingActivity::class.java)
            startActivity(intent)
        }

        binding.btnLiveDetection.setOnClickListener {
            val intent = Intent(this, LiveDetectActivity::class.java)
            startActivity(intent)
        }
    }
}