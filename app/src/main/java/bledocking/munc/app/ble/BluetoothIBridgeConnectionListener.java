
package bledocking.munc.app.ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

final class BluetoothIBridgeConnectionListener {
    private static final boolean D = true;
    private static final String TAG = "BluetoothIBridgeConnectionListener";
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothIBridgeConnectionListener.ConnectionReceiver mReceiver;
    private BluetoothIBridgeConnectionListener.AcceptThread mThread;
    private boolean mAuthenticated;

    protected BluetoothIBridgeConnectionListener(BluetoothIBridgeConnectionListener.ConnectionReceiver receiver, boolean auth) {
        this.mReceiver = receiver;
        this.mAuthenticated = auth;
    }

    void start() {
        if(this.mThread != null) {
            this.mThread.cancel();
        }

        this.mThread = new BluetoothIBridgeConnectionListener.AcceptThread();
        this.mThread.start();
    }

    void stop() {
        if(this.mThread != null) {
            this.mThread.cancel();
        }

    }

    void setLinkKeyNeedAuthenticated(boolean authenticated) {
        if(this.mAuthenticated != authenticated) {
            this.mAuthenticated = authenticated;
            this.stop();
            this.start();
        }

    }

    private class AcceptThread extends Thread {
        private static final String SERVICE_NAME = "IVT-IBridge";
        private final BluetoothServerSocket mmServerSocket;
        private volatile boolean running;

        protected AcceptThread() {
            BluetoothServerSocket tmp = null;
            this.running = true;

            try {
                if(!BluetoothIBridgeConnectionListener.this.mAuthenticated && Build.VERSION.SDK_INT >= 10) {
                    tmp = this.listenUsingInsecureRfcommWithServiceRecord("IVT-IBridge", BluetoothIBridgeDevice.SPPUUID);
                    Log.i("ConnListener", "insecure rfcomm " + tmp);
                } else {
                    tmp = BluetoothIBridgeConnectionListener.this.mAdapter.listenUsingRfcommWithServiceRecord("IVT-IBridge", BluetoothIBridgeDevice.SPPUUID);
                    Log.i("ConnListener", "secure rfcomm " + tmp);
                }
            } catch (IOException var4) {
                Log.e("ConnListener", "Connection listen failed", var4);
            }

            this.mmServerSocket = tmp;
        }

        public void run() {
            this.setName("AcceptThread");
            BluetoothSocket socket = null;

            while(this.running) {
                try {
                    if(this.mmServerSocket == null) {
                        return;
                    }

                    socket = this.mmServerSocket.accept();
                } catch (IOException var3) {
                    Log.i("ConnListener", "accept failed");
                    return;
                }

                if(socket != null && BluetoothIBridgeConnectionListener.this.mReceiver != null) {
                    BluetoothIBridgeConnectionListener.this.mReceiver.onConnectionEstablished(socket);
                }
            }

        }

        void cancel() {
            try {
                if(null != this.mmServerSocket) {
                    this.mmServerSocket.close();
                }
            } catch (IOException var2) {
                ;
            }

        }

        private BluetoothServerSocket listenUsingInsecureRfcommWithServiceRecord(String serviceName, UUID serviceUUID) {
            BluetoothServerSocket socket = null;

            try {
                Class e = BluetoothAdapter.class;
                Method m = e.getMethod("listenUsingInsecureRfcommWithServiceRecord", new Class[]{String.class, UUID.class});
                socket = (BluetoothServerSocket)m.invoke(BluetoothIBridgeConnectionListener.this.mAdapter, new Object[]{serviceName, serviceUUID});
            } catch (NoSuchMethodException var6) {
                var6.printStackTrace();
            } catch (IllegalArgumentException var7) {
                var7.printStackTrace();
            } catch (IllegalAccessException var8) {
                var8.printStackTrace();
            } catch (InvocationTargetException var9) {
                var9.printStackTrace();
            }

            return socket;
        }
    }

    protected interface ConnectionReceiver {
        void onConnectionEstablished(BluetoothSocket var1);
    }
}
