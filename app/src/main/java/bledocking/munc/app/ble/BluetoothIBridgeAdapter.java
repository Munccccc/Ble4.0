
package bledocking.munc.app.ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import bledocking.munc.app.ble.Ancs.GattAncsServer;
import bledocking.munc.app.ble.Ancs.GattNotificationManager;
import bledocking.munc.app.ble.Ancs.PhoneStateReceiver;
import bledocking.munc.app.ble.Ancs.SMSReceiver;

public class BluetoothIBridgeAdapter {
    static final String LAST_CONNECTED_DEVICE = "last_connected_device";
    static final String LAST_CONNECTED_DEVICE_NAME = "last_connected_device_name";
    static final String LAST_CONNECTED_DEVICE_ADDRESS = "last_connected_device_address";
    static final boolean D = true;
    static final int MESSAGE_BLUETOOTH_ON = 1;
    static final int MESSAGE_BLUETOOTH_OFF = 2;
    static final int MESSAGE_DEVICE_BONDING = 3;
    static final int MESSAGE_DEVICE_BONDED = 4;
    static final int MESSAGE_DEVICE_BONDNONE = 5;
    static final int MESSAGE_DEVICE_CONNECTED = 6;
    static final int MESSAGE_DEVICE_DISCONNECTED = 7;
    static final int MESSAGE_DEVICE_CONNECT_FAILED = 8;
    static final int MESSAGE_DEVICE_FOUND = 9;
    static final int MESSAGE_DISCOVERY_FINISHED = 10;
    static final int MESSAGE_WRITE_FAILED = 11;
    static final int MESSAGE_LE_SERVICES_DISCOVERED = 12;
    static final String VERSION_CODE = "2.9";
    private static BluetoothIBridgeAdapter bluetoothIBridgeAdapter = null;
    private BluetoothAdapter mAdapter;
    private BluetoothIBridgeAdapter.MyHandler mHandler;
    private static BluetoothIBridgeAdapter sAdapter;
    BluetoothAdapter.LeScanCallback mLeScanCallback = null;
    private boolean mDiscoveryOnlyBonded;
    private Context mContext;
    private boolean isBtEnable = false;
    private boolean isAutoWritePincode = false;
    private BluetoothIBridgeConnManager mConnManager = null;
    private BluetoothIBridgeConnManager4Le mConnManager4Le = null;
    private ArrayList<EventReceiver> mEventReceivers = null;
    LocationManager mLocationManager;
    double mLatitude = 0.0D;
    double mLongitude = 0.0D;
    private GattAncsServer gattAncsServer = null;
    private GattNotificationManager gattNotificationManager = GattNotificationManager.sharedInstance();
    private PhoneStateReceiver phoneStateReceiver = null;
    private SMSReceiver smsReceiver = null;
    private ArrayList<BluetoothIBridgeAdapter.AncsReceiver> ancsReceivers = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String exceptionMessage = null;
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                exceptionMessage = bundle.getString("exception");
            }

            Log.i("BluetoothIBridgeAdapter", "broadcast message:" + action.toString());
            BluetoothDevice dev;
            if (action.equals("android.bluetooth.device.action.FOUND")) {
                dev = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                BluetoothIBridgeAdapter.this.onEventReceived(9, BluetoothIBridgeDeviceFactory.getDefaultFactory().createDevice(dev, BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC), exceptionMessage);
            }

            if (action.equals("android.bluetooth.adapter.action.DISCOVERY_FINISHED")) {
                BluetoothIBridgeAdapter.this.onEventReceived(10, (BluetoothIBridgeDevice) null, exceptionMessage);
            }

            if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                if (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1) == 12) {
                    BluetoothIBridgeAdapter.this.isBtEnable = true;
                    BluetoothIBridgeAdapter.this.mConnManager.start();
                    if (BluetoothIBridgeAdapter.this.gattAncsServer != null) {
                        BluetoothIBridgeAdapter.this.gattAncsServer.registerService(BluetoothIBridgeAdapter.this.mContext);
                    }

                    BluetoothIBridgeAdapter.this.onEventReceived(1, (BluetoothIBridgeDevice) null, exceptionMessage);
                }

                if (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1) == 10) {
                    BluetoothIBridgeAdapter.this.onEventReceived(2, (BluetoothIBridgeDevice) null, exceptionMessage);
                    BluetoothIBridgeAdapter.this.isBtEnable = false;
                    if (BluetoothIBridgeAdapter.this.mConnManager != null) {
                        BluetoothIBridgeAdapter.this.mConnManager.stop();
                    }

                    if (BluetoothIBridgeAdapter.this.gattAncsServer != null) {
                        BluetoothIBridgeAdapter.this.gattAncsServer.unregisterService();
                    }
                }
            }

            BluetoothIBridgeDevice device;
