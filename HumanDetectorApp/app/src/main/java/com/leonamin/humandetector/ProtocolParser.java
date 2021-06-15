package com.leonamin.humandetector;

import android.content.Context;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProtocolParser {
    private final String TAG = "HD/ProtocolParser";

    private final byte BT_CONNECT_EVENT = 0x01;
    private final byte DETECT_EVENT = 0x10;
    private final byte DETECT_PHOTO_EVENT = 0x11;

    private final int MSG_DATA_PARSE_COMPLETED = 2;

    private Context mContext;

    private int headerReceiveCnt = 0;
    private int dataReceiveCnt = 0;

    class Protocol {
        private byte EVENT_TYPE;
        private int LENGTH;
        private final List<Byte> receiveData;

        Protocol() {
            EVENT_TYPE = 0x00;
            LENGTH = 0;
            receiveData = Collections.synchronizedList(new ArrayList<Byte>());
        }
    }

    public byte getEventType() {
        return completeData.EVENT_TYPE;
    }

    public int getLength() {
        return completeData.LENGTH;
    }

    public List<Byte> getData() {
        return completeData.receiveData;
    }

    private Protocol mProtocol;
    private Protocol completeData;

    ProtocolParser(Context context) {
        mContext = context;
        mProtocol = new Protocol();
        completeData = new Protocol();
    }

    /**
     * Save received data and parse that
     * @param data
     * 1 Byte data
     * @return
     * if parsing ended, return true
     */
    public boolean procDataReceive(byte data) {
        if (mProtocol.EVENT_TYPE == 0x00) {
            switch (data) {
                case BT_CONNECT_EVENT:
                case DETECT_EVENT:
                case DETECT_PHOTO_EVENT:
                    Log.i(TAG, "Event Type = " + data);
                    headerReceiveCnt++;
                    mProtocol.EVENT_TYPE = data;
                    return false;
                default:
                    Log.e(TAG, "unknown packet event type = " + ((int) data & 0xff));
                    return false;
            }
        }

        if (headerReceiveCnt < 5) {
            mProtocol.LENGTH = (mProtocol.LENGTH << 8) + (data & 0xff);
            headerReceiveCnt++;
            if (headerReceiveCnt == 5) {
                Log.i(TAG, "Data size = " + mProtocol.LENGTH);
            }
            return false;
        }

        mProtocol.receiveData.add(data);
        dataReceiveCnt++;
//        Log.i(TAG, "Data receive size = " + dataReceiveCnt);
        if (dataReceiveCnt == mProtocol.LENGTH) {
            completeData.receiveData.clear();
            completeData = new Protocol();
            completeData.EVENT_TYPE = mProtocol.EVENT_TYPE;
            completeData.LENGTH = mProtocol.LENGTH;
            completeData.receiveData.addAll(mProtocol.receiveData);
            mProtocol.receiveData.clear();
            mProtocol = new Protocol(); // clear
            headerReceiveCnt = 0;
            dataReceiveCnt = 0;

            return true;
        }

        return false;
    }
}
