

package bledocking.munc.app.ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class BluetoothIBridgeDeviceFactory {
    private static BluetoothIBridgeDeviceFactory INSTANCE = null;
    private static byte[] LOCK = new byte[0];
    private List<BluetoothIBridgeDevice> mList = new ArrayList();

    private BluetoothIBridgeDeviceFactory() {
    }

    public static BluetoothIBridgeDeviceFactory getDefaultFactory() {
        byte[] var0 = LOCK;
        synchronized(LOCK) {
            if(INSTANCE == null) {
                INSTANCE = new BluetoothIBridgeDeviceFactory();
            }
        }

        return INSTANCE;
    }

    public BluetoothIBridgeDevice createDevice(BluetoothDevice device, int deviceTye) {
        if(null != device) {
            Iterator it = this.mList.iterator();

            BluetoothIBridgeDevice newDev;
            do {
                if(!it.hasNext()) {
                    newDev = new BluetoothIBridgeDevice(device);
                    newDev.setDeviceType(deviceTye);
                    this.mList.add(newDev);
                    return newDev;
                }

                newDev = (BluetoothIBridgeDevice)it.next();
            } while(newDev == null || !newDev.isSameDevice(device, deviceTye));

            return newDev;
        } else {
            return null;
        }
    }

    public BluetoothIBridgeDevice createDevice(String address, int deivceType) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null?this.createDevice(adapter.getRemoteDevice(address), deivceType):null;
    }
}
