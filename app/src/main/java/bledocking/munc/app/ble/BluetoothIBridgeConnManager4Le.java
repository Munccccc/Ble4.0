

package bledocking.munc.app.ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

final class BluetoothIBridgeConnManager4Le {
    private int mMtu = 20;
    private int credit = 0;
    private BluetoothIBridgeAdapter.MyHandler mHandler;
    private BluetoothManager mBluetoothManager;
    private ArrayList<BluetoothIBridgeAdapter.DataReceiver> mDataReceivers;
    private BluetoothIBridgeConnManager4Le.ConnectionList mList;
    private Context mContext;

    public BluetoothIBridgeConnManager4Le(Context context, BluetoothIBridgeAdapter.MyHandler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mList = new BluetoothIBridgeConnManager4Le.ConnectionList();
        this.mList.clear();
        if (this.mBluetoothManager == null) {
            this.mBluetoothManager = (BluetoothManager) context.getSystemService("bluetooth");
            if (this.mBluetoothManager == null) {
                Log.e("IBridgeGatt", "no bluetooth manager");
            }
        }

    }

    void registerDataReceiver(BluetoothIBridgeAdapter.DataReceiver receiver) {
        if (this.mDataReceivers == null) {
            this.mDataReceivers = new ArrayList();
        }

        if (!this.mDataReceivers.contains(receiver)) {
            this.mDataReceivers.add(receiver);
        }

    }

    void unregisterDataReceiver(BluetoothIBridgeAdapter.DataReceiver receiver) {
        if (this.mDataReceivers != null) {
            this.mDataReceivers.remove(receiver);
        }
    }

    void destory() {
        this.mList.releaseAllConnections();
        this.mHandler = null;
        this.mDataReceivers = null;
    }

    void connect(BluetoothIBridgeDevice device) {
        if (device != null && !device.isConnected()) {
            device.setBondStatus(BluetoothIBridgeDevice.BondStatus.STATE_BONDED);
            device.setConnectStatus(BluetoothIBridgeDevice.ConnectStatus.STATUS_CONNECTTING);
            new BluetoothIBridgeConnManager4Le.GattConnection(this.mContext, device, this.mHandler, this.mDataReceivers);
        } else {
            Log.e("ConnManager", "device is connected or is null");
        }

    }

    void disconnect(BluetoothIBridgeDevice device) {
        BluetoothIBridgeConnManager4Le.GattConnection found = this.mList.foundDevice(device);
        Log.i("IBridgeGatt", "try to release gatt connection:" + found);
        if (found == null) {
            Log.e("IBridgeGatt", "The gatt device[" + device + "] may has been closed.");
        } else {
            found.disconnnect();
        }
    }

    void write(BluetoothIBridgeDevice device, byte[] buffer, int length) {
        this.mList.write(device, buffer, length);
    }

    void disconnectAll() {
        this.mList.releaseAllConnections();
    }

    public List<BluetoothIBridgeDevice> getCurrentConnectedDevice() {
        return this.mList.getCurrentConnectedDevice();
    }

    public void setTargetUUIDs(BluetoothIBridgeDevice device, String serviceUUID, String notifyCharacteristicUUID, String writeCharacteristicUUID) {
        BluetoothIBridgeConnManager4Le.GattConnection found = this.mList.foundDevice(device);
        if (found != null) {
            found.setTargetUUIDs(serviceUUID, notifyCharacteristicUUID, writeCharacteristicUUID);
        }

    }

    public void setMtu(BluetoothIBridgeDevice device, int mtu) {
        BluetoothIBridgeConnManager4Le.GattConnection found = this.mList.foundDevice(device);
        if (found != null) {
            found.setMtu(mtu);
        }

    }