//            if (action.equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
//                dev = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
//                device = BluetoothIBridgeDeviceFactory.getDefaultFactory().createDevice(dev, BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC);
//                if (device != null) {
//                    device.setBondStatus();
//                    switch (BluetoothIBridgeAdapter.SyntheticClass_1.$SwitchMap$com$ivt$bluetooth$ibridge$BluetoothIBridgeDevice$BondStatus[device.getBondStatus().ordinal()]) {
//                        case 1:
//                            BluetoothIBridgeAdapter.this.onEventReceived(3, device, exceptionMessage);
//                            break;
//                        case 2:
//                            BluetoothIBridgeAdapter.this.onEventReceived(4, device, exceptionMessage);
//                            break;
//                        case 3:
//                            BluetoothIBridgeAdapter.this.onEventReceived(5, device, exceptionMessage);
//                    }
//                }
//            }

            if (action.equals("android.bluetooth.device.action.PAIRING_REQUEST") && BluetoothIBridgeAdapter.this.isAutoWritePincode) {
                dev = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                device = BluetoothIBridgeDeviceFactory.getDefaultFactory().createDevice(dev, BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC);
                int type = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", -2147483648);
                int pairingKey = 0;
                if (type == 2 || type == 4 || type == 5) {
                    pairingKey = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", -2147483648);
                }

                BluetoothIBridgeAdapter.this.mConnManager.onPairingRequested(device, type, pairingKey);
            }

        }
    };

    public static BluetoothIBridgeAdapter sharedInstance(Context context) {
        if (bluetoothIBridgeAdapter == null && context != null) {
            bluetoothIBridgeAdapter = new BluetoothIBridgeAdapter(context);
        }

        return bluetoothIBridgeAdapter;
    }

    public static String getVersion() {
        return "2.9";
    }

    public static boolean bleIsSupported() {
        if (Build.VERSION.SDK_INT >= 18) {
            return true;
        } else {
            Log.e("BluetoothIBridgeAdapter", "BLE can not be supported");
            return false;
        }
    }

    private BluetoothIBridgeAdapter(Context context) {
        Log.e("Adapter", "Create....");
        this.mContext = context;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mHandler = new BluetoothIBridgeAdapter.MyHandler(this);
        this.mConnManager = new BluetoothIBridgeConnManager(context, this.mHandler);
        if (this.isEnabled()) {
            this.mConnManager.start();
        }

        if (bleIsSupported()) {
            this.mConnManager4Le = new BluetoothIBridgeConnManager4Le(context, this.mHandler);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.FOUND");
        intentFilter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        if (sAdapter != null) {
            sAdapter.clean();
        }

        sAdapter = this;
        if (bleIsSupported()) {
            this.gattAncsServer = new GattAncsServer();
            if (this.isEnabled()) {
                this.gattAncsServer.registerService(this.mContext);
            }

            this.gattAncsServer.registerCallback(new GattAncsServer.GattAncsServerCallback() {
                public void onControlPointDataIn(byte[] value) {
                    BluetoothIBridgeAdapter.this.gattNotificationManager.parseControlPoint(value);
                }
            });
        }

        this.gattNotificationManager.setNotificationPrividerGattFunctions(new GattNotificationManager.NotificationPrividerGattFunctions() {
            public void notifyAncsNotificationSource(byte[] packet) {
                BluetoothIBridgeAdapter.this.gattAncsServer.notifyAncsNotificationSource(packet);
            }

            public void notifyAncsDataSoure(byte[] packet) {
                BluetoothIBridgeAdapter.this.gattAncsServer.notifyAncsDataSoure(packet);
            }

            public void onPerformNotificationAction(String appIdentifier, byte actionID) {
                Iterator i$ = BluetoothIBridgeAdapter.this.ancsReceivers.iterator();

                while (i$.hasNext()) {
                    BluetoothIBridgeAdapter.AncsReceiver ancsReceiver = (BluetoothIBridgeAdapter.AncsReceiver) i$.next();
                    ancsReceiver.onPerformNotificationAction(appIdentifier, actionID);
                }

            }
        });
    }

    public void destroy() {
        Log.e("Adapter", "destroy");
        if (this.phoneStateReceiver != null) {
            this.mContext.unregisterReceiver(this.phoneStateReceiver);
        }

        if (this.smsReceiver != null) {
            this.mContext.unregisterReceiver(this.smsReceiver);
        }

        if (this.mConnManager4Le != null) {
            this.mConnManager4Le.destory();
            this.mConnManager4Le = null;
        }

        if (this.mConnManager != null) {
            this.mConnManager.stop();
            this.mConnManager = null;
        }

        if (this.gattAncsServer != null) {
            this.gattAncsServer.unregisterService();
        }

        if (this.mContext != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }

        this.mContext = null;
        sAdapter = null;
        bluetoothIBridgeAdapter = null;
    }

    public void setEnabled(boolean enabled) {
        Log.i("BluetoothIBridgeAdapter", "setEnabled to " + enabled + "...");
        if (this.isEnabled() == enabled) {
            Log.i("BluetoothIBridgeAdapter", "bluetooth already enabled");
        } else {
            if (this.mAdapter == null) {
                Log.e("BluetoothIBridgeAdapter", "bluetooth adapter is null");
            }

            if (enabled) {
                Log.i("BluetoothIBridgeAdapter", "enable bluetooth");
                this.mAdapter.enable();
            } else {
                Log.i("BluetoothIBridgeAdapter", "disable bluetooth");
                this.mAdapter.disable();
            }

            Log.i("BluetoothIBridgeAdapter", "setEnabled.");
        }
    }

    public boolean isEnabled() {
        if (this.mAdapter != null) {
            this.isBtEnable = this.mAdapter.isEnabled();
        }

        return this.isBtEnable;
    }

    public void registerEventReceiver(BluetoothIBridgeAdapter.EventReceiver receiver) {
        Log.i("BluetoothIBridgeAdapter", "registerEventReceiver " + receiver + "...");
        if (receiver == null) {
            Log.e("BluetoothIBridgeAdapter", "receiver is null");
        }

        if (this.mEventReceivers == null) {
            this.mEventReceivers = new ArrayList();
        }

        if (!this.mEventReceivers.contains(receiver)) {
            this.mEventReceivers.add(receiver);
        }

        Log.i("BluetoothIBridgeAdapter", "registerEventReceiver.");
    }

    public void unregisterEventReceiver(BluetoothIBridgeAdapter.EventReceiver receiver) {
        Log.i("BluetoothIBridgeAdapter", "unregisterEventReceiver " + receiver + "...");
        if (this.mEventReceivers != null) {
            this.mEventReceivers.remove(receiver);
        }

        Log.i("BluetoothIBridgeAdapter", "unregisterEventReceiver.");
    }

    public void registerDataReceiver(BluetoothIBridgeAdapter.DataReceiver receiver) {
        Log.i("BluetoothIBridgeAdapter", "registerDataReceiver " + receiver + "...");
        if (this.mConnManager != null) {
            this.mConnManager.registerDataReceiver(receiver);
        }

        if (this.mConnManager4Le != null) {
            this.mConnManager4Le.registerDataReceiver(receiver);
        }

        Log.i("BluetoothIBridgeAdapter", "registerDataReceiver.");
    }

    public void unregisterDataReceiver(BluetoothIBridgeAdapter.DataReceiver receiver) {
        Log.i("BluetoothIBridgeAdapter", "unregisterDataReceiver " + receiver + "...");
        if (this.mConnManager != null) {
            this.mConnManager.unregisterDataReceiver(receiver);
        }

        if (this.mConnManager4Le != null) {
            this.mConnManager4Le.unregisterDataReceiver(receiver);
        }

        Log.i("BluetoothIBridgeAdapter", "unregisterDataReceiver.");
    }

    public boolean startDiscovery(boolean onlyBonded) {
        Log.i("BluetoothIBridgeAdapter", "startDiscovery...");
        boolean result = false;
        if (this.isEnabled()) {
            this.mDiscoveryOnlyBonded = onlyBonded;
            if (this.mAdapter.isDiscovering()) {
                Log.i("BluetoothIBridgeAdapter", "stop previous discovering");
                this.mAdapter.cancelDiscovery();
            }

            if (onlyBonded) {
                Log.i("BluetoothIBridgeAdapter", "startDiscovery only bonded");
            } else {
                Log.i("BluetoothIBridgeAdapter", "startDiscovery");
            }

            this.mAdapter.startDiscovery();
            result = true;
        } else {
            Log.e("BluetoothIBridgeAdapter", "bluetooth is not enabled");
        }

        Log.i("BluetoothIBridgeAdapter", "startDiscovery.");
        return result;
    }

    public boolean startDiscovery() {
        return this.startDiscovery(false);
    }

    public void stopDiscovery() {
        Log.i("BluetoothIBridgeAdapter", "stopDiscovery ...");
        if (this.isEnabled()) {
            this.mAdapter.cancelDiscovery();
        } else {
            Log.e("BluetoothIBridgeAdapter", "bluetooth is not enabled");
        }

        Log.i("BluetoothIBridgeAdapter", "stopDiscovery.");
    }

    public boolean connectDevice(BluetoothIBridgeDevice device) {
        boolean result = this.connectDevice(device, 10);
        if (!result) {
            this.onEventReceived(8, device, "parameter invalid");
        }

        return result;
    }

    public boolean connectDevice(BluetoothIBridgeDevice device, int bondTime) {
        Log.i("BluetoothIBridgeAdapter", "connectDevice...");
        Log.i("BluetoothIBridgeAdapter", "bondTime = " + bondTime);
        boolean result = false;
        if (this.isEnabled()) {
            if (device != null) {
                Log.i("BluetoothIBridgeAdapter", "start to connect");
                if (device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC) {
                    this.mConnManager.connect(device, bondTime);
                    result = true;
                } else if (device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
                    this.mConnManager4Le.connect(device);
                    result = true;
                }
            } else {
                Log.e("BluetoothIBridgeAdapter", "device is null");
            }
        } else {
            Log.e("BluetoothIBridgeAdapter", "bluetooth is not enabled");
        }

        Log.i("BluetoothIBridgeAdapter", "connectDevice.");
        return result;
    }

    public void cancelBondProcess() {
        Log.i("BluetoothIBridgeAdapter", "cancelBondProcess...");
        if (this.mConnManager != null) {
            this.mConnManager.cancelBond();
        }

        Log.i("BluetoothIBridgeAdapter", "cancelBondProcess.");
    }

    public void disconnectDevice(BluetoothIBridgeDevice device) {
        Log.i("BluetoothIBridgeAdapter", "disconnectDevice...");
        if (this.isEnabled()) {
            if (device != null) {
                if (device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC) {
                    this.mConnManager.disconnect(device);
                } else if (device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
                    this.mConnManager4Le.disconnect(device);
                }
            } else {
                Log.e("BluetoothIBridgeAdapter", "device is not enabled");
            }
        }

        Log.i("BluetoothIBridgeAdapter", "disconnectDevice.");
    }

    public void send(BluetoothIBridgeDevice device, byte[] buffer, int length) {
        if (this.isEnabled() && device != null) {
            if (device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC) {
                this.mConnManager.write(device, buffer, length);
            } else if (device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
                this.mConnManager4Le.write(device, buffer, length);
            }
        }

    }

    public List<BluetoothIBridgeDevice> getCurrentConnectedDevices() {
        Log.i("BluetoothIBridgeAdapter", "getCurrentConnectedDevices...");
        List devicesList = this.mConnManager.getCurrentConnectedDevice();
        ArrayList devicesListTotal = new ArrayList();
        if (devicesList != null) {
            Iterator devicesList4Gatt = devicesList.iterator();

            while (devicesList4Gatt.hasNext()) {
                BluetoothIBridgeDevice i$ = (BluetoothIBridgeDevice) devicesList4Gatt.next();
                devicesListTotal.add(i$);
            }
        }

        if (bleIsSupported()) {
            List devicesList4Gatt1 = this.mConnManager4Le.getCurrentConnectedDevice();
            if (devicesList4Gatt1 != null) {
                Iterator i$1 = devicesList4Gatt1.iterator();

                while (i$1.hasNext()) {
                    BluetoothIBridgeDevice device = (BluetoothIBridgeDevice) i$1.next();
                    devicesListTotal.add(device);
                }
            }
        }

        Log.i("BluetoothIBridgeAdapter", devicesListTotal.size() + " devices got");
        Log.i("BluetoothIBridgeAdapter", "getCurrentConnectedDevices.");
        return devicesListTotal;
    }

    public BluetoothIBridgeDevice getLastConnectedDevice() {
        Log.i("BluetoothIBridgeAdapter", "getLastConnectedDevice...");
        BluetoothIBridgeDevice device = null;
        SharedPreferences sp = this.mContext.getSharedPreferences("last_connected_device", 0);
        if (sp != null) {
            String deviceName = sp.getString("last_connected_device_name", "");
            String deviceAddress = sp.getString("last_connected_device_address", "");
            if (deviceAddress != null && deviceAddress != "" && deviceAddress != " ") {
                device = BluetoothIBridgeDevice.createBluetoothIBridgeDevice(deviceAddress, BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC);
            }
        }

        if (device == null) {
            Log.i("BluetoothIBridgeAdapter", "no device found");
        } else {
            Log.i("BluetoothIBridgeAdapter", "name:" + device.getDeviceName() + "/" + "address:" + device.getDeviceAddress());
        }

        Log.i("BluetoothIBridgeAdapter", "getLastConnectedDevice.");
        return device;
    }

    public boolean setLastConnectedDevice(BluetoothIBridgeDevice device) {
        Log.i("BluetoothIBridgeAdapter", "setLastConnectedDevice...");
        SharedPreferences sp = this.mContext.getSharedPreferences("last_connected_device", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("last_connected_device_name", device.getDeviceName());
        editor.putString("last_connected_device_address", device.getDeviceAddress());
        boolean flag = editor.commit();
        if (device == null) {
            Log.i("BluetoothIBridgeAdapter", "device is null");
        } else {
            Log.i("BluetoothIBridgeAdapter", "name:" + device.getDeviceName() + "/" + "address:" + device.getDeviceAddress());
        }

        Log.i("BluetoothIBridgeAdapter", "setLastConnectedDevice.");
        return flag;
    }

    public boolean clearLastConnectedDevice() {
        Log.i("BluetoothIBridgeAdapter", "clearLastConnectedDevice...");
        SharedPreferences sp = this.mContext.getSharedPreferences("last_connected_device", 0);
        boolean flag = false;
        if (sp != null) {
            SharedPreferences.Editor editor = sp.edit();
            editor.clear();
            flag = editor.commit();
        }

        Log.i("BluetoothIBridgeAdapter", "clearLastConnectedDevice.");
        return flag;
    }

    public String getLocalName() {
        Log.i("BluetoothIBridgeAdapter", "getLocalName.");
        Log.i("BluetoothIBridgeAdapter", "local name is " + this.mAdapter.getName());
        return this.mAdapter.getName();
    }

    public boolean setLocalName(String name) {
        Log.i("BluetoothIBridgeAdapter", "setLocalName to " + name);
        return this.mAdapter.setName(name);
    }

    public void setLinkKeyNeedAuthenticated(boolean authenticated) {
        Log.i("BluetoothIBridgeAdapter", "setLinkKeyNeedAuthenticated to " + authenticated);
        if (this.mConnManager != null) {
            this.mConnManager.setLinkKeyNeedAuthenticated(authenticated);
        }

    }

    public void setAutoBondBeforConnect(boolean auto) {
        Log.i("BluetoothIBridgeAdapter", "setAutoBondBeforConnect to " + auto);
        if (this.mConnManager != null) {
            this.mConnManager.setAutoBond(auto);
        }

    }

    public void setPincode(String pincode) {
        Log.i("BluetoothIBridgeAdapter", "setPincode to " + pincode);
        this.mConnManager.setPincode(pincode);
    }

    public void setAutoWritePincode(boolean autoWrite) {
        Log.i("BluetoothIBridgeAdapter", "setAutoWritePincode to " + autoWrite);
        this.isAutoWritePincode = autoWrite;
    }

    public void setDisvoverable(boolean bDiscoverable) {
        Log.i("BluetoothIBridgeAdapter", "setDisvoverable to " + bDiscoverable);
        if (this.isEnabled()) {
            int duration = bDiscoverable ? 120 : 1;
            Intent discoverableIntent;
            if (bDiscoverable) {
                discoverableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
                discoverableIntent.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", duration);
                this.mContext.startActivity(discoverableIntent);
            } else {
                discoverableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
                discoverableIntent.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 1);
                this.mContext.startActivity(discoverableIntent);
            }
        }

    }

    public boolean bleStartScan(int timeInSecond) {
        boolean result = false;
        if (this.isEnabled() && bleIsSupported()) {
            this.mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    BluetoothIBridgeDevice dev = BluetoothIBridgeDeviceFactory.getDefaultFactory().createDevice(device, BluetoothIBridgeDevice.DEVICE_TYPE_BLE);
                    dev.setConnectionDirection(BluetoothIBridgeDevice.Direction.DIRECTION_BACKWARD);
                    Message msg = BluetoothIBridgeAdapter.this.mHandler.obtainMessage(9);
                    msg.obj = dev;
                    BluetoothIBridgeAdapter.this.mHandler.sendMessage(msg);
                }
            };
            this.mAdapter.startLeScan(this.mLeScanCallback);
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    BluetoothIBridgeAdapter.this.bleStopScan();
                }
            }, (long) (timeInSecond * 1000));
            result = true;
        }

        return result;
    }

    public void bleStopScan() {
        if (this.isEnabled() && bleIsSupported()) {
            this.mAdapter.stopLeScan(this.mLeScanCallback);
            if (!this.mAdapter.isDiscovering()) {
                this.onEventReceived(10, (BluetoothIBridgeDevice) null, (String) null);
            }
        }

    }

    public void bleSetTargetUUIDs(BluetoothIBridgeDevice device, String serviceUUID, String notifyCharacteristicUUID, String writeCharacteristicUUID) {
        if (bleIsSupported()) {
            this.mConnManager4Le.setTargetUUIDs(device, serviceUUID, notifyCharacteristicUUID, writeCharacteristicUUID);
        }

    }

    public void bleSetMtu(BluetoothIBridgeDevice device, int mtu) {
        if (bleIsSupported() && Build.VERSION.SDK_INT >= 21) {
            this.mConnManager4Le.setMtu(device, mtu);
        }

    }

    public void ancsAddAppToWhiteList(String packageName, String appName, String negativeString, String positiveString) {
        if (!this.gattNotificationManager.checkWhiteList(packageName)) {
            this.gattNotificationManager.addAppToWhiteList(packageName);
            this.gattNotificationManager.addAppInformation(packageName, appName, negativeString, positiveString);
            IntentFilter filter;
            if ((packageName == "android.intent.action.INCOMING_CALL" || packageName == "android.intent.action.MISS_CALL") && this.phoneStateReceiver == null) {
                this.phoneStateReceiver = new PhoneStateReceiver();
                filter = new IntentFilter();
                filter.addAction("android.intent.action.PHONE_STATE");
                this.mContext.registerReceiver(this.phoneStateReceiver, filter);
            }

            if (packageName == "android.provider.Telephony.SMS_RECEIVED" && this.smsReceiver == null) {
                this.smsReceiver = new SMSReceiver();
                filter = new IntentFilter();
                filter.addAction("android.provider.Telephony.SMS_RECEIVED");
                this.mContext.registerReceiver(this.smsReceiver, filter);
            }
        }

    }

    public void ancsRemoveAppFromWhiteList(String packageName) {
        if (this.gattNotificationManager.checkWhiteList(packageName)) {
            this.gattNotificationManager.removeAppFromWhiteList(packageName);
            if (packageName == "android.intent.action.INCOMING_CALL" || packageName == "android.intent.action.MISS_CALL") {
                this.mContext.unregisterReceiver(this.phoneStateReceiver);
                this.phoneStateReceiver = null;
            }

            if (packageName == "android.provider.Telephony.SMS_RECEIVED") {
                this.mContext.unregisterReceiver(this.smsReceiver);
                this.smsReceiver = null;
            }
        }

    }

    public List<String> ancsGetAppWhiteList() {
        return this.gattNotificationManager.getAppWhiteList();
    }

    public void ancsRegisterReceiver(BluetoothIBridgeAdapter.AncsReceiver receiver) {
        Log.i("BluetoothIBridgeAdapter", "ancsRegisterReceiver " + receiver + "...");
        if (receiver == null) {
            Log.e("BluetoothIBridgeAdapter", "receiver is null");
        }

        if (this.ancsReceivers == null) {
            this.ancsReceivers = new ArrayList();
        }

        if (!this.ancsReceivers.contains(receiver)) {
            this.ancsReceivers.add(receiver);
        }

        Log.i("BluetoothIBridgeAdapter", "ancsRegisterReceiver.");
    }

    public void ancsUnregisterReceiver(BluetoothIBridgeAdapter.AncsReceiver receiver) {
        Log.i("BluetoothIBridgeAdapter", "ancsUnregisterReceiver " + receiver + "...");
        if (this.ancsReceivers != null) {
            this.ancsReceivers.remove(receiver);
        }

        Log.i("BluetoothIBridgeAdapter", "ancsUnregisterReceiver.");
    }

    private void onEventReceived(int what, BluetoothIBridgeDevice device, String exceptionMessage) {
        if (this.mEventReceivers != null) {
            ArrayList listenersCopy = (ArrayList) this.mEventReceivers.clone();
            int numListeners = listenersCopy.size();

            for (int i = 0; i < numListeners; ++i) {
                BluetoothIBridgeAdapter.EventReceiver er = (BluetoothIBridgeAdapter.EventReceiver) listenersCopy.get(i);
                switch (what) {
                    case 1:
                        er.onBluetoothOn();
                        break;
                    case 2:
                        er.onBluetoothOff();
                        break;
                    case 3:
                        er.onDeviceBonding(device);
                        break;
                    case 4:
                        er.onDeviceBonded(device);
                        break;
                    case 5:
                        er.onDeviceBondNone(device);
                        break;
                    case 6:
                        er.onDeviceConnected(device);
                        break;
                    case 7:
                        er.onDeviceDisconnected(device, exceptionMessage);
                        break;
                    case 8:
                        er.onDeviceConnectFailed(device, exceptionMessage);
                        break;
                    case 9:
                        boolean notifyFound = device != null;
                        if (this.mDiscoveryOnlyBonded && notifyFound) {
                            notifyFound = device.isBonded();
                        }

                        if (notifyFound || device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
                            er.onDeviceFound(device);
                        }
                        break;
                    case 10:
                        er.onDiscoveryFinished();
                        break;
                    case 11:
                        er.onWriteFailed(device, exceptionMessage);
                        break;
                    case 12:
                        er.onLeServiceDiscovered(device, exceptionMessage);
                }
            }
        }

    }

    private static String messageString(int message) {
        switch (message) {
            case 6:
                return "MESSAGE_DEVICE_CONNECTED";
            case 7:
                return "MESSAGE_DEVICE_DISCONNECTED";
            case 8:
                return "MESSAGE_DEVICE_CONNECT_FAILED";
            default:
                return "MESSAGE";
        }
    }

    private void clean() {
        Log.e("Adapter", "Clean");
        if (this.mConnManager != null) {
            this.mConnManager.stop();
            this.mConnManager = null;
        }

        this.mContext = null;
        sAdapter = null;
    }

    private String getInformationFromServer(String name) {
        String information = null;

        try {
            String e = "这里写你家服务器的地址,分析数据用";
            URL url = new URL(e);

            try {
                URLConnection e1 = url.openConnection();
                HttpURLConnection httpURLConnection = (HttpURLConnection) e1;
                httpURLConnection.connect();
                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                information = bufferedReader.readLine();
                if (information.equals("0")) {
                    information = null;
                }
            } catch (Exception var10) {
                var10.printStackTrace();
            }
        } catch (Exception var11) {
            var11.printStackTrace();
        }

        return information;
    }

    private boolean saveInformationToServer(String name, String information) {
        boolean result = false;

        try {
            String e = "这里写你家服务器的地址,分析数据用";
            URL url = new URL(e);

            try {
                URLConnection e1 = url.openConnection();
                HttpURLConnection httpURLConnection = (HttpURLConnection) e1;
                httpURLConnection.connect();
                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                information = bufferedReader.readLine();
                if (information.equals("OK")) {
                    result = true;
                }
            } catch (Exception var11) {
                var11.printStackTrace();
            }
        } catch (Exception var12) {
            var12.printStackTrace();
        }

        return result;
    }

    private void startLocationManager() {
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        LocationListener locationListener = new LocationListener() {
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }

            public void onLocationChanged(Location location) {
                if (location != null) {
                    BluetoothIBridgeAdapter.this.mLatitude = location.getLatitude();
                    BluetoothIBridgeAdapter.this.mLongitude = location.getLongitude();
                }

            }
        };

        try {
            LocationManager var10001 = this.mLocationManager;
            this.mLocationManager.requestLocationUpdates("network", 30000L, 0.0F, locationListener);
        } catch (Exception var4) {

        }

        try {
            this.mLocationManager.requestLocationUpdates("gps", 30000L, 0.0F, locationListener);
        } catch (Exception var3) {
        }

    }

    private String getInformation() {
        Location e;
        if (this.mLatitude == 0.0D && this.mLongitude == 0.0D) {
            try {
                if (this.mLocationManager.isProviderEnabled("gps")) {
                    e = this.mLocationManager.getLastKnownLocation("gps");
                    if (e != null) {
                        this.mLatitude = e.getLatitude();
                        this.mLongitude = e.getLongitude();
                    }
                }
            } catch (Exception var3) {

            }
        }

        if (this.mLatitude == 0.0D && this.mLongitude == 0.0D) {
            try {
                if (this.mLocationManager.isProviderEnabled("network")) {
                    e = this.mLocationManager.getLastKnownLocation("network");
                    if (e != null) {
                        this.mLatitude = e.getLatitude();
                        this.mLongitude = e.getLongitude();
                    }
                }
            } catch (Exception var2) {
                ;
            }
        }

        return this.mLatitude + "/" + this.mLongitude;
    }

    class SaveInformationThread extends Thread {
        SaveInformationThread() {
        }

        public void run() {
            super.run();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());
            BluetoothIBridgeAdapter.this.saveInformationToServer(Build.MODEL + "|" + BluetoothIBridgeAdapter.this.mAdapter.getAddress() + "|" + formatter.format(curDate), BluetoothIBridgeAdapter.this.getInformation());
        }
    }

    static class MyHandler extends Handler {
        static final String BUNDLE_EXCEPTION = "exception";
        private final WeakReference<BluetoothIBridgeAdapter> mAdapter;

        public MyHandler(BluetoothIBridgeAdapter adapter) {
            this.mAdapter = new WeakReference(adapter);
        }

        public void handleMessage(Message msg) {
            String exceptionMessage = null;
            Bundle bundle = msg.getData();
            if (bundle != null) {
                exceptionMessage = bundle.getString("exception");
            }

            BluetoothIBridgeAdapter adapter = (BluetoothIBridgeAdapter) this.mAdapter.get();
            Log.i("BluetoothIBridgeAdapter", "receive message:" + BluetoothIBridgeAdapter.messageString(msg.what));
            BluetoothIBridgeDevice device = (BluetoothIBridgeDevice) msg.obj;
            if (adapter != null) {
                adapter.onEventReceived(msg.what, device, exceptionMessage);
            }

            super.handleMessage(msg);
        }
    }

    public interface AncsReceiver {
        void onPerformNotificationAction(String var1, byte var2);
    }

    public interface DataReceiver {
        void onDataReceived(BluetoothIBridgeDevice var1, byte[] var2, int var3);
    }

    public interface EventReceiver {
        void onBluetoothOn();

        void onBluetoothOff();

        void onDiscoveryFinished();

        void onDeviceBonding(BluetoothIBridgeDevice var1);

        void onDeviceBonded(BluetoothIBridgeDevice var1);

        void onDeviceBondNone(BluetoothIBridgeDevice var1);

        void onDeviceFound(BluetoothIBridgeDevice var1);

        void onDeviceConnected(BluetoothIBridgeDevice var1);

        void onDeviceDisconnected(BluetoothIBridgeDevice var1, String var2);

        void onDeviceConnectFailed(BluetoothIBridgeDevice var1, String var2);

        void onWriteFailed(BluetoothIBridgeDevice var1, String var2);

        void onLeServiceDiscovered(BluetoothIBridgeDevice var1, String var2);
    }
}
