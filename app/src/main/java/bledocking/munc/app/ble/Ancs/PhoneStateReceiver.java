
package bledocking.munc.app.ble.Ancs;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PhoneStateReceiver extends BroadcastReceiver {
    private int previousPhoneState = -1;
    private int incomingCallNotificationUID = -1;

    public PhoneStateReceiver() {
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i("PhoneStateReceiver", "[Broadcast]" + action);
        this.doReceivePhone(context, intent);
    }

    public void doReceivePhone(Context context, Intent intent) {
        String phoneNumber = intent.getStringExtra("incoming_number");
        int state = ((TelephonyManager)context.getSystemService("phone")).getCallState();
        AppInformation incomingCallAppInformation = GattNotificationManager.sharedInstance().getAppInformation("android.intent.action.INCOMING_CALL");
        AppInformation missCallAppInformation = GattNotificationManager.sharedInstance().getAppInformation("android.intent.action.MISS_CALL");
        GattNotification notification;
        switch(state) {
            case 0:
                Log.i("PhoneStateReceiver", "[Broadcast]电话挂断=" + phoneNumber);
                if(missCallAppInformation != null && this.previousPhoneState == 1) {
                    GattNotificationManager.sharedInstance().removeNotification(this.incomingCallNotificationUID);
                    notification = new GattNotification();
                    notification.eventID = 0;
                    notification.eventFlags = 24;
                    notification.categoryID = 2;
                    if(phoneNumber != null) {
                        notification.addAttribute((byte)1, phoneNumber.getBytes());
                    }

                    SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd\'T\'HHmmSS");
                    Date curDate = new Date(System.currentTimeMillis());
                    String date = formatter1.format(curDate);
                    notification.addAttribute((byte)5, date.getBytes());
                    String missCall = missCallAppInformation.displayName;
                    notification.addAttribute((byte)4, String.format("%d", new Object[]{Integer.valueOf(missCall.length())}).getBytes());
                    notification.addAttribute((byte)3, missCall.getBytes());
                    notification.addAttribute((byte)0, "android.intent.action.MISS_CALL".getBytes());
                    if(missCallAppInformation.negativeString != null) {
                        notification.addAttribute((byte)7, missCallAppInformation.negativeString.getBytes());
                    }

                    if(missCallAppInformation.positiveString != null) {
                        notification.addAttribute((byte)6, missCallAppInformation.positiveString.getBytes());
                    }

                    GattNotificationManager.sharedInstance().addNotification(notification);
                }

                this.incomingCallNotificationUID = -1;
                break;
            case 1:
                Log.i("PhoneStateReceiver", "[Broadcast]等待接电话=" + phoneNumber);
                if(incomingCallAppInformation != null && this.previousPhoneState != 1) {
                    notification = new GattNotification();
                    notification.eventID = 0;
                    notification.eventFlags = 25;
                    notification.categoryID = 1;
                    if(phoneNumber != null) {
                        notification.addAttribute((byte)1, phoneNumber.getBytes());
                    }

                    String formatter = incomingCallAppInformation.displayName;
                    notification.addAttribute((byte)4, String.format("%d", new Object[]{Integer.valueOf(formatter.length())}).getBytes());
                    notification.addAttribute((byte)3, formatter.getBytes());
                    notification.addAttribute((byte)0, "android.intent.action.INCOMING_CALL".getBytes());
                    if(incomingCallAppInformation.negativeString != null) {
                        notification.addAttribute((byte)7, incomingCallAppInformation.negativeString.getBytes());
                    }

                    if(incomingCallAppInformation.positiveString != null) {
                        notification.addAttribute((byte)6, incomingCallAppInformation.positiveString.getBytes());
                    }

                    GattNotificationManager.sharedInstance().addNotification(notification);
                    this.incomingCallNotificationUID = notification.notificationUID;
                    Log.i("PhoneStateReceiver", "incomingCallNotificationUID = " + this.incomingCallNotificationUID);
                }
                break;
            case 2:
                Log.i("PhoneStateReceiver", "[Broadcast]通话中=" + phoneNumber);
                if(this.previousPhoneState == 1) {
                    GattNotificationManager.sharedInstance().removeNotification(this.incomingCallNotificationUID);
                    this.incomingCallNotificationUID = -1;
                }
        }

        this.previousPhoneState = state;
    }
}
