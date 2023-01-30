package com.nazar.textrecognizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RecognizerActivity extends AppCompatActivity {

    private Button btnSnap, btnConvert;
    private TextView tvResult, tvLocation, tvDistanceVal;
    private ImageView ivCapture;
    private Bitmap bmpImage;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    FusedLocationProviderClient fusedLocationProviderClient;

    private LocationManager locationManager;

    private static final DecimalFormat df = new DecimalFormat("0.00");

    private FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognizer);

        firebaseDatabase = FirebaseDatabase.getInstance();

        ivCapture = findViewById(R.id.ivCaptureImage);
        tvResult = findViewById(R.id.tvResult);
        tvLocation = findViewById(R.id.tvLocation);
        tvDistanceVal = findViewById(R.id.tvDistanceVal);
        btnSnap = findViewById(R.id.btnSnap);
        btnConvert = findViewById(R.id.btnConvert);

        // Location Services
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(RecognizerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RecognizerActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }

        btnSnap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPermitted()) {
                    getImage();
                    showLocation();
                }
                else
                    requestPermission();
            }
        });

        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                convertText();



            }
        });
    }


    private Double getDistance(Double latCurr, Double lonCurr){
        Double pi = 3.14159265358979323846;
        // Plaza Indonesia Lat and Long
        Double latDest = -6.1937536472129056;
        Double lonDest = 106.82196076339258;

        // User location
        Double latSrc = latCurr;
        Double lonSrc = lonCurr;

        Double R = 6371e3;

        Double latitudeRad1 = latDest * (pi / 180);
        Double latitudeRad2 = latSrc * (pi / 180);
        Double deltaLatRad = (latSrc - latDest) * (pi/180);
        Double deltaLonRad = (lonSrc - lonDest) * (pi/180);

        // Haversine form
        Double n = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) + Math.cos(latitudeRad1) * Math.cos(latitudeRad2) * Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        Double m = 2 * Math.atan2(Math.sqrt(n), Math.sqrt(1-n));
        Double s = R * m;

        return s;

    }
    private void showLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location != null) {
                    Geocoder geocoder = new Geocoder(RecognizerActivity.this, Locale.getDefault());
                    List<Address> addressList = null;
                    try {
                        addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String place = addressList.get(0).getAddressLine(0);
                    Log.d("nazar", "showLocation: " + place);

                    double distance = getDistance(location.getLatitude(), location.getLongitude());

                    tvLocation.setText(place);
                    df.setRoundingMode(RoundingMode.UP);
                    tvDistanceVal.setText(df.format(distance).toString() + " m | " + df.format(distance/1000).toString() +  " km");
                }
            }
        });
    }

    private boolean isPermitted() {
        if (
                (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        && (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        )
            return true;
        return false;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.CAMERA,
                }, 200);
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            boolean isGranted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            if (isGranted) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                getImage();
            } else
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            bmpImage = (Bitmap) extras.get("data");
            ivCapture.setImageBitmap(bmpImage);
        }
    }

    private void getImage(){
        Intent pic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(pic.resolveActivity(getPackageManager()) != null){
            startActivityForResult(pic, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void processText(Text result){
        String text = result.getText();
        StringBuilder stringBuilder = new StringBuilder();

        Log.d("nazar", "count : " + result.getTextBlocks().size());

        for(Text.TextBlock block : result.getTextBlocks()){
            String textBlock = block.getText();
            Log.d("nazar", "textBlock : " + textBlock);
            Point[] textBlockPoints = block.getCornerPoints();
            Rect textBlockFrame = block.getBoundingBox();
            for(Text.Line line: block.getLines()){
                String textLine = line.getText();
                Log.d("nazar", "textLine : " + textLine);
                Point[] textLinePoints = line.getCornerPoints();
                Rect textLineFrame = line.getBoundingBox();
                for(Text.Element element: line.getElements()){
                    String textElem = element.getText();
                    Log.d("nazar", "textElem : " + textElem);
                    Point[] textElemPoints = element.getCornerPoints();
                    Rect textElemFrame = element.getBoundingBox();
//                    for(Text.Symbol symbol: element.getSymbols()){
//                        String textSymbol = symbol.getText();
//                        Log.d("nazar", "textSymbol : " + textSymbol);
//                        Point[] textSymbolPoints = symbol.getCornerPoints();
//                        Rect textSymbolFrame = symbol.getBoundingBox();
//                    }
                    stringBuilder.append(textElem);
                    stringBuilder.append(" ");
                }
            }
        }
        tvResult.setText(stringBuilder.toString());
    }

    private void convertText(){
        InputImage inputImage = InputImage.fromBitmap(bmpImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result = recognizer.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        processText(visionText);

                        // upload to firebase
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("Text", tvResult.getText().toString());
                        map.put("Location", tvLocation.getText().toString());
                        map.put("Distance", tvDistanceVal.getText().toString());

                        firebaseDatabase.getReference()
                                .child("upload post")
                                .push()
                                .setValue(map)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(RecognizerActivity.this, "Upload Successfully", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(RecognizerActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RecognizerActivity.this, "Fail to Convert Text from Image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}