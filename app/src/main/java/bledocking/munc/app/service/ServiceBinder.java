package bledocking.munc.app.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

import bledocking.munc.app.ble.BluetoothIBridgeAdapter;


public class ServiceBinder {

	private boolean mIsBound;
	private Context mContext;
	private List<BluetoothAdapterListener> mListeners;

	public ServiceBinder(Context context) {
		mContext = context;
		mListeners = new ArrayList<BluetoothAdapterListener>();
	}

	public void doBindService() {
		mContext.bindService(new Intent(mContext, BleService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			mContext.unbindService(mConnection);
			mIsBound = false;
		}
	}

	public void registerBluetoothAdapterListener(
			BluetoothAdapterListener listener) {
		synchronized (mListeners) {
			mListeners.add(listener);
		}
	}

	public void unregisterBluetoothAdapterListener(
			BluetoothAdapterListener listener) {
		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}

	public interface BluetoothAdapterListener {
		void onBluetoothAdapterCreated(BluetoothIBridgeAdapter adapter);

		void onBluetoothAdapterDestroyed();
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			BluetoothIBridgeAdapter adapter = ((BleService.LocalBinder) service)
					.getBluetoothAdapter();
			for (BluetoothAdapterListener l : mListeners) {
				if (l != null) {
					l.onBluetoothAdapterCreated(adapter);
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			for (BluetoothAdapterListener l : mListeners) {
				if (l != null) {
					l.onBluetoothAdapterDestroyed();
				}
			}
		}
	};


}