    class GattConnection {
        static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
        String mGattServiceUUID = "0000ff00-0000-1000-8000-00805f9b34fb";
        String mNotifyCharacteristicUUID = "0000ff01-0000-1000-8000-00805f9b34fb";
        String mWriteCharacteristicUUID = "0000ff02-0000-1000-8000-00805f9b34fb";
        String mMTUCharacteristicUUID = "0000ff03-0000-1000-8000-00805f9b34fb";
        private final BluetoothIBridgeConnManager4Le.GattConnection mGattConnection = this;
        public BluetoothIBridgeDevice mmDevice;
        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothGatt mBluetoothGatt = null;
        private BluetoothIBridgeAdapter.MyHandler mHandler;
        private ArrayList<BluetoothIBridgeAdapter.DataReceiver> mDataReceivers;
        public BluetoothGattCharacteristic mWriteCharacteristic;
        private BluetoothGattCharacteristic mNotifyCharacteristic;
        private BluetoothGattCharacteristic mMTUCharacteristic;
        private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i("IBridgeGatt", "onConnectionStateChange:" + newState);
                Message msg;
                if (gatt.equals(GattConnection.this.mBluetoothGatt) && newState == 2) {
                    BluetoothIBridgeConnManager4Le.this.mList.addConnection(GattConnection.this.mGattConnection);
                    GattConnection.this.discoveryServices();
                    if (GattConnection.this.mHandler != null) {
                        msg = GattConnection.this.mHandler.obtainMessage(6);
                        msg.obj = GattConnection.this.mmDevice;
                        GattConnection.this.mmDevice.connected(true);
                        GattConnection.this.mmDevice.setConnectStatus(BluetoothIBridgeDevice.ConnectStatus.STATUS_CONNECTED);
                        GattConnection.this.mHandler.sendMessage(msg);
                    }
                } else if (gatt.equals(GattConnection.this.mBluetoothGatt) && newState == 0) {
                    Log.i("IBridgeGatt", "BluetoothGattCallback STATE_DISCONNECTED");
                    BluetoothIBridgeConnManager4Le.this.credit = 0;
                    GattConnection.this.mWriteCharacteristic = null;
                    GattConnection.this.mNotifyCharacteristic = null;
                    GattConnection.this.mMTUCharacteristic = null;
                    if (GattConnection.this.mHandler != null) {
                        msg = GattConnection.this.mHandler.obtainMessage(7);
                        msg.obj = GattConnection.this.mmDevice;
                        GattConnection.this.mmDevice.connected(false);
                        GattConnection.this.mmDevice.setConnectStatus(BluetoothIBridgeDevice.ConnectStatus.STATUS_DISCONNECTED);
                        GattConnection.this.mHandler.sendMessage(msg);
                    }

                    GattConnection.this.close();
                }

            }

            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (gatt.equals(GattConnection.this.mBluetoothGatt) && status == 0) {
                    GattConnection.this.onServicesFound(GattConnection.this.getSupportedGattServices());
                } else {
                    Log.i("IBridgeGatt", "onGattServicesDiscoveredFailed");
                }

            }

            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (gatt.equals(GattConnection.this.mBluetoothGatt) && status == 0) {
                    GattConnection.this.onDataChanged(characteristic);
                }

            }

            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                if (gatt.equals(GattConnection.this.mBluetoothGatt)) {
                    GattConnection.this.onDataChanged(characteristic);
                }

            }

            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (GattConnection.this.mNotifyCharacteristicUUID.equals(descriptor.getCharacteristic().getUuid().toString()) && GattConnection.this.mMTUCharacteristic != null) {
                    GattConnection.this.setCharacteristicNotification(GattConnection.this.mMTUCharacteristic, true);
                }

            }

            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                if (gatt.equals(GattConnection.this.mBluetoothGatt) && status == 0) {
                    Log.i("IBridgeGatt", "mtu change to " + mtu);
                    BluetoothIBridgeConnManager4Le.this.mMtu = mtu;
                } else {
                    Log.i("IBridgeGatt", "request mtu fail");
                    BluetoothIBridgeConnManager4Le.this.mMtu = 20;
                }

            }
        };

        public GattConnection(Context var1, BluetoothIBridgeDevice context, BluetoothIBridgeAdapter.MyHandler device, ArrayList<BluetoothIBridgeAdapter.DataReceiver> handler) {
//            this.mHandler = handler;
//            this.mDataReceivers = dataReceivers;
//            this.mBluetoothAdapter = BluetoothIBridgeConnManager4Le.this.mBluetoothManager.getAdapter();
//            this.mmDevice = device;
            BluetoothDevice dev = this.mBluetoothAdapter.getRemoteDevice(this.mmDevice.getDeviceAddress());
            if (Build.VERSION.SDK_INT < 21) {
//                this.mBluetoothGatt = dev.connectGatt(context, false, this.mGattCallback);
            } else {
                Class cls = BluetoothDevice.class;
                Method m = null;

                try {
                    m = cls.getMethod("connectGatt", new Class[]{Context.class, Boolean.TYPE, BluetoothGattCallback.class, Integer.TYPE});
                    if (m != null) {
                        try {
                            this.mBluetoothGatt = (BluetoothGatt) m.invoke(dev, new Object[]{context, Boolean.valueOf(false), this.mGattCallback, Integer.valueOf(2)});
                        } catch (IllegalArgumentException var10) {
                            var10.printStackTrace();
                        } catch (IllegalAccessException var11) {
                            var11.printStackTrace();
                        } catch (InvocationTargetException var12) {
                            var12.printStackTrace();
                        }
                    }
                } catch (NoSuchMethodException var13) {
                    var13.printStackTrace();
                }
            }

        }

        public void setTargetUUIDs(String serviceUUID, String notifyCharacteristicUUID, String writeCharacteristicUUID) {
            this.mGattServiceUUID = serviceUUID;
            this.mWriteCharacteristicUUID = writeCharacteristicUUID;
            this.mNotifyCharacteristicUUID = notifyCharacteristicUUID;
            if (serviceUUID == "0000ff00-0000-1000-8000-00805f9b34fb") {
                this.mMTUCharacteristicUUID = "0000ff03-0000-1000-8000-00805f9b34fb";
            }

            Iterator i$ = this.mmDevice.getGattServices().iterator();

            while (i$.hasNext()) {
                BluetoothGattService gattService = (BluetoothGattService) i$.next();
                String serviceUUIDString = gattService.getUuid().toString();
                if (serviceUUIDString != null && serviceUUIDString.equals(this.mGattServiceUUID)) {
                    List gattCharacteristics = gattService.getCharacteristics();
                    Iterator i$1 = gattCharacteristics.iterator();

                    while (i$1.hasNext()) {
                        BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic) i$1.next();
                        String characteristicUUIDString = gattCharacteristic.getUuid().toString();
                        if (characteristicUUIDString.equals(this.mWriteCharacteristicUUID)) {
                            this.mWriteCharacteristic = gattCharacteristic;
                        }

                        if (characteristicUUIDString.equals(this.mNotifyCharacteristicUUID)) {
                            this.mNotifyCharacteristic = gattCharacteristic;
                            this.setCharacteristicNotification(this.mNotifyCharacteristic, true);
                        }

                        if (characteristicUUIDString.equals(this.mMTUCharacteristicUUID)) {
                            this.mMTUCharacteristic = gattCharacteristic;
                            BluetoothIBridgeConnManager4Le.this.credit = 0;
                        }
                    }

                    return;
                }
            }

        }

        void setMtu(int mtu) {
            if (this.mBluetoothGatt != null) {
                this.mBluetoothGatt.requestMtu(mtu);
            }

        }

        void discoveryServices() {
            if (this.mBluetoothGatt != null) {
                this.mBluetoothGatt.discoverServices();
            }

        }

        void disconnnect() {
            if (this.mBluetoothGatt != null) {
                this.mBluetoothGatt.disconnect();
            }

        }

        void close() {
            if (this.mBluetoothGatt != null) {
                this.mBluetoothGatt.close();
                this.mBluetoothGatt = null;
            }

        }

        void write(byte[] buf, int length) {
            if (this.mWriteCharacteristic != null) {
                int len = length;
                int off = 0;

                while (len > 0) {
                    byte[] buffer;
                    if (len >= BluetoothIBridgeConnManager4Le.this.mMtu) {
                        buffer = new byte[BluetoothIBridgeConnManager4Le.this.mMtu];
                        System.arraycopy(buf, off, buffer, 0, BluetoothIBridgeConnManager4Le.this.mMtu);
                    } else {
                        buffer = new byte[len];
                        System.arraycopy(buf, off, buffer, 0, len);
                    }

                    if (this.mMTUCharacteristic != null) {
                        if (BluetoothIBridgeConnManager4Le.this.credit > 0) {
                            this.mWriteCharacteristic.setValue(buffer);
                            this.mWriteCharacteristic.setWriteType(1);
                            if (this.writeCharacteristic(this.mWriteCharacteristic)) {
                                off += buffer.length;
                                len -= buffer.length;
//                                BluetoothIBridgeConnManager4Le.access$310(BluetoothIBridgeConnManager4Le.this);
                            }
                        }
                    } else {
                        this.mWriteCharacteristic.setValue(buffer);
                        this.mWriteCharacteristic.setWriteType(1);
                        if (this.writeCharacteristic(this.mWriteCharacteristic)) {
                            off += buffer.length;
                            len -= buffer.length;
                        }
                    }
                }
            }

        }



        void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
            if (this.mBluetoothGatt != null) {
                this.mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
                if (this.mNotifyCharacteristicUUID.equals(characteristic.getUuid().toString()) || this.mMTUCharacteristicUUID.equals(characteristic.getUuid().toString())) {
                    characteristic.setWriteType(2);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    }

                    this.mBluetoothGatt.writeDescriptor(descriptor);
                }
            }

        }

        void readCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (this.mBluetoothGatt != null) {
                this.mBluetoothGatt.readCharacteristic(characteristic);
            }

        }

        boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
            boolean flag = false;
            if (this.mBluetoothGatt != null) {
                flag = this.mBluetoothGatt.writeCharacteristic(characteristic);
            }

            return flag;
        }

        List<BluetoothGattService> getSupportedGattServices() {
            return this.mBluetoothGatt == null ? null : this.mBluetoothGatt.getServices();
        }

        void onServicesFound(List<BluetoothGattService> gattServices) {
            Message msg = this.mHandler.obtainMessage(12);
            this.mmDevice.setGattServices(gattServices);
            msg.obj = this.mmDevice;
            this.mHandler.sendMessage(msg);
            Iterator i$ = gattServices.iterator();

            while (i$.hasNext()) {
                BluetoothGattService gattService = (BluetoothGattService) i$.next();
                String serviceUUIDString = gattService.getUuid().toString();
                if (serviceUUIDString != null && serviceUUIDString.equals(this.mGattServiceUUID)) {
                    List gattCharacteristics = gattService.getCharacteristics();
                    Iterator i$1 = gattCharacteristics.iterator();

                    while (i$1.hasNext()) {
                        BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic) i$1.next();
                        String characteristicUUIDString = gattCharacteristic.getUuid().toString();
                        if (characteristicUUIDString.equals(this.mWriteCharacteristicUUID)) {
                            this.mWriteCharacteristic = gattCharacteristic;
                        }

                        if (characteristicUUIDString.equals(this.mNotifyCharacteristicUUID)) {
                            this.mNotifyCharacteristic = gattCharacteristic;
                            this.setCharacteristicNotification(this.mNotifyCharacteristic, true);
                        }

                        if (characteristicUUIDString.equals(this.mMTUCharacteristicUUID)) {
                            this.mMTUCharacteristic = gattCharacteristic;
                            BluetoothIBridgeConnManager4Le.this.credit = 0;
                        }
                    }

                    return;
                }
            }

        }

        void onDataChanged(BluetoothGattCharacteristic characteristic) {
            byte[] data;
            int mtu;
            if (this.mNotifyCharacteristicUUID.equals(characteristic.getUuid().toString())) {
                data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    this.mmDevice.buffer = data;
                    this.mmDevice.length = data.length;
                    if (this.mDataReceivers != null) {
                        ArrayList d = (ArrayList) this.mDataReceivers.clone();
                        mtu = d.size();

                        for (int i = 0; i < mtu; ++i) {
                            BluetoothIBridgeAdapter.DataReceiver er = (BluetoothIBridgeAdapter.DataReceiver) d.get(i);
                            if (this.mmDevice.isValidDevice()) {
                                er.onDataReceived(this.mmDevice, this.mmDevice.buffer, this.mmDevice.length);
                            }
                        }
                    }
                }
            }

            if (this.mMTUCharacteristicUUID.equals(characteristic.getUuid().toString())) {
                data = characteristic.getValue();
                byte var7 = data[0];
                if (var7 == 1) {
                    BluetoothIBridgeConnManager4Le.this.credit = data[1];
                } else if (var7 == 2) {
                    mtu = data[1] + (data[2] << 8);
                }
            }

        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (!(o instanceof BluetoothIBridgeConnManager4Le.GattConnection)) {
                return false;
            } else {
                BluetoothIBridgeConnManager4Le.GattConnection conn = (BluetoothIBridgeConnManager4Le.GattConnection) o;
                return conn.mmDevice.equals(this.mmDevice);
            }
        }

        BluetoothIBridgeDevice getDevice() {
            return this.mmDevice;
        }
    }

    private class ConnectionList {
        private List<BluetoothIBridgeConnManager4Le.GattConnection> mConnectedDevices;
        private byte[] LOCK;

        private ConnectionList() {
            this.mConnectedDevices = new ArrayList();
            this.LOCK = new byte[0];
        }

        public void write(BluetoothIBridgeDevice device, byte[] buffer, int length) {
            if (null != device && null != buffer && length > 0) {
                BluetoothIBridgeConnManager4Le.GattConnection found = this.foundDevice(device);
                if (null != found) {
                    found.write(buffer, length);
                }

            }
        }

        public void addConnection(BluetoothIBridgeConnManager4Le.GattConnection connection) {
            BluetoothIBridgeConnManager4Le.GattConnection found = this.foundDevice(connection.getDevice());
            byte[] var3;
            if (found != null) {
                var3 = this.LOCK;
                synchronized (this.LOCK) {
                    this.mConnectedDevices.remove(found);
                }
            }

            var3 = this.LOCK;
            synchronized (this.LOCK) {
                this.mConnectedDevices.add(connection);
            }
        }

        private BluetoothIBridgeConnManager4Le.GattConnection foundDevice(BluetoothIBridgeDevice device) {
            BluetoothIBridgeConnManager4Le.GattConnection found = null;
            byte[] var3 = this.LOCK;
            synchronized (this.LOCK) {
                Iterator i$ = this.mConnectedDevices.iterator();

                while (i$.hasNext()) {
                    BluetoothIBridgeConnManager4Le.GattConnection ds = (BluetoothIBridgeConnManager4Le.GattConnection) i$.next();
                    if (device.equals(ds.getDevice())) {
                        found = ds;
                        break;
                    }
                }

                return found;
            }
        }

        public List<BluetoothIBridgeDevice> getCurrentConnectedDevice() {
            ArrayList devicesList = new ArrayList();
            byte[] var2 = this.LOCK;
            synchronized (this.LOCK) {
                Iterator i$ = this.mConnectedDevices.iterator();

                while (i$.hasNext()) {
                    BluetoothIBridgeConnManager4Le.GattConnection ds = (BluetoothIBridgeConnManager4Le.GattConnection) i$.next();
                    BluetoothIBridgeDevice device = ds.getDevice();
                    if (device != null && !devicesList.contains(device)) {
                        devicesList.add(device);
                    }
                }

                return devicesList;
            }
        }

        public void clear() {
            byte[] var1 = this.LOCK;
            synchronized (this.LOCK) {
                this.mConnectedDevices.clear();
            }
        }

        public void releaseAllConnections() {
            byte[] var1 = this.LOCK;
            synchronized (this.LOCK) {
                Iterator i$ = this.mConnectedDevices.iterator();

                while (true) {
                    if (!i$.hasNext()) {
                        break;
                    }

                    BluetoothIBridgeConnManager4Le.GattConnection ds = (BluetoothIBridgeConnManager4Le.GattConnection) i$.next();
                    if (ds != null) {
                        ds.close();
                    }
                }
            }

            this.mConnectedDevices.clear();
        }
    }
}
