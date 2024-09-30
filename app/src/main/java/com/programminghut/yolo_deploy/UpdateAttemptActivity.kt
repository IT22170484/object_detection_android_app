package com.example.yolo_deploy

import android.content.ContentValues
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.programminghut.yolo_deploy.R

class UpdateAttemptActivity : AppCompatActivity() {

    private lateinit var objectsEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var databaseHelper: DatabaseHelper
    private var tryNumber: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_attempt)

        objectsEditText = findViewById(R.id.objectsEditText)
        saveButton = findViewById(R.id.saveButton)
        databaseHelper = DatabaseHelper(this)

        // Retrieve the tryNumber and objects from the Intent
        tryNumber = intent.getIntExtra("TRY_NUMBER", -1)
        val objects = intent.getStringExtra("OBJECTS")

        // Set the current objects in the EditText for modification
        objectsEditText.setText(objects)

        saveButton.setOnClickListener {
            val updatedObjects = objectsEditText.text.toString()
            if (tryNumber != -1) {
                // Call update method
                val result = databaseHelper.updatePrediction(tryNumber.toString(), updatedObjects)
                if (result > 0) {
                    Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Invalid try number", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
