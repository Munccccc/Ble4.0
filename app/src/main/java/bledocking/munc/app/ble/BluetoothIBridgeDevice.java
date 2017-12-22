
package bledocking.munc.app.ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import bledocking.munc.app.ble.util.SystemUtils;

public class BluetoothIBridgeDevice implements Parcelable {
    static final UUID SPPUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public BluetoothDevice mDevice;
    private String mDeviceAddress;
    private String mDeviceName;
    private int mDeviceType;
    private boolean mIsConnected;
    private BluetoothIBridgeDevice.Direction mDirection;
    private BluetoothIBridgeDevice.ConnectStatus mConnectStatus;
    private BluetoothIBridgeDevice.BondStatus mBondStatus;
    private int mDeviceClass;
    private List<BluetoothGattService> mGattServices;
    static final String EXTRA_PAIRING_VARIANT = "android.bluetooth.device.extra.PAIRING_VARIANT";
    static final String EXTRA_PAIRING_KEY = "android.bluetooth.device.extra.PAIRING_KEY";
    static final String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    static final String ACTION_PAIRING_CANCEL = "android.bluetooth.device.action.PAIRING_CANCEL";
    static final int PAIRING_VARIANT_PIN = 0;
    static final int PAIRING_VARIANT_PASSKEY = 1;
    static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
    static final int PAIRING_VARIANT_CONSENT = 3;
    static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;
    static final int PAIRING_VARIANT_DISPLAY_PIN = 5;
    static final int PAIRING_VARIANT_OOB_CONSENT = 6;
    public static int DEVICE_TYPE_CLASSIC = 0;
    public static int DEVICE_TYPE_BLE = 1;
    byte[] buffer;
    int length;
    public static final Creator<BluetoothIBridgeDevice> CREATOR = new Creator() {
        public BluetoothIBridgeDevice createFromParcel(Parcel source) {
            return new BluetoothIBridgeDevice(source);
        }

        public BluetoothIBridgeDevice[] newArray(int size) {
            return new BluetoothIBridgeDevice[size];
        }
    };

    BluetoothIBridgeDevice(BluetoothDevice device) {
        this.mDirection = BluetoothIBridgeDevice.Direction.DIRECTION_NONE;
        this.mConnectStatus = BluetoothIBridgeDevice.ConnectStatus.STATUS_DISCONNECTED;
        this.mBondStatus = BluetoothIBridgeDevice.BondStatus.STATE_BONDNONE;
        this.mDeviceAddress = device.getAddress();
        this.mDevice = device;
        this.mDeviceName = this.mDevice.getName();
        BluetoothClass bluetoothClass = null;

        try {
            bluetoothClass = this.mDevice.getBluetoothClass();
        } catch (NullPointerException var4) {
        }

        if(bluetoothClass != null) {
            this.mDeviceClass = this.mDevice.getBluetoothClass().getDeviceClass();
        } else {
            this.mDeviceClass = -1;
        }

    }

    public static BluetoothIBridgeDevice createBluetoothIBridgeDevice(String address, int deviceType) {
        BluetoothIBridgeDeviceFactory factory = BluetoothIBridgeDeviceFactory.getDefaultFactory();
        return factory.createDevice(address, deviceType);
    }

    private BluetoothIBridgeDevice(Parcel source) {
        this.mDirection = BluetoothIBridgeDevice.Direction.DIRECTION_NONE;
        this.mConnectStatus = BluetoothIBridgeDevice.ConnectStatus.STATUS_DISCONNECTED;
        this.mBondStatus = BluetoothIBridgeDevice.BondStatus.STATE_BONDNONE;
        this.readFromParcel(source);
    }

