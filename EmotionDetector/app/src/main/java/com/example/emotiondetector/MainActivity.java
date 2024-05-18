package com.example.emotiondetector;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emotiondetector.ml.ModelUnquant;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button upload_btn, analyse_btn;
    private Bitmap img;
    private TextView tv;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        upload_btn = findViewById(R.id.upload_btn);
        analyse_btn = findViewById(R.id.analyse_btn);
        tv = findViewById(R.id.tv);

        upload_btn.setOnClickListener(new SelectButtonClickListener());
        analyse_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (img != null) {
                    try {
                        ModelUnquant model = ModelUnquant.newInstance(getApplicationContext());

                        Bitmap resizedImage = Bitmap.createScaledBitmap(img, 224, 224, true);

                        // Convert Bitmap to ByteBuffer
                        ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedImage);

                        // Creates inputs for reference.
                        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                        inputFeature0.loadBuffer(byteBuffer);

                        // Runs model inference and gets result.
                        ModelUnquant.Outputs outputs = model.process(inputFeature0);
                        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                        // Releases model resources if no longer used.
                        model.close();

                        // Define emotion labels
                        String[] emotions = {"angry", "disgusted", "fearful", "happy", "neutral", "sad", "surprised"};

                        // Get the output probabilities
                        float[] probabilities = outputFeature0.getFloatArray();

                        // Find the index of the highest probability
                        int maxIndex = 0;
                        float maxProbability = probabilities[0];
                        for (int i = 1; i < probabilities.length; i++) {
                            if (probabilities[i] > maxProbability) {
                                maxProbability = probabilities[i];
                                maxIndex = i;
                            }
                        }

                        // Display the corresponding emotion label
                        tv.setText(maxIndex + " " + emotions[maxIndex]);
                    } catch (IOException e) {
                        e.printStackTrace();
                        tv.setText("Error: " + e.getMessage());
                    }
                } else {
                    tv.setText("Please upload an image first.");
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                imageView.setImageURI(uri);
                try {
                    img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class SelectButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }
        return byteBuffer;
    }
}
