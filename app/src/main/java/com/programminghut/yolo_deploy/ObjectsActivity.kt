package com.example.yolo_deploy

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import androidx.appcompat.app.AppCompatActivity
import com.programminghut.yolo_deploy.R

class ObjectsActivity : AppCompatActivity() {

    private lateinit var objectsListView: ListView
    private lateinit var showAllButton: Button
    private lateinit var databaseHelper: DatabaseHelper
    private var tryNumber: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_objects)

        objectsListView = findViewById(R.id.objectsListView)
        showAllButton = findViewById(R.id.showAll)
        databaseHelper = DatabaseHelper(this)

        tryNumber = intent.getIntExtra("TRY_NUMBER", 0)

        showPredictedObjects()

        showAllButton.setOnClickListener {
            startActivity(Intent(this, AllTriesActivity::class.java))
        }
    }

    private fun showPredictedObjects() {
        val cursor: Cursor = databaseHelper.getPredictionsByTryNumber(tryNumber)
        val adapter = SimpleCursorAdapter(
            this,
            android.R.layout.simple_list_item_1,
            cursor,
            arrayOf(DatabaseHelper.COLUMN_OBJECT_NAME),
            intArrayOf(android.R.id.text1),
            0
        )
        objectsListView.adapter = adapter
    }
}
