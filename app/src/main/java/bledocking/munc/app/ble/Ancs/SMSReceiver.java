

package bledocking.munc.app.ble.Ancs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SMSReceiver extends BroadcastReceiver {
    public SMSReceiver() {
    }

    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        AppInformation smsAppInformation = GattNotificationManager.sharedInstance().getAppInformation("android.provider.Telephony.SMS_RECEIVED");
        Log.i("SMSReceiver", "[Broadcast]" + intent.getAction());
        if(smsAppInformation != null && bundle != null) {
            Object[] pdus = (Object[])((Object[])bundle.get("pdus"));
            SmsMessage[] mges = new SmsMessage[pdus.length];

            for(int arr$ = 0; arr$ < pdus.length; ++arr$) {
                mges[arr$] = SmsMessage.createFromPdu((byte[])((byte[])pdus[arr$]));
            }

            SmsMessage[] var17 = mges;
            int len$ = mges.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                SmsMessage mge = var17[i$];
                String sender = mge.getDisplayOriginatingAddress();
                String content = mge.getMessageBody();
                Date date = new Date(mge.getTimestampMillis());
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd\'T\'HHmmSS");
                String sendTime = format.format(date);
                GattNotification notification = new GattNotification();
                notification.eventID = 0;
                notification.eventFlags = 16;
                notification.categoryID = 4;
                notification.addAttribute((byte)0, "android.provider.Telephony.SMS_RECEIVED".getBytes());
                if(sender != null) {
                    notification.addAttribute((byte)1, sender.getBytes());
                }

                if(content != null) {
                    notification.addAttribute((byte)4, String.format("%d", new Object[]{Integer.valueOf(content.length())}).getBytes());
                    notification.addAttribute((byte)3, content.getBytes());
                }

                if(sendTime != null) {
                    notification.addAttribute((byte)5, sendTime.getBytes());
                }

                if(smsAppInformation.negativeString != null) {
                    notification.addAttribute((byte)7, smsAppInformation.negativeString.getBytes());
                }

                GattNotificationManager.sharedInstance().addNotification(notification);
            }
        } else {
            Log.i("SMSReceiver", "android.provider.Telephony.SMS_RECEIVED not in write list!");
        }

    }
}
