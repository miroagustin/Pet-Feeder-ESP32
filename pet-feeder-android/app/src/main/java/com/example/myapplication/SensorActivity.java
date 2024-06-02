package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SensorActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;

    TextView label_shake;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_sensor); // Carga la pantalla

        label_shake = findViewById(R.id.txt_shake);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorEventListener = new SensorEventListener() {
            private static final float SHAKE_THRESHOLD = 500f; // Puedes ajustar este valor según tu necesidad
            private long lastUpdate = 0;
            private float lastX, lastY, lastZ;

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    long currentTime = System.currentTimeMillis();
                    if ((currentTime - lastUpdate) > 100) {
                        long timeDifference = (currentTime - lastUpdate);
                        lastUpdate = currentTime;

                        float x = event.values[0];
                        float y = event.values[1];
                        float z = event.values[2];

                        float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / timeDifference * 10000;

                        if (speed > SHAKE_THRESHOLD) {
                            System.out.println("SE DETECTO SHAKE!");
                            label_shake.setVisibility(View.VISIBLE);
                        }

                        lastX = x;
                        lastY = y;
                        lastZ = z;
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // No necesitas implementar esto, pero es obligatorio
            }
        };

    }
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    public void changeActivityMain(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void resetText(View view){
        label_shake.setVisibility(View.INVISIBLE);
    }
}
