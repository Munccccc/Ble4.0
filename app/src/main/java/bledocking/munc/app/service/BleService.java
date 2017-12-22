package bledocking.munc.app.service;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import bledocking.munc.app.MainActivity;
import bledocking.munc.app.ble.Ancs.AncsUtils;
import bledocking.munc.app.ble.BluetoothIBridgeAdapter;
import bledocking.munc.app.ble.BluetoothIBridgeDevice;


/**
 * Created by GD on 2017/12/18.
 */

public class BleService extends Service implements BluetoothIBridgeAdapter.EventReceiver {
    private BluetoothIBridgeAdapter mAdapter;
    private IBinder mBinder = new LocalBinder();
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        mAdapter = BluetoothIBridgeAdapter.sharedInstance(this);
        if (!mAdapter.isEnabled()) {
            mAdapter.setEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= 10) {
            mAdapter.setLinkKeyNeedAuthenticated(false);
        } else {
            mAdapter.setLinkKeyNeedAuthenticated(true);
        }
        mAdapter.registerEventReceiver(this);
        acquireWakeLock();
        android.util.Log.i("TestService", "onCreate");

        mAdapter.ancsAddAppToWhiteList(AncsUtils.APP_PACKAGE_NAME_INCOMING_CALL, "Incoming Call", "refuse", "accept");
        mAdapter.ancsAddAppToWhiteList(AncsUtils.APP_PACKAGE_NAME_MISS_CALL, "Miss Call", "clear", "dial");
        mAdapter.ancsAddAppToWhiteList(AncsUtils.APP_PACKAGE_NAME_SMS, "SMS", "Clear", null);
        mAdapter.ancsRegisterReceiver(new BluetoothIBridgeAdapter.AncsReceiver() {
            @SuppressLint("LongLogTag")
            @Override
            public void onPerformNotificationAction(String appIdentifier, byte actionID) {
                Log.i("onPerformNotificationAction", appIdentifier + ":" + actionID);
                if (appIdentifier.equals(AncsUtils.APP_PACKAGE_NAME_INCOMING_CALL)) {
                    if (actionID == AncsUtils.ACTION_ID_POSITIVE) {
                        showToast("accept incoming call");
                        android.util.Log.i("TestService", "accept incoming call here");
                    } else if (actionID == AncsUtils.ACTION_ID_NEGATICE) {
                        showToast("refuse incoming call");
                        android.util.Log.i("TestService", "refuse incoming call here");
                    }
                }
                if (appIdentifier.equals(AncsUtils.APP_PACKAGE_NAME_MISS_CALL)) {
                    if (actionID == AncsUtils.ACTION_ID_POSITIVE) {
                        showToast("dial");
                        android.util.Log.i("TestService", "dial");
                    } else if (actionID == AncsUtils.ACTION_ID_NEGATICE) {
                        showToast("clear");
                        android.util.Log.i("TestService", "clear");
                    }
                } else if (appIdentifier.equals(AncsUtils.APP_PACKAGE_NAME_SMS)) {
                    if (actionID == AncsUtils.ACTION_ID_NEGATICE) {
                        showToast("clear");
                        android.util.Log.i("TestService", "clear");
                    }
                } else {
                    if (actionID == AncsUtils.ACTION_ID_NEGATICE) {
                        showToast("clear");
                        android.util.Log.i("TestService", "clear");
                    } else if (actionID == AncsUtils.ACTION_ID_POSITIVE) {
                        PackageManager packageManager = getPackageManager();
                        Intent intent = new Intent();
                        intent = packageManager.getLaunchIntentForPackage(appIdentifier);
                        if (intent != null) {
                            startActivity(intent);
                        } else {
                            android.util.Log.i("TestService", "APP not found!");
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        android.util.Log.i("TestService", "onDestroy");
        releaseWakeLock();
        mAdapter.unregisterEventReceiver(this);
        mAdapter.destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        BluetoothIBridgeAdapter getBluetoothAdapter() {
            return mAdapter;
        }
    }

    private void showToast(String msg) {
        ToastThread toastThread = new ToastThread(msg);
        toastThread.start();
    }

    private class ToastThread extends Thread {
        private String msg;

        public ToastThread(String msg) {
            this.msg = msg;
        }

        public void run() {
            Looper.prepare();
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.util.Log.i("TestService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDeviceConnectFailed(BluetoothIBridgeDevice device, String exceptionMsg) {
    }

    @Override
    public void onDeviceConnected(BluetoothIBridgeDevice device) {
        ComponentName comp = new ComponentName(getPackageName(), MainActivity.class.getName());
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(comp);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("com.ivt.bleSpp.device", device);
        startActivity(intent);
    }

    @Override
    public void onDeviceDisconnected(BluetoothIBridgeDevice device, String exceptionMsg) {
        stopForeground(true);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(0x37512433);
    }

    @Override
    public void onBluetoothOn() {
    }

    @Override
    public void onBluetoothOff() {
    }

    @Override
    public void onDeviceBonding(BluetoothIBridgeDevice device) {
    }

    @Override
    public void onDeviceBonded(BluetoothIBridgeDevice device) {
    }

    @Override
    public void onDeviceBondNone(BluetoothIBridgeDevice device) {
    }

    @Override
    public void onDeviceFound(BluetoothIBridgeDevice device) {
    }

    @Override
    public void onDiscoveryFinished() {
    }

    @Override
    public void onWriteFailed(BluetoothIBridgeDevice deivce, String exceptionMsg) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onLeServiceDiscovered(BluetoothIBridgeDevice device, String exceptionMsg) {

    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this.getClass().getCanonicalName());
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}
