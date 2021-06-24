package com.leonamin.humandetector;

import android.content.Context;
import android.util.Log;

public class ProtocolParser {
    private final String TAG = "HD/ProtocolParser";

    private final int STX = 0x5A;
    private final int ETX = 0xA5;
    private final byte BT_CONNECT_EVENT = 0x01;
    private final byte DETECT_EVENT = 0x10;
    private final byte DETECT_PHOTO_EVENT = 0x11;

    private final int MSG_DATA_PARSE_COMPLETED = 2;

    private Context mContext;

    private int headerReceiveCnt = 0;
    private int dataReceiveCnt = 0;

    class Protocol {
        private int STX;
        private byte EVENT_TYPE;
        private int LENGTH;
        private int ETX;
        private byte[] receiveData;

        Protocol() {
            STX = 0x00;
            EVENT_TYPE = 0x00;
            LENGTH = 0;
            ETX = 0x00;
        }
    }

    public byte getEventType() {
        return completeData.EVENT_TYPE;
    }

    public int getLength() {
        return completeData.LENGTH;
    }

    public byte[] getData() {
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
        if (mProtocol.STX != STX) {
            if (STX == ((int) data & 0xff)) {
                Log.i(TAG, "STX");
                mProtocol.STX = ((int) data & 0xff);
                return false;
            }
        }
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
                try {
                    mProtocol.receiveData = new byte[mProtocol.LENGTH];
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory");
                    mProtocol = new Protocol(); // clear
                    headerReceiveCnt = 0;
                } catch (Exception e) {
                    Log.e(TAG, "Wrong data size");
                    mProtocol = new Protocol(); // clear
                    headerReceiveCnt = 0;
                }
            }
            return false;
        }
        if (mProtocol.ETX != ETX) {
            if (ETX == ((int) data & 0xff)) {
                Log.i(TAG, "ETX");
                mProtocol.ETX = ((int) data & 0xff);
                return false;
            }
        }
        mProtocol.receiveData[dataReceiveCnt++] = data;
        if (dataReceiveCnt == mProtocol.LENGTH) {
            completeData = new Protocol();
            completeData.EVENT_TYPE = mProtocol.EVENT_TYPE;
            completeData.LENGTH = mProtocol.LENGTH;
            completeData.receiveData = mProtocol.receiveData.clone();
            mProtocol = new Protocol(); // clear
            headerReceiveCnt = 0;
            dataReceiveCnt = 0;

            return true;
        }

        return false;
    }

    public void clearDataReceive() {
        mProtocol = new Protocol();
        headerReceiveCnt = 0;
        dataReceiveCnt = 0;
    }
}
