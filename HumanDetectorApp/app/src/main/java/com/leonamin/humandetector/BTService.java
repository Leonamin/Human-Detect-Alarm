package com.leonamin.humandetector;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BTService extends Service {
    private static final String TAG = "HD/BTService";

    // member
    private UUID mDeviceUUID;
    private BluetoothSocket mBTSocket;
    private BluetoothDevice mDevice;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    private ProtocolParser mProtocolParser;

    private final List<Byte> carray = Collections.synchronizedList(new ArrayList<Byte>());

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_DISCONNECTED = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private final int MSG_UART_RECEIVE = 1;

    private final byte BT_CONNECT_EVENT = 0x01;
    private final byte DETECT_EVENT = 0x10;
    private final byte DETECT_PHOTO_EVENT = 0x11;

    public BTService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = intent.getExtras();
        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);
        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID));
        mProtocolParser = new ProtocolParser(this);

        Log.d(TAG, "Ready");

        mState = STATE_DISCONNECTED;

        start();

        return flags;
    }

    private void start() {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        connect();
    }

    private void connect() {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread();
        mConnectThread.start();
    }

    private void connected() {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread();
        mConnectedThread.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // TODO Send a failure message back to the Activity
        mState = STATE_NONE;

        start();
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void msg(int strId) {
        Toast.makeText(getApplicationContext(), strId, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SimpleDateFormat")
    private void createNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Human Detected!");
        builder.setContentText(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
        builder.setColor(Color.RED);
        builder.setAutoCancel(true);
        builder.setDefaults(Notification.DEFAULT_SOUND);
        builder.setOnlyAlertOnce(true);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));
        }

        notificationManager.notify(1, builder.build());
    }

    private class ConnectThread extends Thread {
        public ConnectThread() {
            mState = STATE_CONNECTING;
        }

        public void run() {
            // TODO Send ProgressDialog start signal with broadcast https://stackoverflow.com/questions/36535346

            try {
                if (mBTSocket == null) {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                }
            } catch (IOException e) {
                // TODO Process failed connecting
                // TODO Send destroying BTService signal and Caller Activity
//                msg(R.string.toast_bluetooth_disconnect);
            }

            // TODO Send ProgressDialog end signal with broadcast https://stackoverflow.com/questions/36535346

            mConnectedThread = null;
            Log.i(TAG, "Bluetooth connection completed!");
//            msg(R.string.toast_bluetooth_connect);
            connected();
        }

        public void cancel() {

        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream inputStream;

        public ConnectedThread() {
            mState = STATE_CONNECTED;
            InputStream tempIn = null;
            try {
                tempIn = mBTSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "bluetooth socket inputStream is not created!");
            }

            inputStream = tempIn;
        }

        public void run() {
            try {
                byte[] buffer = new byte[1024];
                Log.i(TAG, "Running bluetooth data reading...");
                int ch = 0;
                while (mState == STATE_CONNECTED) {
                    // TODO Sometimes data receiving is not end and it can't catch timeout and wrong start data
                    do {
                        ch = inputStream.read();
                        synchronized (carray) {
                            carray.add((byte) ch);
                            Message m = Message.obtain(mHandler, MSG_UART_RECEIVE);
                            mHandler.sendMessage(m);
                        }
                    } while (inputStream.available() > 0);
                }

            } catch (Exception e) {
                Log.e(TAG, "disconnected", e);
                connectionLost();
                e.printStackTrace();
            }
        }

        public void cancel() {

        }
    }

    @SuppressLint("HandlerLeak")
    public final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UART_RECEIVE:
                    synchronized (carray) {
                        while (!carray.isEmpty()) {
                            byte receivedByte;
                            receivedByte = carray.get(0);
                            carray.remove(0);
                            if (mProtocolParser.procDataReceive(receivedByte)) {
                                switch (mProtocolParser.getEventType()) {
                                    case BT_CONNECT_EVENT:
                                        Log.i(TAG, "BT Connected");
                                        break;
                                    case DETECT_EVENT:
                                        Log.i(TAG, "Human is detected");
                                        break;
                                    case DETECT_PHOTO_EVENT:
                                        Log.i(TAG, "Human is detected with photo");
                                        try {
                                            createImageFile(mProtocolParser.getData());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                }
                            }
                        }
                    }
                    break;
            }
        }
    };


    public static String byte2Hex(byte b) {
        return String.format(" 0x%02x ", b & 0xff);
    }

    public static String bytes2Ascii(byte[] bytes) {
        StringBuilder str = new StringBuilder();
        str.append("[len=").append(bytes.length).append("],");
        for (byte b : bytes) {
            str.append(byte2Hex(b));
        }
        return str.toString();
    }

    static String bytes2Ascii(byte[] bytes, int len) {
        StringBuilder str = new StringBuilder();
        str.append("[len=").append(len).append("],");
        for (int i = 0; i < len; i++) {
            str.append(byte2Hex(bytes[i]));
        }
        return str.toString();
    }

    public static String bytes2Ascii(List buf) {
        StringBuilder str = new StringBuilder();
        str.append("[len=").append(buf.size()).append("],");
        for (int i = 0; i < buf.size(); i++) {
            str.append(byte2Hex((byte) buf.get(i)));
        }
        return str.toString();
    }

    private void createImageFile(byte[] imageData) throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        FileOutputStream stream = new FileOutputStream(image.getAbsolutePath());
        stream.write(imageData);
        stream.close();
        Log.i(TAG, "Image file is created on " + image.getAbsolutePath());
    }
}