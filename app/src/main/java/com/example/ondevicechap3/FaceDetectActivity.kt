package com.example.ondevicechap3

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.example.ondevicechap3.databinding.ActivityFaceDetectBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

private const val TAG = "FaceDetectActivity"
class FaceDetectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaceDetectBinding
    private lateinit var bitmap: Bitmap

    private lateinit var detector: FaceDetector

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceDetectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        detector = FaceDetection.getClient(highAccuracyOpts)

        binding.btnGallery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        binding.btnDetection.setOnClickListener {
            detectFace()
        }
    }

    private fun detectFace(){
        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces ->
                bitmap.apply {
                    binding.ivPic.setImageBitmap(this.drawFaceBoundBox(faces))
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }

    }

    fun Bitmap.drawFaceBoundBox(faces: List<Face>): Bitmap?{
        val newBitmap = copy(Bitmap.Config.ARGB_8888,true)
        val canvas = Canvas(newBitmap)
        val width = newBitmap.width
        Log.d(TAG, "drawFaceBoundBox: $width")

        val blur = BlurMaskFilter((width/90).toFloat(), BlurMaskFilter.Blur.NORMAL)

        faces.forEach { face ->
            val bounds = face.boundingBox
            val rad = minOf(bounds.width(), bounds.height()) /2
            val rgb = newBitmap.getPixel(bounds.centerX(), bounds.centerY())
            Log.d(TAG, "drawFaceBoundBox: $rgb")
            val bluredRGB = Color.argb(246, rgb.red, rgb.green, rgb.blue)

            Paint().apply {
                color = bluredRGB
                style = Paint.Style.FILL
                strokeWidth = (width/70).toFloat()
                isAntiAlias = true
                maskFilter = blur
                canvas.drawCircle(bounds.centerX().toFloat(), bounds.centerY().toFloat(), rad.toFloat(), this)
            }
        }

        return newBitmap
    }


    private val galleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){
            if (it){
                val intent = Intent(Intent.ACTION_PICK)
                intent.setDataAndType(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "image/*"
                )
                galleryResult.launch(intent)
            }
        }

    private val galleryResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imgUri = result.data?.data
                imgUri.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, it!!))
                        binding.ivPic.setImageBitmap(bitmap)
                    }
                }
            }
        }


}