package com.example.yolo_deploy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.programminghut.yolo_deploy.R
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.File
import java.io.FileOutputStream

class ReportActivity : AppCompatActivity() {

    private lateinit var reportTable: TableLayout
    private lateinit var downloadReportButton: Button
    private lateinit var reportData: ArrayList<AllTriesActivity.ReportItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        reportTable = findViewById(R.id.reportTable)
        downloadReportButton = findViewById(R.id.downloadReportButton)

        // Get the report data from the intent
        reportData = intent.getParcelableArrayListExtra("report_data") ?: arrayListOf()

        populateReport(reportData)

        // Set up download button
        downloadReportButton.setOnClickListener {
            checkAndRequestPermissions() // Check for permissions before generating the PDF
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE)
        } else {
            generatePDF()  // Generate the PDF if permission is granted
        }
    }

    private fun populateReport(reportData: List<AllTriesActivity.ReportItem>) {
        // Add headers
        val headerRow = TableRow(this)
        headerRow.addView(createTextView("Try Number"))
        headerRow.addView(createTextView("Detected Objects"))
        headerRow.addView(createTextView("Number of Objects"))
        reportTable.addView(headerRow)

        // Aggregate data by try number and object name
        val aggregatedData = mutableMapOf<Int, MutableMap<String, Int>>()

        for (item in reportData) {
            val tryNumber = item.tryNumber
            val objectName = item.objectName
            val objectCount = item.objectCount

            if (aggregatedData[tryNumber] == null) {
                aggregatedData[tryNumber] = mutableMapOf()
            }

            val currentCount = aggregatedData[tryNumber]?.get(objectName) ?: 0
            aggregatedData[tryNumber]?.put(objectName, currentCount + objectCount)
        }

        // Populate the table
        for ((tryNumber, objects) in aggregatedData) {
            // Add a row for each object under the same try number
            var isFirstRow = true
            for ((objectName, count) in objects) {
                val row = TableRow(this)

                if (isFirstRow) {
                    row.addView(createTextView(tryNumber.toString()))
                    isFirstRow = false
                } else {
                    row.addView(createTextView("")) // Empty view for spacing
                }

                row.addView(createTextView(objectName))
                row.addView(createTextView(count.toString()))

                reportTable.addView(row)
            }
        }
    }

    private fun createTextView(text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(16, 16, 16, 16) // Add padding for better appearance
        return textView
    }

    private fun generatePDF() {
        val filePath = "${Environment.getExternalStorageDirectory()}/Download/report.pdf" // Save path
        val pdfWriter = PdfWriter(FileOutputStream(filePath))
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        // Add content to the PDF
        document.add(Paragraph("Report").setBold().setFontSize(18f))
        document.add(Paragraph("\n")) // Add some space

        // Add table headers
        val table = com.itextpdf.layout.element.Table(floatArrayOf(1f, 2f, 2f)) // Define column widths
        table.addCell("Try Number")
        table.addCell("Detected Objects")
        table.addCell("Number of Objects")

        // Track the last try number to avoid duplication
        var lastTryNumber = -1

        for (item in reportData) {
            // Check if it's a new try number
            if (item.tryNumber != lastTryNumber) {
                table.addCell(item.tryNumber.toString())
                lastTryNumber = item.tryNumber
            } else {
                table.addCell("") // Empty cell for spacing
            }

            // Add detected object and count
            table.addCell(item.objectName)
            table.addCell(item.objectCount.toString())
        }

        document.add(table) // Add the table to the document
        document.close()

        Toast.makeText(this, "Report downloaded: $filePath", Toast.LENGTH_LONG).show()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePDF() // Generate the PDF if permission is granted
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_WRITE_STORAGE = 1
    }
}
