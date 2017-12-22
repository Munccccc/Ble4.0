package bledocking.munc.app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.UnsupportedEncodingException;
import java.util.List;

import bledocking.munc.app.ble.BluetoothIBridgeAdapter;
import bledocking.munc.app.ble.BluetoothIBridgeDevice;
import bledocking.munc.app.service.ServiceBinder;
import bledocking.munc.app.util.LockData;
import bledocking.munc.app.util.Utils;
import butterknife.BindView;
import butterknife.ButterKnife;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


/**
 * 这里是连接蓝牙BLE 4.0以上的方式 传输和链接的报文需要你和写C的程序员协商好 必须有一个完整的通讯协议  当时这里是发送的十六进制报文作为交互
 */
public class MainActivity extends AppCompatActivity implements BluetoothIBridgeAdapter.DataReceiver, BluetoothIBridgeAdapter.EventReceiver, EasyPermissions.PermissionCallbacks {

    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;
    @BindView(R.id.btstart)
    Button btstart;
    @BindView(R.id.btstop)
    Button btstop;
    @BindView(R.id.bar)
    ProgressBar bar;
    @BindView(R.id.activity_main)
    LinearLayout activityMain;
    private ServiceBinder mBinder;
    protected Context context;
    private BluetoothIBridgeDevice mDevice;


    private byte[] packetData;
    LockData lockData = new LockData();
    private String openDoor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        //初始化蓝牙的一些动作
        requestCodeQRCodePermissions();
        initBlueTooth();
        lockData.setCommand((byte) 0x91);
        lockData.setData(new byte[]{(byte) 0x00, (byte) 0x00});
        packetData = lockData.getPacketData();
        lockData.setAuthCode("A" + "B" + "C" + "D" + "E" + "F" + "G" + "H" + "I" + "J" + "K" + "L" + "M" + "N" + "O" + "P");
//        openDoor = "$" + "$" + "91" + "FE" + "41" + "42" + "43" + "44" + "45" + "46" + "47" + "48" + "49" + "4A" + "4B" + "4C" + "4D" + "4E" + "4F" + "50" + "00" + "01" + "00" + "02" + "00" + "00" + "7C" + "#";
        btstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开锁
                if (null != mDevice && mAdapter != null
                        && mDevice.isConnected()) {
                    mAdapter.send(mDevice, packetData, packetData.length);
                    Log.e("data", packetData.toString());
                }
            }
        });
        //关锁
        btstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
      /*          lockData.setCommand((byte) 0x92);
                lockData.setData(new byte[]{(byte) 0x00, (byte) 0x00});
                packetData = lockData.getPacketData();*/
                mAdapter.send(mDevice, packetData, packetData.length);
            }
        });

    }

    private void initBlueTooth() {
        if (null != mAdapter) {
            return;
        }
        mBinder = new ServiceBinder(this);
        mBinder.registerBluetoothAdapterListener(serviceListener);
        mBinder.doBindService();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    mAdapter.setEnabled(true);
                }
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    //refreshScreen();
                }
            }
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            }
            if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            }
        }

    };
    public static final int MSG_RECEIVED_STRING = 0x01;
    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECEIVED_STRING:
                    String result = (String) msg.obj;
                    Log.e("blueTRe", result);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }

    };
    private BluetoothIBridgeAdapter mAdapter;
    private ServiceBinder.BluetoothAdapterListener serviceListener = new ServiceBinder.BluetoothAdapterListener() {


        @Override
        public void onBluetoothAdapterDestroyed() {
            mAdapter.unregisterDataReceiver(MainActivity.this);
            mAdapter.unregisterEventReceiver(MainActivity.this);
            mAdapter = null;
        }

        @Override
        public void onBluetoothAdapterCreated(BluetoothIBridgeAdapter adapter) {
            if (adapter != null) {
                mAdapter = adapter;
                mAdapter.bleStartScan(10);
                mAdapter.registerDataReceiver(MainActivity.this);
                mAdapter.registerEventReceiver(MainActivity.this);
            }
        }
    };

    @Override
    public void onDataReceived(BluetoothIBridgeDevice bluetoothIBridgeDevice, byte[] bytes, int len) {
        String result = "";
        try {
            result = new String(bytes, 0, len, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        result = StringUtils.bytesToHexString(bytes).trim();
        myHandler.obtainMessage(MSG_RECEIVED_STRING, result).sendToTarget();
    }

    @Override
    public void onBluetoothOn() {

    }

    @Override
    public void onBluetoothOff() {

    }

    @Override
    public void onDiscoveryFinished() {

    }

    @Override
    public void onDeviceBonding(BluetoothIBridgeDevice bluetoothIBridgeDevice) {

    }

    @Override
    public void onDeviceBonded(BluetoothIBridgeDevice bluetoothIBridgeDevice) {

    }

    @Override
    public void onDeviceBondNone(BluetoothIBridgeDevice bluetoothIBridgeDevice) {

    }

    @Override
    public void onDeviceFound(BluetoothIBridgeDevice device) {
        //在某一个界面存值  拿到服务器传的deviceName设备名称 每次用完要清空首选项
//        SPUtil.putObject(getApplicationContext(), SPConstant.BLUET_NAME,deviceName);
        //在这里取值
//        String deviceName = SPUtil.getString(this, SPConstant.BLUET_NAME);
        String deviceName = "JMEV200";

        if (TextUtils.isEmpty(deviceName)) {
            Log.e("blueT", "deviceName为空");
            return;
        }
        if (null != device.getDeviceName() && device.getDeviceName().equals(deviceName)
                && device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
            mAdapter.connectDevice(device);
            Log.e("blueT", "开始连接");
            mAdapter.bleStopScan();
        }
        Log.e("blueT", "Device名字" + device.getDeviceName() + "服务器deviceName" + deviceName);
    }

    @Override
    public void onDeviceConnected(BluetoothIBridgeDevice device) {
        if (null != mDevice && mDevice.isConnected()) {
            Log.e("blueT", "device已经连接过了");
            mAdapter.bleStopScan();
        } else {
            mDevice = device;
            //连接完成 开始 发送指令
        }
        Log.e("blueT", "device初次连接");
    }


    @Override
    public void onDeviceDisconnected(BluetoothIBridgeDevice bluetoothIBridgeDevice, String s) {

    }

    @Override
    public void onDeviceConnectFailed(BluetoothIBridgeDevice bluetoothIBridgeDevice, String s) {

    }

    @Override
    public void onWriteFailed(BluetoothIBridgeDevice bluetoothIBridgeDevice, String s) {

    }

    @Override
    public void onLeServiceDiscovered(BluetoothIBridgeDevice bluetoothIBridgeDevice, String s) {

    }

    /**
     * 蓝牙申请的权限
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == REQUEST_CODE_QRCODE_PERMISSIONS) {
            if (perms.size() == 2) {
                if (perms.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Utils.showToast(MainActivity.this, "权限获取成功");
                }
            }

        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Utils.showToast(MainActivity.this, "权限获取失败");
    }

    @AfterPermissionGranted(REQUEST_CODE_QRCODE_PERMISSIONS)
    private void requestCodeQRCodePermissions() {
        String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        if (!EasyPermissions.hasPermissions(MainActivity.this, perms)) {
            EasyPermissions.requestPermissions(this, "需要打开蓝牙的权限", REQUEST_CODE_QRCODE_PERMISSIONS, perms);
        } else {
            Utils.showToast(MainActivity.this, "权限获取成功");
        }
    }
}
