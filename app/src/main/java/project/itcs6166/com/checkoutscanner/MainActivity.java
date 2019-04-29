package project.itcs6166.com.checkoutscanner;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CAMERA = 0x000000;
    private static final String TAG = "CheckOutActivity";
    SurfaceView surfaceView;
    CameraSource cameraSource;
    BarcodeDetector barcodeDetector;
    private RequestQueue requestQueue;
    private String url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.cameraView);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = "http://172.20.10.4:3000/postdata";
        requestQueue.start();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity
                            .this,
                    Manifest
                            .permission
                            .CAMERA)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity
                                .this,
                        new String[]{Manifest.permission
                                .CAMERA},
                        CAMERA_PERMISSION_CAMERA);

                // CAMERA_PERMISSION_CAMERA is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setRequestedPreviewSize(640, 230)
                .setAutoFocusEnabled(true)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15.0f)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                try {
                    cameraSource.start(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCode = detections.getDetectedItems();
                if(qrCode.size() > 0){
                    //barcodeDetector.release();
                    Vibrator vibrator = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    assert vibrator != null;
                    vibrator.vibrate(500);
                    String value = qrCode.valueAt(0).displayValue;
                    String[] parts = value.split(",");
                    String label = parts[0];
                  //  Date date = null;
                   // String exp_date = null;
                    try {
                        Log.i(TAG, "exp_date: "+ parts[1]);
                        Date date  = new SimpleDateFormat("MM/dd/yyyy").parse(parts[1]);
                        Log.i(TAG, "date: "+ date.toString());
                        String exp_date = DateFormat.getDateInstance().format(date);
                        Log.i(TAG, "exp_date: "+ exp_date);
                        sendPostRequest(label, exp_date);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    //Log.i(TAG, "value: "+ value);
                    Message msg = handler.obtainMessage();
                    msg.obj = value;
                    handler.sendMessage(msg);

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }

        });
    }

    private void sendPostRequest(String label, String expDate) {
    final Map<String, String> body = new HashMap<>();
    body.put("label",label);
    body.put("exp_date", expDate);
        Log.i(TAG, "body : "+ body);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(body),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                            Log.i(TAG, "response: "+response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error While sending data: "+error.getMessage());
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if(msg.obj != null)
                Toast.makeText(getApplicationContext(),(String)msg.obj, Toast.LENGTH_SHORT).show();
        }
    };
}
