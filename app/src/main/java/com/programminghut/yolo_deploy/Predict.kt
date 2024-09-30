package com.programminghut.yolo_deploy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.yolo_deploy.DatabaseHelper;
import com.example.yolo_deploy.ObjectsActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Predict extends AppCompatActivity {

    private final int IMAGE_PICK = 100;
    private ImageView imageView;
    private Bitmap bitmap;
    private Yolov5TFLiteDetector yolov5TFLiteDetector;
    private Paint boxPaint = new Paint();
    private Paint textPaint = new Paint();
    private Button predictButton;
    private Button objectsButton;
    private DatabaseHelper databaseHelper;
    private ArrayList<Recognition> recognitions = new ArrayList<>();
    private int tryNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);

        imageView = findViewById(R.id.imageView);
        predictButton = findViewById(R.id.predict);
        objectsButton = findViewById(R.id.objects);
        databaseHelper = new DatabaseHelper(this);

        yolov5TFLiteDetector = new Yolov5TFLiteDetector();
        yolov5TFLiteDetector.setModelFile("yolov5s-fp16.tflite");
        yolov5TFLiteDetector.initialModel(this);

        boxPaint.setStrokeWidth(5);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.RED);

        textPaint.setTextSize(50);
        textPaint.setColor(Color.GREEN);
        textPaint.setStyle(Paint.Style.FILL);

        // Set the try number to the latest one in the database + 1
        tryNumber = getNextTryNumber();

        predictButton.setOnClickListener(v -> predict());
        objectsButton.setOnClickListener(v -> showObjects());

        // Set fixed dimensions for the ImageView
        imageView.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.image_view_width);
        imageView.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.image_view_height);
        imageView.requestLayout();
    }

    public void selectImage(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK);
        // Reset tryNumber for the new image selection
        tryNumber = getNextTryNumber(); // This will set a new try number for each image selection
    }

    public void predict() {
        if (bitmap != null) {
            // Resize the bitmap to a fixed size (e.g., 800x600 pixels)
            Bitmap resizedBitmap = resizeBitmap(bitmap, 800, 600);

            // Create a mutable bitmap from the resized bitmap
            Bitmap mutableBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);

            // Clear previous drawings by filling the canvas with the resized bitmap
            canvas.drawBitmap(resizedBitmap, 0, 0, null);

            // Perform object detection
            recognitions = yolov5TFLiteDetector.detect(resizedBitmap);

            // Clear previous results from the database for the current tryNumber
            databaseHelper.clearPredictionsForTryNumber(tryNumber);

            // Create a map to count occurrences of each object
            HashMap<String, Integer> objectCountMap = new HashMap<>();

            // Draw rectangles and text on the mutable bitmap
            for (Recognition recognition : recognitions) {
                if (recognition.getConfidence() > 0.4) {
                    RectF location = recognition.getLocation();
                    canvas.drawRect(location, boxPaint);
                    canvas.drawText(recognition.getLabelName() + ":" + recognition.getConfidence(), location.left, location.top, textPaint);

                    // Count occurrences of each object
                    String labelName = recognition.getLabelName();
                    objectCountMap.put(labelName, objectCountMap.getOrDefault(labelName, 0) + 1);
                }
            }

            // Save counted predictions to the database
            for (Map.Entry<String, Integer> entry : objectCountMap.entrySet()) {
                String objectName = entry.getKey();
                int count = entry.getValue();
                databaseHelper.addPrediction(tryNumber, count + " " + objectName); // Save as "3 books"
            }

            // Update the ImageView with the mutable bitmap
            imageView.setImageBitmap(mutableBitmap);

            // Check if the image is set correctly
            if (imageView.getDrawable() == null) {
                Log.e("Predict", "Image is not set in the ImageView.");
            }

            objectsButton.setVisibility(View.VISIBLE); // Show the button after prediction
        } else {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
        }
    }


    private int getNextTryNumber() {
        Cursor cursor = databaseHelper.getAllPredictions();
        int lastTryNumber = 0;

        // Check if the cursor has at least one entry and the column exists
        if (cursor.moveToLast()) {
            int tryNumberIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRY_NUMBER);

            // Ensure the index is valid
            if (tryNumberIndex != -1) {
                lastTryNumber = cursor.getInt(tryNumberIndex);
            } else {
                // Handle the error: column does not exist
                throw new IllegalStateException("COLUMN_TRY_NUMBER not found in the Cursor.");
            }
        }

        cursor.close();
        return lastTryNumber + 1; // Increment for the next try
    }

    private void saveRecognitionToDatabase(String objectName) {
        databaseHelper.addPrediction(tryNumber, objectName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                // Resize the bitmap to a fixed size (e.g., 800x600 pixels)
                bitmap = resizeBitmap(bitmap, 800, 600);

                imageView.setImageBitmap(bitmap); // Display the selected image
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap originalBitmap, int width, int height) {
        return Bitmap.createScaledBitmap(originalBitmap, width, height, true);
    }

    private void showObjects() {
        Intent intent = new Intent(this, ObjectsActivity.class);
        intent.putExtra("TRY_NUMBER", tryNumber); // Pass the try number to ObjectsActivity
        startActivity(intent);
    }

}
