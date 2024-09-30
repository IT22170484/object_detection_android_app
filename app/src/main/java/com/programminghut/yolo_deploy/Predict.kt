package com.programminghut.yolo_deploy

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yolo_deploy.DatabaseHelper
import com.example.yolo_deploy.ObjectsActivity
import java.io.IOException

class Predict : AppCompatActivity() {
    private val IMAGE_PICK = 100
    private var imageView: ImageView? = null
    private var bitmap: Bitmap? = null
    private var yolov5TFLiteDetector: Yolov5TFLiteDetector? = null
    private val boxPaint = Paint()
    private val textPaint = Paint()
    private var predictButton: Button? = null
    private var objectsButton: Button? = null
    private var databaseHelper: DatabaseHelper? = null
    private var recognitions = ArrayList<Recognition?>()
    private var tryNumber = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_predict)
        imageView = findViewById(R.id.imageView)
        predictButton = findViewById(R.id.predict)
        objectsButton = findViewById(R.id.objects)
        databaseHelper = DatabaseHelper(this)
        yolov5TFLiteDetector = Yolov5TFLiteDetector()
        yolov5TFLiteDetector!!.setModelFile("yolov5s-fp16.tflite")
        yolov5TFLiteDetector!!.initialModel(this)
        boxPaint.strokeWidth = 5f
        boxPaint.style = Paint.Style.STROKE
        boxPaint.color = Color.RED
        textPaint.textSize = 50f
        textPaint.color = Color.GREEN
        textPaint.style = Paint.Style.FILL

        // Set the try number to the latest one in the database + 1
        tryNumber = nextTryNumber
        predictButton?.setOnClickListener { v: View? -> predict() }
        objectsButton?.setOnClickListener { v: View? -> showObjects() }

// Set fixed dimensions for the ImageView
        imageView?.let {
            it.layoutParams.width = resources.getDimensionPixelSize(R.dimen.image_view_width)
            it.layoutParams.height = resources.getDimensionPixelSize(R.dimen.image_view_height)
            it.requestLayout()
        }

    }

    fun selectImage(view: View?) {
        val intent = Intent()
        intent.setAction(Intent.ACTION_PICK)
        intent.setType("image/*")
        startActivityForResult(intent, IMAGE_PICK)
        // Reset tryNumber for the new image selection
        tryNumber = nextTryNumber // This will set a new try number for each image selection
    }

    fun predict() {
        if (bitmap != null) {
            // Resize the bitmap to a fixed size (e.g., 800x600 pixels)
            val resizedBitmap = resizeBitmap(bitmap, 800, 600)

            // Create a mutable bitmap from the resized bitmap
            val mutableBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            // Clear previous drawings by filling the canvas with the resized bitmap
            canvas.drawBitmap(resizedBitmap, 0f, 0f, null)

            // Perform object detection
            recognitions = yolov5TFLiteDetector!!.detect(resizedBitmap)

            // Clear previous results from the database for the current tryNumber
            databaseHelper!!.clearPredictionsForTryNumber(tryNumber)

            // Create a map to count occurrences of each object
            val objectCountMap = HashMap<String?, Int>()

            // Draw rectangles and text on the mutable bitmap
            for (recognition in recognitions) {
                if (recognition!!.confidence!! > 0.4) {
                    val location = recognition.getLocation()
                    canvas.drawRect(location, boxPaint)
                    canvas.drawText(
                        recognition.labelName + ":" + recognition.confidence,
                        location.left,
                        location.top,
                        textPaint
                    )

                    // Count occurrences of each object
                    val labelName = recognition.labelName
                    objectCountMap[labelName] = objectCountMap.getOrDefault(labelName, 0) + 1
                }
            }

            // Save counted predictions to the database
            for ((objectName, count) in objectCountMap) {
                databaseHelper!!.addPrediction(tryNumber, "$count $objectName") // Save as "3 books"
            }

            // Update the ImageView with the mutable bitmap
            imageView!!.setImageBitmap(mutableBitmap)

            // Check if the image is set correctly
            if (imageView!!.drawable == null) {
                Log.e("Predict", "Image is not set in the ImageView.")
            }
            objectsButton!!.visibility = View.VISIBLE // Show the button after prediction
        } else {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
        }
    }

    private val nextTryNumber: Int
        private get() {
            val cursor = databaseHelper!!.getAllPredictions()
            var lastTryNumber = 0

            // Check if the cursor has at least one entry and the column exists
            if (cursor.moveToLast()) {
                val tryNumberIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRY_NUMBER)

                // Ensure the index is valid
                lastTryNumber = if (tryNumberIndex != -1) {
                    cursor.getInt(tryNumberIndex)
                } else {
                    // Handle the error: column does not exist
                    throw IllegalStateException("COLUMN_TRY_NUMBER not found in the Cursor.")
                }
            }
            cursor.close()
            return lastTryNumber + 1 // Increment for the next try
        }

    private fun saveRecognitionToDatabase(objectName: String) {
        databaseHelper!!.addPrediction(tryNumber, objectName)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)

                // Resize the bitmap to a fixed size (e.g., 800x600 pixels)
                bitmap = resizeBitmap(bitmap, 800, 600)
                imageView!!.setImageBitmap(bitmap) // Display the selected image
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    private fun resizeBitmap(originalBitmap: Bitmap?, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(originalBitmap!!, width, height, true)
    }

    private fun showObjects() {
        val intent = Intent(this, ObjectsActivity::class.java)
        intent.putExtra("TRY_NUMBER", tryNumber) // Pass the try number to ObjectsActivity
        startActivity(intent)
    }
}
