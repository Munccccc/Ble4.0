
package bledocking.munc.app.ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import bledocking.munc.app.ble.util.SystemUtils;

final class BluetoothIBridgeConnManager implements BluetoothIBridgeConnectionListener.ConnectionReceiver {
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final BluetoothIBridgeAdapter.MyHandler mHandler;
    private BluetoothIBridgeConnManager.ConnectThread mConnectThread;
    private BluetoothIBridgeConnectionListener mListener;
    private BluetoothIBridgeConnections mBluetoothIBridgeConnections;
    private String mPincode = "1234";
    private boolean auth = true;
    private boolean autoPair = true;
    String lastExceptionMsg = null;

    protected BluetoothIBridgeConnManager(Context context, BluetoothIBridgeAdapter.MyHandler handler) {
        this.mHandler = handler;
        this.mBluetoothIBridgeConnections = new BluetoothIBridgeConnections(handler);
    }

    protected synchronized void start() {
        if(this.mListener == null) {
            this.mListener = new BluetoothIBridgeConnectionListener(this, this.auth);
        }

        this.mListener.start();
        if(this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

    }

    protected synchronized void stop() {
        if(this.mListener != null) {
            this.mListener.stop();
            this.mListener = null;
        }

        if(this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if(this.mBluetoothIBridgeConnections != null) {
            this.mBluetoothIBridgeConnections.disconnectAll();
        }

    }

    protected void registerDataReceiver(BluetoothIBridgeAdapter.DataReceiver receiver) {
        this.mBluetoothIBridgeConnections.registerDataReceiver(receiver);
    }

    protected void unregisterDataReceiver(BluetoothIBridgeAdapter.DataReceiver receiver) {
        this.mBluetoothIBridgeConnections.unregisterDataReceiver(receiver);
    }

    synchronized void connect(BluetoothIBridgeDevice device, int bondTime) {
        Log.i("ConnManager", "connect...");
        if(this.mConnectThread != null) {
            Log.i("ConnManager", "cancel previous connecting");
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        device.setBondStatus();
        Log.i("ConnManager", "autoPair = " + this.autoPair + " bond status = " + device.getBondStatus());
        if(this.autoPair && device.getBondStatus().equals(BluetoothIBridgeDevice.BondStatus.STATE_BONDNONE)) {
            Log.i("ConnManager", "set bond status to bonding");
            device.setBondStatus(BluetoothIBridgeDevice.BondStatus.STATE_BONDING);
        }

        if(device != null && !device.isConnected()) {
            Log.i("ConnManager", "set connect status to connecting");
            device.setConnectStatus(BluetoothIBridgeDevice.ConnectStatus.STATUS_CONNECTTING);
            Log.i("ConnManager", "create thread to connect");
            this.mConnectThread = new BluetoothIBridgeConnManager.ConnectThread(device, bondTime);
            this.mConnectThread.start();
        } else {
            Log.e("ConnManager", "device is connected or is null");
        }

        Log.i("ConnManager", "connect.");
    }

    synchronized void cancelBond() {
        if(this.mConnectThread != null) {
            this.mConnectThread.cancelBondProcess();
        }

    }

    synchronized void disconnect(BluetoothIBridgeDevice device) {
        Log.i("ConnManager", "disconnect...");
        this.mBluetoothIBridgeConnections.disconnect(device);
        Log.i("ConnManager", "disconnect.");
    }

    void write(BluetoothIBridgeDevice device, byte[] buffer, int length) {
        this.mBluetoothIBridgeConnections.write(device, buffer, length);
    }

    List<BluetoothIBridgeDevice> getCurrentConnectedDevice() {
        List devicesList = null;
        devicesList = this.mBluetoothIBridgeConnections.getCurrentConnectedDevice();
        return devicesList;
    }

    void setPincode(String pincode) {
        this.mPincode = pincode;
    }

    void setAutoBond(boolean auto) {
        this.autoPair = auto;
    }

    void setLinkKeyNeedAuthenticated(boolean authenticated) {
        if(this.mListener != null) {
            this.auth = authenticated;
            this.mListener.setLinkKeyNeedAuthenticated(authenticated);
        }

    }

    private void connectionFailed(BluetoothIBridgeDevice device, String exceptionMsg) {
        if(device != null) {
            device.connected(false);
            device.setConnectStatus(BluetoothIBridgeDevice.ConnectStatus.STATUS_CONNECTFAILED);
        }

        Message msg = this.mHandler.obtainMessage(8);
        msg.obj = device;
        Bundle bundle = new Bundle();
        bundle.putString("exception", exceptionMsg);
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
        synchronized(this) {
            this.mConnectThread = null;
        }
    }

    void onPairingRequested(BluetoothIBridgeDevice device, int type, int pairingKey) {
        String mPairingKey;
        switch(type) {
            case 0:
                device.setPin(this.mPincode.getBytes());
            case 1:
            default:
                break;
            case 2:
            case 3:
                device.setPairingConfirmation(true);
                break;
            case 4:
                mPairingKey = String.format("%06d", new Object[]{Integer.valueOf(pairingKey)});
                device.setPairingConfirmation(true);
                break;
            case 5:
                mPairingKey = String.format("%04d", new Object[]{Integer.valueOf(pairingKey)});
                device.setPin(mPairingKey.getBytes());
        }

    }

    public void onConnectionEstablished(BluetoothSocket socket) {
        BluetoothIBridgeDevice device = BluetoothIBridgeDeviceFactory.getDefaultFactory().createDevice(socket.getRemoteDevice(), BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC);
        if(device != null) {
            device.setConnectionDirection(BluetoothIBridgeDevice.Direction.DIRECTION_BACKWARD);
            device.setBondStatus();
        }

        this.mBluetoothIBridgeConnections.connected(socket, device);
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothIBridgeDevice mmDevice;
        private final String name;
        private final int mmBondTime;
        private boolean cancleBond = false;

        protected ConnectThread(BluetoothIBridgeDevice device, int bondTime) {
            this.mmDevice = device;
            this.name = device.getDeviceName();
            this.mmBondTime = bondTime;
        }

        public void run() {
            Log.i("ConnManager", "connect thread run...");
            this.setName("ConnectThread" + this.name);
            if(BluetoothIBridgeConnManager.this.mAdapter.isDiscovering()) {
                Log.i("ConnManager", "cancel previous discovering");
                BluetoothIBridgeConnManager.this.mAdapter.cancelDiscovery();
            }

            if(this.mmDevice != null) {
                this.mmDevice.setConnectStatus(BluetoothIBridgeDevice.ConnectStatus.STATUS_CONNECTTING);
            } else {
                Log.e("ConnManager", "device is null");
            }

            if(BluetoothIBridgeConnManager.this.autoPair) {
                Log.i("ConnManager", "auto pair is enable");
                Log.i("ConnManager", "do bond process");
                this.doBondProcess();
            }

            Log.i("ConnManager", "connect refcomm socket");
            boolean connectResult = this.connectRfcommSocket();
            BluetoothIBridgeConnManager errorString;
            if(!connectResult) {
                if(this.mmDevice.getBondStatus().equals(BluetoothIBridgeDevice.BondStatus.STATE_BONDED)) {
                    try {
                        try {
                            sleep(300L);
                        } catch (InterruptedException var8) {
                            var8.printStackTrace();
                        }

                        if(this.mmSocket != null) {
                            this.mmSocket.close();
                        }
                    } catch (IOException var9) {
                        Log.e("ConnManager", "unable to close socket:" + var9.getMessage());
                    }

                    Log.i("ConnManager", "connect with channel 6");
                    connectResult = this.connectWithChannel(6);
                }

                if(!connectResult) {
                    errorString = null;

                    try {
                        try {
                            sleep(300L);
                        } catch (InterruptedException var5) {
                            var5.printStackTrace();
                        }

                        if(this.mmSocket != null) {
                            this.mmSocket.close();
                        }
                    } catch (IOException var6) {
                        Log.e("ConnManager", "unable to close socket:" + var6.getMessage());
                    }

                    BluetoothIBridgeConnManager.this.connectionFailed(this.mmDevice, BluetoothIBridgeConnManager.this.lastExceptionMsg);
                    Log.i("ConnManager", "connect thread run.");
                    return;
                }
            }

            errorString = BluetoothIBridgeConnManager.this;
            synchronized(BluetoothIBridgeConnManager.this) {
                BluetoothIBridgeConnManager.this.mConnectThread = null;
            }

            if(this.mmDevice != null) {
                this.mmDevice.setConnectionDirection(BluetoothIBridgeDevice.Direction.DIRECTION_FORWARD);
                this.mmDevice.setBondStatus();
            }

            BluetoothIBridgeConnManager.this.mBluetoothIBridgeConnections.connected(this.mmSocket, this.mmDevice);
            Log.i("ConnManager", "connected");
            Log.i("ConnManager", "connect thread run.");
        }

        private boolean connectRfcommSocket() {
            boolean result = false;
            int max_retry_count = 2;
            Log.i("ConnManager", "connectRfcommSocket...");
            this.mmSocket = this.mmDevice.createSocket();
            if(SystemUtils.isMediatekPlatform()) {
                try {
                    Log.i("ConnManager", "it is MTK platform");
                    sleep(3000L);
                } catch (InterruptedException var6) {
                    var6.printStackTrace();
                }
            }

            while(true) {
                try {
                    if(this.mmSocket != null) {
                        Log.i("ConnManager", "socket connect");
                        this.mmSocket.connect();
                        result = true;
                    } else {
                        Log.e("ConnManager", "socket is null");
                        BluetoothIBridgeConnManager.this.lastExceptionMsg = "socket is null";
                    }
                } catch (IOException var7) {
                    result = false;
                    if(var7.getMessage() != null && var7.getMessage().equals("Service discovery failed")) {
                        Log.e("ConnManager", "no service found");
                        if(max_retry_count > 0) {
                            Log.i("ConnManager", "retry");
                            --max_retry_count;

                            try {
                                sleep(300L);
                            } catch (InterruptedException var5) {
                                var5.printStackTrace();
                            }
                            continue;
                        }

                        Log.e("ConnManager", "max retry count reached");
                        BluetoothIBridgeConnManager.this.lastExceptionMsg = var7.getMessage();
                        break;
                    }

                    Log.e("ConnManager", "connect failed");
                    if(var7.getMessage() != null) {
                        Log.e("ConnManager", "error is " + var7.getMessage());
                        BluetoothIBridgeConnManager.this.lastExceptionMsg = var7.getMessage();
                    }
                }
                break;
            }

            Log.i("ConnManager", "connectRfcommSocket.");
            return result;
        }

        private boolean connectWithChannel(int channel) {
            boolean result = false;
            Log.i("ConnManager", "connectWithChannel " + channel + "...");
            this.mmSocket = this.mmDevice.createSocketWithChannel(channel);

            try {
                this.mmSocket.connect();
                result = true;
            } catch (IOException var4) {
                result = false;
                Log.e("ConnManager", "connect failed");
                if(var4.getMessage() != null) {
                    Log.e("ConnManager", "error is " + var4.getMessage());
                    BluetoothIBridgeConnManager.this.lastExceptionMsg = var4.getMessage();
                }
            }

            Log.i("ConnManager", "connectWithChannel.");
            return result;
        }

        private void doBondProcess() {
            boolean isPaired = false;
            boolean bonding = false;
            int during = 0;
            Log.i("ConnManager", "doBondProcess...");

            for(; !this.cancleBond && !isPaired && during < this.mmBondTime * 2; ++during) {
                BluetoothDevice device = BluetoothIBridgeConnManager.this.mAdapter.getRemoteDevice(this.mmDevice.getDeviceAddress());
                if(device.getBondState() == 12) {
                    Log.i("ConnManager", "bond status is bonded");
                    isPaired = true;
                    bonding = false;
                    this.mmDevice.setBondStatus(BluetoothIBridgeDevice.BondStatus.STATE_BONDED);
                    break;
                }

                if(device.getBondState() == 11) {
                    Log.i("ConnManager", "bond status is bonding");
                    this.mmDevice.setBondStatus(BluetoothIBridgeDevice.BondStatus.STATE_BONDING);
                } else if(device.getBondState() == 10) {
                    Log.i("ConnManager", "bond status is none");

                    try {
                        if(!bonding) {
                            Log.i("ConnManager", "start bond device");
                            this.mmDevice.createBond();
                            bonding = true;
                            this.mmDevice.setBondStatus(BluetoothIBridgeDevice.BondStatus.STATE_BONDING);
                        } else {
                            Log.i("ConnManager", "bond failed");
                            this.mmDevice.setBondStatus(BluetoothIBridgeDevice.BondStatus.STATE_BONDFAILED);
                            bonding = false;
                        }
                    } catch (Exception var7) {
                        var7.printStackTrace();
                    }
                }

                try {
                    sleep(500L);
                } catch (InterruptedException var6) {
                    var6.printStackTrace();
                }
            }

            if(this.cancleBond) {
                Log.i("ConnManager", "bond canceled");
                this.mmDevice.setBondStatus(BluetoothIBridgeDevice.BondStatus.STATE_BOND_CANCLED);
            } else if(!isPaired && during >= this.mmBondTime) {
                Log.i("ConnManager", "bond timeout");
                this.mmDevice.setBondStatus(BluetoothIBridgeDevice.BondStatus.STATE_BOND_OVERTIME);
            }

            Log.i("ConnManager", "doBondProcess.");
        }

        void cancel() {
            Log.i("ConnManager", "cancel...");

            try {
                if(this.mmSocket != null) {
                    this.mmSocket.close();
                }
            } catch (IOException var2) {
                Log.e("ConnManager", "close() of connect " + this.name + " socket failed", var2);
            }

            Log.i("ConnManager", "cancel.");
        }

        void cancelBondProcess() {
            Log.i("ConnManager", "cancelBondProcess...");
            this.cancleBond = true;
            Log.i("ConnManager", "cancelBondProcess.");
        }
    }
}
