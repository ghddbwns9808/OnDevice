package com.example.ondevicechap3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ondevicechap3.databinding.ActivityImageLabelingBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

private const val TAG = "ImageLabelingActivity"
class ImageLabelingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageLabelingBinding
    private lateinit var bitmap: Bitmap
    private lateinit var labeler: ImageLabeler
    private lateinit var objectDetector: ObjectDetector
    private var outputText = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageLabelingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        binding.btnGallery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        binding.btnImageLabel.setOnClickListener {
//            labelImage()
            detectObject()
        }
    }

    private fun detectObject(){
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .build()

        objectDetector = ObjectDetection.getClient(options)
        var image = InputImage.fromBitmap(bitmap, 0)
        objectDetector.process(image)
            .addOnSuccessListener {detectedObjects ->
                bitmap.apply {
                    binding.ivPic.setImageBitmap(drawWithRectangle(detectedObjects))
                    getLabels(this, detectedObjects)
                }
            }.addOnFailureListener {

            }
    }

    fun getLabels(bitmap: Bitmap, objects: List<DetectedObject>){
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        objects.forEachIndexed { idx, it ->
            val bounds = it.boundingBox
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                bounds.left,
                bounds.top,
                bounds.width(),
                bounds.height()
            )

            var image = InputImage.fromBitmap(croppedBitmap, 0)

            labeler.process(image)
                .addOnSuccessListener{labels ->
                    var labelText = ""
                    if (labels.count() > 0){
                        labelText = binding.tvLabels.text.toString()
                        labelText += "${idx+1}\n"
                        labels.forEachIndexed { index, thisLabel ->
                            if (thisLabel.confidence > 0.5f) {
                                labelText += thisLabel.text +
                                        ": ${((thisLabel.confidence * 10000).toInt()).toFloat() / 100.0f}%, "
                                if (index % 2 == 1)
                                    labelText += "\n"
                            }
                        }
                        labelText += "\n"
                    }else{
                        labelText = "Not found.\n"
                    }
                    binding.tvLabels.text = labelText.toString()
                }.addOnFailureListener{

                }
        }
    }

    fun Bitmap.drawWithRectangle(objects: List<DetectedObject>): Bitmap?{
        val newBitmap = copy(Bitmap.Config.ARGB_8888,true)
        val canvas = Canvas(newBitmap)
        val width = canvas.width
        Log.d(TAG, "drawWithRectangle: ${canvas.width}")
        var thisLabel = 0
        objects.forEach {
            thisLabel++
            val bounds = it.boundingBox
            Paint().apply {
                color = Color.GREEN
                style = Paint.Style.STROKE
                textSize = (width/20).toFloat()
                strokeWidth = (width/100).toFloat()
                isAntiAlias = true
                canvas.drawRect(
                    bounds,
                    this
                )
                canvas.drawText(thisLabel.toString(),
                    bounds.left.toFloat() - (width/25).toFloat(),
                    bounds.top.toFloat() + (width/25).toFloat(), this)
            }
        }
        return newBitmap
    }

    private fun labelImage(){
        labeler.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { labels ->
                labels.forEachIndexed { idx, label ->
                    outputText += "${idx+1} ${label.text} : ${label.confidence}"
                }
            }.addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
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
                binding.tvLabels.text = ""
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