package com.leonamin.humandetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Controlling extends AppCompatActivity {
    private static final String TAG = "HD/Controlling";

    public static final String EVENT_TIMESTAMP = "HD.EVENT.TIMESTAMP";
    public static final String EVENT_IMAGE = "HD.EVENT.IMAGE";
    public static final String EVENT_THUMBNAIL = "HD.EVENT.THUMBNAIL";

    private Button logClearBtn;
    private Button exitBtn;
    private RecyclerView eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_controller);

        logClearBtn = findViewById(R.id.log_clear_btn);
        exitBtn = findViewById(R.id.exit_btn);
        eventList = findViewById(R.id.event_list);

        initList(new ArrayList<>());

        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter("EventServiceFilter")
        );

        logClearBtn.setOnClickListener((View v) -> {
            EventListAdapter adapter = (EventListAdapter) eventList.getAdapter();
            adapter.clearItems();
        });

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

    private void initList(List<EventData> objects) {
        final EventListAdapter adapter = new EventListAdapter(getApplicationContext(), objects);
        eventList.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        eventList.setLayoutManager(layoutManager);
        eventList.setItemAnimator(new DefaultItemAnimator());
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long timeStamp = intent.getLongExtra(EVENT_TIMESTAMP, 0);
            String image = intent.getStringExtra(EVENT_IMAGE);
            String thumbnail = intent.getStringExtra(EVENT_THUMBNAIL);

            EventListAdapter adapter = (EventListAdapter) eventList.getAdapter();
            adapter.addItem(new EventData(timeStamp, image, thumbnail));
        }
    };
}