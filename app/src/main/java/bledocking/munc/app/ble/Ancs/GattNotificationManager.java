

package bledocking.munc.app.ble.Ancs;


import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GattNotificationManager {
    private Context context = null;
    private List<GattNotification> notifications = null;
    private List<AppInformation> appInformations = null;
    private int notificationUID = 0;
    private GattNotificationManager.NotificationPrividerGattFunctions notificationPrividerGattFunctions = null;
    private GattNotificationManager.NotificationConsumerGattFunctions notificationConsumerGattFunctions = null;
    private byte[] controlPointBuffer = null;
    private List<String> appWhiteList = new ArrayList();
    private static GattNotificationManager gattNotificationManager = null;

    public static GattNotificationManager sharedInstance() {
        if(gattNotificationManager == null) {
            gattNotificationManager = new GattNotificationManager();
        }

        return gattNotificationManager;
    }

    public GattNotificationManager() {
        this.notifications = new ArrayList();
        this.appInformations = new ArrayList();
    }

    public void addAppToWhiteList(String appIdentifier) {
        if(!this.checkWhiteList(appIdentifier)) {
            this.appWhiteList.add(appIdentifier);
        }

    }

    public void removeAppFromWhiteList(String appIdentifier) {
        Iterator i$ = this.appWhiteList.iterator();

        while(i$.hasNext()) {
            String app = (String)i$.next();
            if(app.equals(appIdentifier)) {
                this.appWhiteList.remove(app);
                break;
            }
        }

    }

    public boolean checkWhiteList(String appIdentifier) {
        boolean found = false;
        Iterator i$ = this.appWhiteList.iterator();

        while(i$.hasNext()) {
            String app = (String)i$.next();
            if(app.equals(appIdentifier)) {
                found = true;
                break;
            }
        }

        return found;
    }

    public List<String> getAppWhiteList() {
        return this.appWhiteList;
    }

    public void setNotificationPrividerGattFunctions(GattNotificationManager.NotificationPrividerGattFunctions gattFunctions) {
        this.notificationPrividerGattFunctions = gattFunctions;
    }

    public void addAppInformation(String packageName, String appName, String negativeString, String positiveString) {
        AppInformation appInformation = new AppInformation();
        appInformation.appIdentifier = packageName;
        appInformation.negativeString = negativeString;
        appInformation.positiveString = positiveString;
        appInformation.displayName = appName;
        if(appName != null) {
            appInformation.addAttribute((byte)0, appName.getBytes());
        }

        this.appInformations.add(appInformation);
    }

    public AppInformation getAppInformation(String packageName) {
        AppInformation appInformation = null;
        if(this.appInformations != null) {
            Iterator i$ = this.appInformations.iterator();

            while(i$.hasNext()) {
                AppInformation app = (AppInformation)i$.next();
                if(app.appIdentifier.equals(packageName)) {
                    appInformation = app;
                    break;
                }
            }
        }

        return appInformation;
    }

    public void addNotification(GattNotification notification) {
        notification.categoryCount = (byte)(this.getCurrentCategoryCount(notification.categoryID) + 1);
        notification.notificationUID = this.notificationUID++;
        this.notifications.add(notification);
        Log.i("addNotification", "notification:" + notification.toString());
        Log.i("addNotification", "size = " + this.notifications.size());
        GattNotificationNotify gattNotificationNotify = new GattNotificationNotify(notification.eventID, notification.eventFlags, notification.categoryID, notification.categoryCount, notification.notificationUID);
        if(this.notificationPrividerGattFunctions != null) {
            this.notificationPrividerGattFunctions.notifyAncsNotificationSource(gattNotificationNotify.build());
        }

    }

    public void removeNotification(int notificationUID) {
        Iterator i$ = this.notifications.iterator();

        while(i$.hasNext()) {
            GattNotification notification = (GattNotification)i$.next();
            if(notification.notificationUID == notificationUID) {
                this.notifications.remove(notification);
                GattNotificationNotify gattNotificationNotify = new GattNotificationNotify((byte)2, notification.eventFlags, notification.categoryID, notification.categoryCount, notification.notificationUID);
                if(this.notificationPrividerGattFunctions != null) {
                    this.notificationPrividerGattFunctions.notifyAncsNotificationSource(gattNotificationNotify.build());
                }
                break;
            }
        }

    }

    public void removeNotifications(String appIdentifier) {
        ArrayList notificationsToRemove = new ArrayList();
        Iterator i$ = this.notifications.iterator();

        GattNotification notification;
        while(i$.hasNext()) {
            notification = (GattNotification)i$.next();
            Iterator gattNotificationNotify = notification.getAttributes().iterator();

            while(gattNotificationNotify.hasNext()) {
                Attribute attribute = (Attribute)gattNotificationNotify.next();
                if(attribute.id == 0 && appIdentifier.equals(new String(attribute.attribute))) {
                    notificationsToRemove.add(notification);
                }
            }
        }

        i$ = notificationsToRemove.iterator();

        while(i$.hasNext()) {
            notification = (GattNotification)i$.next();
            this.notifications.remove(notification);
            GattNotificationNotify gattNotificationNotify1 = new GattNotificationNotify((byte)2, notification.eventFlags, notification.categoryID, notification.categoryCount, notification.notificationUID);
            if(this.notificationPrividerGattFunctions != null) {
                this.notificationPrividerGattFunctions.notifyAncsNotificationSource(gattNotificationNotify1.build());
            }
        }

        Log.i("removeNotifications", notificationsToRemove.size() + " notifications removed");
        Log.i("removeNotifications", "size = " + this.notifications.size());
    }

    public GattNotification getNotification(int notificationUID) {
        GattNotification notification = null;
        Iterator i$ = this.notifications.iterator();

        while(i$.hasNext()) {
            GattNotification gattNotification = (GattNotification)i$.next();
            if(gattNotification.notificationUID == notificationUID) {
                notification = gattNotification;
                break;
            }
        }

        return notification;
    }

    public void parseControlPoint(byte[] format) {
        boolean parsed = false;
        byte[] packet;
        if(this.controlPointBuffer != null && this.controlPointBuffer.length > 0) {
            packet = new byte[this.controlPointBuffer.length + format.length];
            System.arraycopy(this.controlPointBuffer, 0, packet, 0, this.controlPointBuffer.length);
            System.arraycopy(format, 0, packet, this.controlPointBuffer.length, format.length);
        } else {
            packet = format;
        }

        GattNotification gattNotification;
        switch(packet[0]) {
            case 0:
                GetNotificationAttributesCommand getNotificationAttributesCommand1 = GetNotificationAttributesCommand.parse(packet);
                if(getNotificationAttributesCommand1 != null) {
                    Log.i("parseControlPoint", "getNotificationAttributesCommand:" + getNotificationAttributesCommand1.toString());
                    parsed = true;
                    GetNotificationAttributesResponse performNotificationAction2 = new GetNotificationAttributesResponse();
                    performNotificationAction2.notificationUID = getNotificationAttributesCommand1.notificationUID;
                    Iterator appIdentifier2 = this.notifications.iterator();

                    label85:
                    while(appIdentifier2.hasNext()) {
                        gattNotification = (GattNotification)appIdentifier2.next();
                        if(gattNotification.notificationUID == getNotificationAttributesCommand1.notificationUID) {
                            Iterator attribute3 = getNotificationAttributesCommand1.getAttributeIDs().iterator();

                            while(true) {
                                if(!attribute3.hasNext()) {
                                    break label85;
                                }

                                AttributeID attributeID1 = (AttributeID)attribute3.next();
                                Attribute attribute1 = gattNotification.getAttribute(attributeID1.id);
                                if(attribute1 != null) {
                                    performNotificationAction2.addAttribute(attribute1.id, attribute1.attribute);
                                }
                            }
                        }
                    }

                    if(this.notificationPrividerGattFunctions != null) {
                        this.notificationPrividerGattFunctions.notifyAncsDataSoure(performNotificationAction2.build());
                    }
                }
                break;
            case 1:
                GetAppAttributesCommand getAppAttributesCommand = GetAppAttributesCommand.parse(packet);
                if(getAppAttributesCommand != null) {
                    Log.i("parseControlPoint", "getAppAttributesCommand:" + getAppAttributesCommand.toString());
                    parsed = true;
                    GetAppAttributesResponse getNotificationAttributesCommand = new GetAppAttributesResponse();
                    getNotificationAttributesCommand.appIdentifier = getAppAttributesCommand.appIdentifier;
                    Iterator performNotificationAction1 = this.appInformations.iterator();

                    label73:
                    while(performNotificationAction1.hasNext()) {
                        AppInformation appIdentifier1 = (AppInformation)performNotificationAction1.next();
                        Log.i("parseControlPoint", "appIdentifier:" + appIdentifier1.appIdentifier);
                        if(appIdentifier1.appIdentifier.equalsIgnoreCase(getAppAttributesCommand.appIdentifier)) {
                            Iterator gattNotification1 = getAppAttributesCommand.getAttributeIDs().iterator();

                            while(true) {
                                if(!gattNotification1.hasNext()) {
                                    break label73;
                                }

                                AttributeID attribute2 = (AttributeID)gattNotification1.next();
                                Attribute attributeID = appIdentifier1.getAttribute(attribute2.id);
                                if(attributeID != null) {
                                    getNotificationAttributesCommand.addAttribute(attributeID.id, attributeID.attribute);
                                }
                            }
                        }
                    }

                    if(this.notificationPrividerGattFunctions != null) {
                        this.notificationPrividerGattFunctions.notifyAncsDataSoure(getNotificationAttributesCommand.build());
                    }
                }
                break;
            case 2:
                PerformNotificationAction performNotificationAction = PerformNotificationAction.parse(packet);
                if(performNotificationAction != null) {
                    parsed = true;
                    if(performNotificationAction.actionID == 0) {
                        Log.i("parseControlPoint", "performNotificationAction:Positive");
                    } else {
                        Log.i("parseControlPoint", "performNotificationAction:Negative");
                    }

                    String appIdentifier = null;
                    gattNotification = this.getNotification(performNotificationAction.notificationUID);
                    if(gattNotification != null) {
                        Attribute attribute = gattNotification.getAttribute((byte)0);
                        if(attribute != null) {
                            appIdentifier = new String(attribute.attribute);
                        }
                    }

                    if(this.notificationPrividerGattFunctions != null) {
                        this.notificationPrividerGattFunctions.onPerformNotificationAction(appIdentifier, performNotificationAction.actionID);
                    }
                }
                break;
            default:
                Log.i("parseControlPoint", "discard:" + AncsUtils.getPacketString(packet));
        }

        if(!parsed) {
            this.controlPointBuffer = new byte[packet.length];
            System.arraycopy(packet, 0, this.controlPointBuffer, 0, packet.length);
        } else {
            this.controlPointBuffer = null;
        }

    }

    public void setNotificationConsumerGattFunctions(GattNotificationManager.NotificationConsumerGattFunctions gattFunctions) {
        this.notificationConsumerGattFunctions = gattFunctions;
    }

    public void performNotificationAction(PerformNotificationAction performNotificationAction) {
        this.notificationConsumerGattFunctions.writeAncsControlPoint(performNotificationAction.build());
    }

    public void getNotificationAttributes(GetNotificationAttributesCommand getNotificationAttributesCommand) {
        this.notificationConsumerGattFunctions.writeAncsControlPoint(getNotificationAttributesCommand.build());
    }

    public void getAppAttributes(GetAppAttributesCommand getAppAttributesCommand) {
        this.notificationConsumerGattFunctions.writeAncsControlPoint(getAppAttributesCommand.build());
    }

    public void parseNotificationSource(byte[] format) {
        GattNotificationNotify gattNotificationNotify = GattNotificationNotify.parse(format);
        if(gattNotificationNotify != null) {
            Log.i("parseNotificationSource", "gattNotificationNotify:" + gattNotificationNotify.toString());
            this.notificationConsumerGattFunctions.onNotificationNotify(gattNotificationNotify);
        }

    }

    public void parseDataSource(byte[] format) {
        switch(format[0]) {
            case 0:
                GetNotificationAttributesResponse getNotificationAttributesResponse = GetNotificationAttributesResponse.parse(format);
                if(getNotificationAttributesResponse != null) {
                    Log.i("parseDataSource", "getNotificationAttributesResponse:" + getNotificationAttributesResponse.toString());
                    this.notificationConsumerGattFunctions.onGetNotificationAttributesResponse(getNotificationAttributesResponse);
                }
                break;
            case 1:
                GetAppAttributesResponse getAppAttributesResponse = GetAppAttributesResponse.parse(format);
                if(getAppAttributesResponse != null) {
                    Log.i("parseDataSource", "getAppAttributesResponse:" + getAppAttributesResponse.toString());
                    this.notificationConsumerGattFunctions.onGetAppAttributesResponse(getAppAttributesResponse);
                }
        }

    }

    private byte getCurrentCategoryCount(byte categoryID) {
        byte categoryCount = 0;
        Iterator i$ = this.notifications.iterator();

        while(i$.hasNext()) {
            GattNotification notification = (GattNotification)i$.next();
            if(categoryID == notification.categoryID) {
                ++categoryCount;
            }
        }

        return categoryCount;
    }

    public interface NotificationConsumerGattFunctions {
        void writeAncsControlPoint(byte[] var1);

        void onNotificationNotify(GattNotificationNotify var1);

        void onGetNotificationAttributesResponse(GetNotificationAttributesResponse var1);

        void onGetAppAttributesResponse(GetAppAttributesResponse var1);
    }

    public interface NotificationPrividerGattFunctions {
        void notifyAncsNotificationSource(byte[] var1);

        void notifyAncsDataSoure(byte[] var1);

        void onPerformNotificationAction(String var1, byte var2);
    }
}