    public String getDeviceName() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        this.mDevice = adapter.getRemoteDevice(this.mDeviceAddress);
        this.mDeviceName = this.mDevice.getName();
        return this.mDeviceName;
    }

    public String getDeviceAddress() {
        return this.mDeviceAddress;
    }

    public int getDeviceType() {
        return this.mDeviceType;
    }

    public void setDeviceType(int deviceType) {
        this.mDeviceType = deviceType;
    }

    public int getDeviceClass() {
        return this.mDeviceClass;
    }

    public List<BluetoothGattService> getGattServices() {
        return this.mGattServices;
    }

    public String toString() {
        String name = this.mDeviceName == null?"Device":this.mDeviceName;
        String addr = this.mDeviceAddress == null?"00:00:00:00:00:00":this.mDeviceAddress;
        return super.toString() + " [" + name + " - " + addr + "]";
    }

    public boolean isConnected() {
        return this.mIsConnected;
    }

    public void createBond() {
        try {
            this.mDevice.getClass().getMethod("createBond", (Class[])null).invoke(this.mDevice, new Object[0]);
        } catch (NoSuchMethodException var2) {
            var2.printStackTrace();
        } catch (IllegalAccessException var3) {
            var3.printStackTrace();
        } catch (InvocationTargetException var4) {
            var4.printStackTrace();
        }

    }

    public void removeBond() {
        try {
            this.mDevice.getClass().getMethod("removeBond", (Class[])null).invoke(this.mDevice, new Object[0]);
        } catch (NoSuchMethodException var2) {
            var2.printStackTrace();
        } catch (IllegalAccessException var3) {
            var3.printStackTrace();
        } catch (InvocationTargetException var4) {
            var4.printStackTrace();
        }

    }

    boolean isValidDevice() {
        return true;
    }

    public boolean equals(Object o) {
        if(o == null) {
            return false;
        } else if(!(o instanceof BluetoothIBridgeDevice)) {
            return false;
        } else {
            String addr = this.mDeviceAddress == null?"00:00:00:00:00:00":this.mDeviceAddress;
            BluetoothIBridgeDevice dev = (BluetoothIBridgeDevice)o;
            String another = dev.mDeviceAddress == null?"00:00:00:00:00:00":dev.mDeviceAddress;
            return addr.equals(another) && dev.getDeviceType() == this.mDeviceType;
        }
    }

    BluetoothSocket createSocket() {
        BluetoothSocket socket = null;
        if(Build.VERSION.SDK_INT >= 10 && !SystemUtils.isMediatekPlatform()) {
            Class e = BluetoothDevice.class;
            Method m = null;

            try {
                m = e.getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
            } catch (NoSuchMethodException var9) {
                var9.printStackTrace();
            }

            if(m != null) {
                try {
                    socket = (BluetoothSocket)m.invoke(this.mDevice, new Object[]{SPPUUID});
                } catch (IllegalArgumentException var6) {
                    var6.printStackTrace();
                } catch (IllegalAccessException var7) {
                    var7.printStackTrace();
                } catch (InvocationTargetException var8) {
                    var8.printStackTrace();
                }
            }
        } else {
            try {
                socket = this.mDevice.createRfcommSocketToServiceRecord(SPPUUID);
            } catch (IOException var5) {
                var5.printStackTrace();
            }
        }

        return socket;
    }

    BluetoothSocket createSocketWithChannel(int channel) {
        BluetoothSocket socket = null;
        Class cls = BluetoothDevice.class;
        Method m = null;

        try {
            m = cls.getMethod("createRfcommSocket", new Class[]{Integer.TYPE});
        } catch (NoSuchMethodException var9) {
            var9.printStackTrace();
        }

        if(m != null) {
            try {
                socket = (BluetoothSocket)m.invoke(this.mDevice, new Object[]{Integer.valueOf(channel)});
            } catch (IllegalArgumentException var6) {
                var6.printStackTrace();
            } catch (IllegalAccessException var7) {
                var7.printStackTrace();
            } catch (InvocationTargetException var8) {
                var8.printStackTrace();
            }
        }

        return socket;
    }

    void connected(boolean connected) {
        this.mIsConnected = connected;
    }

    void setConnectionDirection(BluetoothIBridgeDevice.Direction d) {
        this.mDirection = d;
    }

    BluetoothIBridgeDevice.Direction connectionDirection() {
        return this.mDirection;
    }

    void setConnectStatus(BluetoothIBridgeDevice.ConnectStatus d) {
        this.mConnectStatus = d;
    }

    BluetoothIBridgeDevice.ConnectStatus getConnectStatus() {
        return this.mConnectStatus;
    }

    void setBondStatus() {
        if(this.mDevice.getBondState() == 12) {
            this.mBondStatus = BluetoothIBridgeDevice.BondStatus.STATE_BONDED;
        }

        if(this.mDevice.getBondState() == 11) {
            this.mBondStatus = BluetoothIBridgeDevice.BondStatus.STATE_BONDING;
        }

        if(this.mDevice.getBondState() == 10) {
            this.mBondStatus = BluetoothIBridgeDevice.BondStatus.STATE_BONDNONE;
        }

    }

    void setBondStatus(BluetoothIBridgeDevice.BondStatus d) {
        this.mBondStatus = d;
    }

    BluetoothIBridgeDevice.BondStatus getBondStatus() {
        return this.mBondStatus;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mDeviceName);
        dest.writeString(this.mDeviceAddress);
        dest.writeInt(this.mDeviceClass);
        dest.writeInt(this.mIsConnected?1:0);
        dest.writeInt(this.mDirection.ordinal());
        dest.writeInt(this.mConnectStatus.ordinal());
        dest.writeInt(this.mBondStatus.ordinal());
    }

    private void readFromParcel(Parcel source) {
        this.mDeviceName = source.readString();
        this.mDeviceAddress = source.readString();
        this.mDeviceClass = source.readInt();
        this.mIsConnected = source.readInt() == 1;
        int i = source.readInt();
        BluetoothIBridgeDevice.Direction[] ds = BluetoothIBridgeDevice.Direction.values();
        if(i < ds.length) {
            this.mDirection = BluetoothIBridgeDevice.Direction.values()[i];
        } else {
            this.mDirection = BluetoothIBridgeDevice.Direction.DIRECTION_NONE;
        }

        int j = source.readInt();
        BluetoothIBridgeDevice.ConnectStatus[] cs = BluetoothIBridgeDevice.ConnectStatus.values();
        if(i < cs.length) {
            this.mConnectStatus = BluetoothIBridgeDevice.ConnectStatus.values()[j];
        } else {
            this.mConnectStatus = BluetoothIBridgeDevice.ConnectStatus.STATUS_DISCONNECTED;
        }

        int k = source.readInt();
        BluetoothIBridgeDevice.BondStatus[] bs = BluetoothIBridgeDevice.BondStatus.values();
        if(i < cs.length) {
            this.mBondStatus = BluetoothIBridgeDevice.BondStatus.values()[j];
        } else {
            this.mBondStatus = BluetoothIBridgeDevice.BondStatus.STATE_BONDNONE;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        this.mDevice = adapter.getRemoteDevice(this.mDeviceAddress);
    }

    void setPin(byte[] pin) {
        try {
            Class e = Class.forName(this.mDevice.getClass().getName());
            Method setPin = e.getMethod("setPin", new Class[]{byte[].class});
            setPin.setAccessible(true);
            setPin.invoke(this.mDevice, new Object[]{pin});
        } catch (NoSuchMethodException var4) {
            var4.printStackTrace();
        } catch (IllegalAccessException var5) {
            var5.printStackTrace();
        } catch (InvocationTargetException var6) {
            var6.printStackTrace();
        } catch (ClassNotFoundException var7) {
            var7.printStackTrace();
        }

    }

    void setPasskey(int passkey) {
        try {
            Class e = Class.forName(this.mDevice.getClass().getName());
            Method setPasskey = e.getMethod("setPasskey", new Class[]{Integer.TYPE});
            setPasskey.setAccessible(true);
            setPasskey.invoke(this.mDevice, new Object[]{Integer.valueOf(passkey)});
        } catch (NoSuchMethodException var4) {
            var4.printStackTrace();
        } catch (IllegalAccessException var5) {
            var5.printStackTrace();
        } catch (InvocationTargetException var6) {
            var6.printStackTrace();
        } catch (ClassNotFoundException var7) {
            var7.printStackTrace();
        }

    }

    void setPairingConfirmation(boolean confirm) {
        try {
            Class e = Class.forName(this.mDevice.getClass().getName());
            Method setPairingConfirmation = e.getMethod("setPairingConfirmation", new Class[]{Boolean.TYPE});
            setPairingConfirmation.setAccessible(true);
            setPairingConfirmation.invoke(this.mDevice, new Object[]{Boolean.valueOf(confirm)});
        } catch (NoSuchMethodException var4) {
            var4.printStackTrace();
        } catch (IllegalAccessException var5) {
            var5.printStackTrace();
        } catch (InvocationTargetException var6) {
            var6.printStackTrace();
        } catch (ClassNotFoundException var7) {
            var7.printStackTrace();
        }

    }

    void cancelPairingUserInput() {
        try {
            this.mDevice.getClass().getMethod("cancelPairingUserInput", (Class[])null).invoke(this.mDevice, new Object[0]);
        } catch (NoSuchMethodException var2) {
            var2.printStackTrace();
        } catch (IllegalAccessException var3) {
            var3.printStackTrace();
        } catch (InvocationTargetException var4) {
            var4.printStackTrace();
        }

    }

    boolean isBonded() {
        return this.mDevice != null?this.mDevice.getBondState() == 12:false;
    }

    boolean isSameDevice(BluetoothDevice device, int deviceTye) {
        String addr = this.mDeviceAddress == null?"00:00:00:00:00:00":this.mDeviceAddress;
        String another = device.getAddress() == null?"00:00:00:00:00:00":device.getAddress();
        return addr.equals(another) && deviceTye == this.mDeviceType;
    }

    void setGattServices(List<BluetoothGattService> gattServices) {
        this.mGattServices = gattServices;
    }

    public static enum BondStatus {
        STATE_BONDED,
        STATE_BONDING,
        STATE_BONDNONE,
        STATE_BONDFAILED,
        STATE_BOND_OVERTIME,
        STATE_BOND_CANCLED;

        private BondStatus() {
        }
    }

    public static enum ConnectStatus {
        STATUS_DISCONNECTED,
        STATUS_CONNECTED,
        STATUS_DISCONNECTTING,
        STATUS_CONNECTTING,
        STATUS_CONNECTFAILED,
        STATE_BONDED,
        STATE_BONDING,
        STATE_BONDNONE;

        private ConnectStatus() {
        }
    }

    static enum Direction {
        DIRECTION_NONE,
        DIRECTION_FORWARD,
        DIRECTION_BACKWARD;

        private Direction() {
        }
    }
}
