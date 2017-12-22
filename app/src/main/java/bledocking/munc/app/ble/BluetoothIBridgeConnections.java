
package bledocking.munc.app.ble;


import android.bluetooth.BluetoothSocket;
import android.os.Message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class BluetoothIBridgeConnections {
    private BluetoothIBridgeAdapter.MyHandler mHandler;
    private ArrayList<BluetoothIBridgeAdapter.DataReceiver> mDataReceivers;
    private BluetoothIBridgeConnections.ConnectionList mList;

    protected BluetoothIBridgeConnections(BluetoothIBridgeAdapter.MyHandler handler) {
        this.mHandler = handler;
        this.mList = new BluetoothIBridgeConnections.ConnectionList();
        this.mList.clear();
    }

    void registerDataReceiver(BluetoothIBridgeAdapter.DataReceiver receiver) {
        if(this.mDataReceivers == null) {
            this.mDataReceivers = new ArrayList();
        }

        if(!this.mDataReceivers.contains(receiver)) {
            this.mDataReceivers.add(receiver);
        }

    }

    void unregisterDataReceiver(BluetoothIBridgeAdapter.DataReceiver receiver) {
        if(this.mDataReceivers != null) {
            this.mDataReceivers.remove(receiver);
        }
    }

    void disconnect(BluetoothIBridgeDevice device) {
        BluetoothIBridgeConnectionThread found = this.mList.foundDevice(device);
        if(found != null) {
            if(device != null) {
                device.setConnectStatus(BluetoothIBridgeDevice.ConnectStatus.STATUS_DISCONNECTTING);
            }

            found.cancel();
        }
    }

    void write(BluetoothIBridgeDevice device, byte[] buffer, int length) {
        this.mList.write(device, buffer, length);
    }

    List<BluetoothIBridgeDevice> getCurrentConnectedDevice() {
        return this.mList.getCurrentConnectedDevice();
    }

    void connected(BluetoothSocket socket, BluetoothIBridgeDevice device) {
        BluetoothIBridgeConnectionThread conn = new BluetoothIBridgeConnectionThread(socket, device, this.mHandler, this.mDataReceivers);
        conn.start();
        this.mList.addConnection(conn);
        if(device != null) {
            device.connected(true);
            device.setConnectStatus(BluetoothIBridgeDevice.ConnectStatus.STATUS_CONNECTED);
        }

        Message msg = this.mHandler.obtainMessage(6);
        msg.obj = device;
        this.mHandler.sendMessage(msg);
    }

    void disconnectAll() {
        this.mList.releaseAllConnections();
    }

    private class ConnectionList {
        private List<BluetoothIBridgeConnectionThread> mConnectedDevices;
        private byte[] LOCK;

        private ConnectionList() {
            this.mConnectedDevices = new ArrayList();
            this.LOCK = new byte[0];
        }

        public void write(BluetoothIBridgeDevice device, byte[] buffer, int length) {
            if(null != device && null != buffer && length > 0) {
                BluetoothIBridgeConnectionThread found = this.foundDevice(device);
                if(null != found) {
                    found.write(buffer, length);
                }

            }
        }

        public void addConnection(BluetoothIBridgeConnectionThread connection) {
            BluetoothIBridgeConnectionThread found = this.foundDevice(connection.getDevice());
            byte[] var3;
            if(found != null) {
                var3 = this.LOCK;
                synchronized(this.LOCK) {
                    this.mConnectedDevices.remove(found);
                }
            }

            var3 = this.LOCK;
            synchronized(this.LOCK) {
                this.mConnectedDevices.add(connection);
            }
        }

        private BluetoothIBridgeConnectionThread foundDevice(BluetoothIBridgeDevice device) {
            BluetoothIBridgeConnectionThread found = null;
            byte[] var3 = this.LOCK;
            synchronized(this.LOCK) {
                Iterator i$ = this.mConnectedDevices.iterator();

                while(i$.hasNext()) {
                    BluetoothIBridgeConnectionThread ds = (BluetoothIBridgeConnectionThread)i$.next();
                    if(device.equals(ds.getDevice())) {
                        found = ds;
                        break;
                    }
                }

                return found;
            }
        }

        void clear() {
            byte[] var1 = this.LOCK;
            synchronized(this.LOCK) {
                this.mConnectedDevices.clear();
            }
        }

        List<BluetoothIBridgeDevice> getCurrentConnectedDevice() {
            ArrayList devicesList = new ArrayList();
            byte[] var2 = this.LOCK;
            synchronized(this.LOCK) {
                Iterator i$ = this.mConnectedDevices.iterator();

                while(i$.hasNext()) {
                    BluetoothIBridgeConnectionThread ds = (BluetoothIBridgeConnectionThread)i$.next();
                    BluetoothIBridgeDevice device = ds.getDevice();
                    if(device != null && !devicesList.contains(device)) {
                        devicesList.add(device);
                    }
                }

                return devicesList;
            }
        }

        void releaseAllConnections() {
            byte[] var1 = this.LOCK;
            synchronized(this.LOCK) {
                Iterator i$ = this.mConnectedDevices.iterator();

                while(true) {
                    if(!i$.hasNext()) {
                        break;
                    }

                    BluetoothIBridgeConnectionThread ds = (BluetoothIBridgeConnectionThread)i$.next();
                    if(ds != null) {
                        ds.cancel();
                    }
                }
            }

            this.mConnectedDevices.clear();
        }
    }
}
