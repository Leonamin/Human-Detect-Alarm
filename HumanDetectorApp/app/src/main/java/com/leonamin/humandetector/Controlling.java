package com.leonamin.humandetector;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Controlling extends AppCompatActivity {
    private static final String TAG = "HD/Controlling";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_controller);

        ActivityHelper.initialize(this);
        // mBtnDisconnect = (Button) findViewById(R.id.btnDisconnect);

        Intent intent = getIntent();
        Bundle b = intent.getExtras();

        intent = new Intent(getApplicationContext(), BTService.class);
        intent.putExtras(b);
        getApplicationContext().startService(intent);
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void msg(int strId) {
        Toast.makeText(getApplicationContext(), strId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Resumed");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopped");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}