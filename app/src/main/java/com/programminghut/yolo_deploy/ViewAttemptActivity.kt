package com.example.yolo_deploy

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.programminghut.yolo_deploy.R

class ViewAttemptActivity : AppCompatActivity() {

    private lateinit var objectsTextView: TextView
    private lateinit var tryNumberTextView: TextView // Add this to display try number

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_attempt)

        objectsTextView = findViewById(R.id.objectsTextView)
        tryNumberTextView = findViewById(R.id.tryNumberTextView) // Initialize your new TextView

        // Retrieve the tryNumber and objects from the Intent
        val tryNumber = intent.getIntExtra("TRY_NUMBER", -1)
        val objects = intent.getStringExtra("OBJECTS")

        // Check if tryNumber is valid
        if (tryNumber != -1) {
            tryNumberTextView.text = "Try Number: $tryNumber" // Display the try number
        } else {
            tryNumberTextView.text = "Try Number: Not available"
        }

        // Display all objects (objects is assumed to be a concatenated string)
        objectsTextView.text = "Objects: $objects"
    }
}
