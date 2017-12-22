

package bledocking.munc.app.ble.Ancs;


import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.text.SimpleDateFormat;

public class GattNotificationListener extends NotificationListenerService {
    private static String TAG = "NotificationListener";

    public GattNotificationListener() {
    }

    public void onNotificationPosted(StatusBarNotification sbn) {
        if(Build.VERSION.SDK_INT >= 19) {
            Bundle extras = sbn.getNotification().extras;
            Log.i(TAG, "GattNotification posted");
            Log.i(TAG, "notification:" + sbn.getNotification().toString());
            if(extras != null) {
                String appIdentifier = sbn.getPackageName();
                AppInformation appInformation = GattNotificationManager.sharedInstance().getAppInformation(appIdentifier);
                Log.i(TAG, "packageName:" + appIdentifier);
                if(appInformation != null) {
                    String notificationTitle = extras.getString("android.title");
                    String notificationText = extras.getString("android.text");
                    String notificationSubText = extras.getString("android.subText");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd\'T\'HHmmSS");
                    String notificationDate = dateFormat.format(Long.valueOf(sbn.getPostTime()));
                    if(notificationText != null) {
                        GattNotification notification = new GattNotification();
                        String notificationMessageSize = String.format("%d", new Object[]{Integer.valueOf(notificationText.length())});
                        Log.i(TAG, "notificationTitle:" + notificationTitle);
                        Log.i(TAG, "notificationText:" + notificationText);
                        Log.i(TAG, "notificationSubText:" + notificationSubText);
                        Log.i(TAG, "notificationDate:" + notificationDate);
                        Log.i(TAG, "notificationMessageSize:" + notificationMessageSize);
                        Log.i(TAG, "isOngoing:" + sbn.isOngoing());
                        notification.eventID = 0;
                        notification.eventFlags = 25;
                        if(Build.VERSION.SDK_INT >= 21 && sbn.getNotification().category != null) {
                            if(sbn.getNotification().category.equals("alarm")) {
                                notification.categoryID = 5;
                            } else if(sbn.getNotification().category.equals("call")) {
                                notification.categoryID = 1;
                            } else if(sbn.getNotification().category.equals("email")) {
                                notification.categoryID = 6;
                            } else if(sbn.getNotification().category.equals("err")) {
                                notification.categoryID = 0;
                            } else if(sbn.getNotification().category.equals("event")) {
                                notification.categoryID = 5;
                            } else if(sbn.getNotification().category.equals("msg")) {
                                notification.categoryID = 4;
                            } else if(sbn.getNotification().category.equals("progress")) {
                                notification.categoryID = 0;
                            } else if(sbn.getNotification().category.equals("promo")) {
                                notification.categoryID = 0;
                            } else if(sbn.getNotification().category.equals("recommendation")) {
                                notification.categoryID = 0;
                            } else if(sbn.getNotification().category.equals("service")) {
                                notification.categoryID = 0;
                            } else if(sbn.getNotification().category.equals("social")) {
                                notification.categoryID = 4;
                            } else if(sbn.getNotification().category.equals("status")) {
                                notification.categoryID = 0;
                            } else if(sbn.getNotification().category.equals("sys")) {
                                notification.categoryID = 0;
                            } else if(sbn.getNotification().category.equals("transport")) {
                                notification.categoryID = 0;
                            } else {
                                notification.categoryID = 0;
                            }
                        } else {
                            notification.categoryID = 0;
                        }

                        if(appIdentifier != null && appIdentifier.length() > 0) {
                            notification.addAttribute((byte)0, appIdentifier.getBytes());
                        }

                        if(notificationTitle != null && notificationTitle.length() > 0) {
                            notification.addAttribute((byte)1, notificationTitle.getBytes());
                        }

                        if(notificationTitle != null && notificationTitle.length() > 0) {
                            notification.addAttribute((byte)2, notificationTitle.getBytes());
                        }

                        if(notificationText != null && notificationText.length() > 0) {
                            notification.addAttribute((byte)3, notificationText.getBytes());
                        }

                        if(notificationMessageSize != null && notificationMessageSize.length() > 0) {
                            notification.addAttribute((byte)4, notificationMessageSize.getBytes());
                        }

                        if(notificationDate != null && notificationDate.length() > 0) {
                            notification.addAttribute((byte)5, notificationDate.getBytes());
                        }

                        if(appInformation.negativeString != null) {
                            notification.addAttribute((byte)7, appInformation.negativeString.getBytes());
                        }

                        if(appInformation.positiveString != null) {
                            notification.addAttribute((byte)6, appInformation.positiveString.getBytes());
                        }

                        GattNotificationManager.sharedInstance().addNotification(notification);
                    } else {
                        Log.i(TAG, "Warnning:Notification without Text!");
                    }
                } else {
                    Log.i(TAG, "Warnning:app not care!");
                }
            }
        }

    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
        if(Build.VERSION.SDK_INT >= 19) {
            String appIdentifier = sbn.getPackageName();
            Log.i(TAG, "packageName:" + appIdentifier);
            if(GattNotificationManager.sharedInstance().checkWhiteList(appIdentifier)) {
                Bundle extras = sbn.getNotification().extras;
                String notificationTitle = extras.getString("android.title");
                String notificationText = extras.getString("android.text");
                String notificationSubText = extras.getString("android.subText");
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd\'T\'HHmmSS");
                String notificationDate = dateFormat.format(Long.valueOf(sbn.getPostTime()));
                String notificationMessageSize = "0";
                if(notificationText != null) {
                    notificationMessageSize = String.format("%d", new Object[]{Integer.valueOf(notificationText.length())});
                }

                Log.i(TAG, "GattNotification removed");
                Log.i(TAG, "notificationTitle:" + notificationTitle);
                Log.i(TAG, "notificationText:" + notificationText);
                Log.i(TAG, "notificationSubText:" + notificationSubText);
                Log.i(TAG, "notificationDate:" + notificationDate);
                Log.i(TAG, "notificationMessageSize:" + notificationMessageSize);
                GattNotificationManager.sharedInstance().removeNotifications(appIdentifier);
            }
        }

    }
}
