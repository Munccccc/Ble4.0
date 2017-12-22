

package bledocking.munc.app.ble;


import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

class BluetoothIBridgeConnectionThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothIBridgeDevice mmDevice;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final BluetoothIBridgeAdapter.MyHandler mHandler;
    private ArrayList<BluetoothIBridgeAdapter.DataReceiver> mDataReceivers;
    private static final int MAX_LEN = 65536;
    private byte[] buffer;
    private boolean isSocketReset = false;

    protected BluetoothIBridgeConnectionThread(BluetoothSocket socket, BluetoothIBridgeDevice device, BluetoothIBridgeAdapter.MyHandler handler, ArrayList<BluetoothIBridgeAdapter.DataReceiver> receivers) {
        this.mmSocket = socket;
        this.mmDevice = device;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.mHandler = handler;
        this.mDataReceivers = receivers;
        this.buffer = new byte[65536];

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException var8) {
            ;
        }

        this.mmInStream = tmpIn;
        this.mmOutStream = tmpOut;
        this.isSocketReset = false;
    }

    public void run() {
        byte[] buffer = new byte[1024];

        while(true) {
            try {
                int bytes = this.mmInStream.read(buffer);
                this.mmDevice.buffer = buffer;
                this.mmDevice.length = bytes;
                if(this.mDataReceivers != null) {
                    ArrayList e = (ArrayList)this.mDataReceivers.clone();
                    int numListeners = e.size();

                    for(int i = 0; i < numListeners; ++i) {
                        BluetoothIBridgeAdapter.DataReceiver er = (BluetoothIBridgeAdapter.DataReceiver)e.get(i);
                        if(this.mmDevice.isValidDevice() && er != null) {
                            er.onDataReceived(this.mmDevice, this.mmDevice.buffer, this.mmDevice.length);
                        }
                    }
                }
            } catch (IOException var7) {
                this.connectionLost(var7.getMessage());
                return;
            }
        }
    }

    private void connectionLost(String exceptionMsg) {
        if(!this.isSocketReset) {
            resetSocket(this.mmSocket);
        }

        if(this.mmDevice != null) {
            this.mmDevice.connected(false);
            this.mmDevice.setConnectStatus(BluetoothIBridgeDevice.ConnectStatus.STATUS_DISCONNECTED);
        }

        Message msg = this.mHandler.obtainMessage(7);
        msg.obj = this.mmDevice;
        Bundle bundle = new Bundle();
        bundle.putString("exception", exceptionMsg);
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }

    void write(byte[] buf, int length) {
        try {
            int e = Math.min(length, 1024);
            System.arraycopy(buf, 0, this.buffer, 0, e);
            this.mmOutStream.write(this.buffer, 0, length);
            this.mmOutStream.flush();
        } catch (IOException var6) {
            Message msg = this.mHandler.obtainMessage(11);
            msg.obj = this.mmDevice;
            Bundle bundle = new Bundle();
            bundle.putString("exception", var6.getMessage());
            msg.setData(bundle);
            this.mHandler.sendMessage(msg);
        }

    }

    void cancel() {
        this.isSocketReset = true;
        resetSocket(this.mmSocket);
    }

    public boolean equals(Object o) {
        if(o == null) {
            return false;
        } else if(!(o instanceof BluetoothIBridgeConnectionThread)) {
            return false;
        } else {
            BluetoothIBridgeConnectionThread conn = (BluetoothIBridgeConnectionThread)o;
            return conn.mmDevice.equals(this.mmDevice);
        }
    }

    BluetoothIBridgeDevice getDevice() {
        return this.mmDevice;
    }

    static void resetSocket(BluetoothSocket sock) {
        if(null != sock) {
            try {
                InputStream se = sock.getInputStream();
                if(null != se) {
                    se.close();
                }
            } catch (IOException var4) {
                var4.printStackTrace();
            }

            try {
                OutputStream se1 = sock.getOutputStream();
                if(null != se1) {
                    se1.close();
                }
            } catch (IOException var3) {
                var3.printStackTrace();
            }

            try {
                sock.close();
            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }

    }
}
