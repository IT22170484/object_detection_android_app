package com.example.yolo_deploy

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "predictions.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "predictions"
        private const val COLUMN_ID = "_id"
        const val COLUMN_TRY_NUMBER = "try_number"
        const val COLUMN_OBJECT_NAME = "object_name"
    }

    private val TABLE_CREATE = "CREATE TABLE $TABLE_NAME (" +
            "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_TRY_NUMBER INTEGER, " +
            "$COLUMN_OBJECT_NAME TEXT" +
            ");"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TABLE_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addPrediction(tryNumber: Int, objectName: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TRY_NUMBER, tryNumber)
            put(COLUMN_OBJECT_NAME, objectName)
        }
        db.insert(TABLE_NAME, null, values)
    }

    fun getPredictionsByTryNumber(tryNumber: Int): Cursor {
        val db = readableDatabase
        return db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_OBJECT_NAME), // Retrieve only needed columns
            "$COLUMN_TRY_NUMBER = ?",
            arrayOf(tryNumber.toString()), // Convert Int to String for query parameter
            null,
            null,
            null
        )
    }


    fun getAllPredictionsGroupedByTryNumber(): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            "SELECT $COLUMN_ID, $COLUMN_TRY_NUMBER, GROUP_CONCAT($COLUMN_OBJECT_NAME) AS objects " +
                    "FROM $TABLE_NAME " +
                    "GROUP BY $COLUMN_TRY_NUMBER",
            null
        )
    }

    fun getAllPredictions(): Cursor {
        val db = readableDatabase
        return db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_TRY_NUMBER, COLUMN_OBJECT_NAME), // Include _id here
            null,
            null,
            null,
            null,
            null
        )
    }
    fun clearPredictionsForTryNumber(tryNumber: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_TRY_NUMBER = ?", arrayOf(tryNumber.toString()))
    }
    fun deletePrediction(tryNumber: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_TRY_NUMBER = ?", arrayOf(tryNumber))
    }
    fun updatePrediction(tryNumber: String, newObject: String): Int {
        val db = writableDatabase

        // Delete old entries for the given try number
        db.delete(TABLE_NAME, "$COLUMN_TRY_NUMBER = ?", arrayOf(tryNumber))

        // Insert the new object for the given try number
        val values = ContentValues().apply {
            put(COLUMN_TRY_NUMBER, tryNumber)
            put(COLUMN_OBJECT_NAME, newObject)
        }

        // Insert new entry
        val newRowId = db.insert(TABLE_NAME, null, values)

        return if (newRowId != -1L) 1 else 0 // Return 1 if insertion was successful
    }




}
