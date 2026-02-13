package com.example.proyectoutilizandomlk;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements OnSuccessListener<Text>, OnFailureListener {

    private static final int REQUEST_CAMERA = 111;
    private static final int REQUEST_GALLERY = 222;

    private ImageView mImageView;
    private Bitmap mSelectedImage;
    private TextView txtResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.image_view);
        txtResults = findViewById(R.id.txtresults);

        // Permiso cámara
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top,
                    systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void abrirGaleria(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }


    public void abrirCamara(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }


    public void OCRfx(View v) {

        if (mSelectedImage == null) {
            txtResults.setText("Seleccione una imagen primero");
            return;
        }

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

        TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    @Override
    public void onSuccess(Text text) {

        List<Text.TextBlock> blocks = text.getTextBlocks();
        StringBuilder resultados = new StringBuilder();

        if (blocks.size() == 0) {
            resultados.append("No hay Texto");
        } else {
            for (Text.TextBlock block : blocks) {
                for (Text.Line line : block.getLines()) {
                    for (Text.Element element : line.getElements()) {
                        resultados.append(element.getText()).append(" ");
                    }
                }
                resultados.append("\n");
            }
        }

        txtResults.setText(resultados.toString());
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        txtResults.setText("Error al procesar imagen");
    }

    public void Rostrosfx(View v) {

        if (mSelectedImage == null) {
            txtResults.setText("Seleccione una imagen primero");
            return;
        }

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                mSelectedImage,
                mSelectedImage.getWidth() * 2,
                mSelectedImage.getHeight() * 2,
                true);

        InputImage image = InputImage.fromBitmap(scaledBitmap, 0);

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setMinFaceSize(0.05f)   // Mayor sensibilidad
                        .enableTracking()
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {

                    if (faces.size() == 0) {
                        txtResults.setText("No hay rostros");
                        return;
                    }

                    txtResults.setText("Hay " + faces.size() + " rostro(s)");

                    Bitmap mutableBitmap = scaledBitmap.copy(
                            Bitmap.Config.ARGB_8888, true);

                    Canvas canvas = new Canvas(mutableBitmap);

                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStrokeWidth(6);
                    paint.setStyle(Paint.Style.STROKE);

                    for (Face rostro : faces) {
                        canvas.drawRect(rostro.getBoundingBox(), paint);
                    }

                    mImageView.setImageBitmap(mutableBitmap);
                })
                .addOnFailureListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            try {

                if (requestCode == REQUEST_CAMERA && data.getExtras() != null) {
                    mSelectedImage =
                            (Bitmap) data.getExtras().get("data");
                }

                if (requestCode == REQUEST_GALLERY && data.getData() != null) {
                    mSelectedImage =
                            MediaStore.Images.Media.getBitmap(
                                    this.getContentResolver(),
                                    data.getData());
                }

                if (mSelectedImage != null) {
                    mImageView.setImageBitmap(mSelectedImage);
                }

            } catch (IOException e) {
                txtResults.setText("Error al cargar imagen");
            }
        }
    }
}
