package com.example.yolo_deploy

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.programminghut.yolo_deploy.R

class AllTriesActivity : AppCompatActivity() {

    private lateinit var allTriesListView: ListView
    private lateinit var backButton: Button
    private lateinit var generateReportButton: Button
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_tries)

        allTriesListView = findViewById(R.id.allTriesListView)
        backButton = findViewById(R.id.backButton)
        generateReportButton = findViewById(R.id.generateReportButton)
        databaseHelper = DatabaseHelper(this)

        showAllTries()

        backButton.setOnClickListener {
            finish()
        }

        // Handle report generation
        generateReportButton.setOnClickListener {
            generateReport()
        }
    }

    private fun showAllTries() {
        val cursor: Cursor = databaseHelper.getAllPredictionsGroupedByTryNumber()
        val adapter = object : SimpleCursorAdapter(
            this,
            R.layout.tries_list_item,
            cursor,
            arrayOf(DatabaseHelper.COLUMN_TRY_NUMBER, "objects"),
            intArrayOf(R.id.tryNumberTextView, R.id.objectsTextView),
            0
        ) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                super.bindView(view, context, cursor)

                val tryNumber = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRY_NUMBER))
                val objects = cursor.getString(cursor.getColumnIndexOrThrow("objects"))

                val viewButton: Button = view.findViewById(R.id.viewButton)
                val updateButton: Button = view.findViewById(R.id.updateButton)
                val deleteButton: Button = view.findViewById(R.id.deleteButton)

                viewButton.setOnClickListener {
                    val intent = Intent(context, ViewAttemptActivity::class.java).apply {
                        putExtra("TRY_NUMBER", tryNumber)
                        putExtra("OBJECTS", objects)
                    }
                    startActivity(intent)
                }

                updateButton.setOnClickListener {
                    val intent = Intent(context, UpdateAttemptActivity::class.java).apply {
                        putExtra("TRY_NUMBER", tryNumber)
                        putExtra("OBJECTS", objects)
                    }
                    startActivityForResult(intent, REQUEST_UPDATE)
                }

                deleteButton.setOnClickListener {
                    AlertDialog.Builder(context)
                        .setTitle("Delete Prediction")
                        .setMessage("Are you sure you want to delete this prediction?")
                        .setPositiveButton("Yes") { _, _ ->
                            val result = databaseHelper.deletePrediction(tryNumber.toString())
                            if (result > 0) {
                                Toast.makeText(context, "Prediction deleted", Toast.LENGTH_SHORT).show()
                                showAllTries() // Refresh the list
                            } else {
                                Toast.makeText(context, "Failed to delete prediction", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
        }
        allTriesListView.adapter = adapter
    }

    // Generate the report based on the current tries
    private fun generateReport() {
        val reportData = mutableListOf<ReportItem>()
        val cursor: Cursor = databaseHelper.getAllPredictionsGroupedByTryNumber()

        if (cursor.moveToFirst()) {
            do {
                val tryNumber = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRY_NUMBER))
                val objects = cursor.getString(cursor.getColumnIndexOrThrow("objects"))

                // Process object counts
                val objectCountMap = HashMap<String, Int>()
                val objectList = objects.split(",")
                for (obj in objectList) {
                    val trimmedObject = obj.trim()
                    objectCountMap[trimmedObject] = objectCountMap.getOrDefault(trimmedObject, 0) + 1
                }

                // Add to report data
                for ((objectName, count) in objectCountMap) {
                    reportData.add(ReportItem(tryNumber, objectName, count))
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        // Pass the report data to the report activity
        val intent = Intent(this, ReportActivity::class.java).apply {
            putParcelableArrayListExtra("report_data", ArrayList(reportData))
        }
        startActivity(intent)
    }

    data class ReportItem(val tryNumber: Int, val objectName: String, val objectCount: Int) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString() ?: "",
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(tryNumber)
            parcel.writeString(objectName)
            parcel.writeInt(objectCount)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<ReportItem> {
            override fun createFromParcel(parcel: Parcel): ReportItem {
                return ReportItem(parcel)
            }

            override fun newArray(size: Int): Array<ReportItem?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_UPDATE && resultCode == RESULT_OK) {
            showAllTries() // Refresh the list when returning from UpdateAttemptActivity
        }
    }

    companion object {
        private const val REQUEST_UPDATE = 1
    }
}
