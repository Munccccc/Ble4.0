

package bledocking.munc.app.ble.Ancs;

import android.util.Log;

import bledocking.munc.app.ble.util.SystemUtils;

class GattNotificationNotify {
    public byte eventID = 0;
    public byte eventFlags = 0;
    public byte categoryID = 0;
    public byte categoryCount = 0;
    public int notificationUID = 0;

    public static GattNotificationNotify parse(byte[] format) {
        GattNotificationNotify notification = null;
        Log.i("build", "parse:" + AncsUtils.getPacketString(format));
        if(format != null && format.length == 8) {
            notification = new GattNotificationNotify();
            notification.eventID = format[0];
            notification.eventFlags = format[1];
            notification.categoryID = format[2];
            notification.categoryCount = format[3];
            notification.notificationUID = SystemUtils.byteArray2Int(format, 4, 4);
        }

        return notification;
    }

    public GattNotificationNotify() {
    }

    public GattNotificationNotify(byte eventID, byte eventFlags, byte categoryID, byte categoryCount, int notificationUID) {
        this.eventID = eventID;
        this.eventFlags = eventFlags;
        this.categoryID = categoryID;
        this.categoryCount = categoryCount;
        this.notificationUID = notificationUID;
    }

    public byte[] build() {
        byte[] format = new byte[8];
        format[0] = this.eventID;
        format[1] = this.eventFlags;
        format[2] = this.categoryID;
        format[3] = this.categoryCount;
        SystemUtils.int2ByteArray(this.notificationUID, format, 4, 4);
        Log.i("build", "format:" + AncsUtils.getPacketString(format));
        return format;
    }

    public String toString() {
        String string = "";
        string = string.concat("notificationUID=" + this.notificationUID + ";");
        string = string.concat("eventID=" + AncsUtils.getEventIDString(this.eventID) + ";");
        string = string.concat("eventFlags=" + AncsUtils.getEventFlags(this.eventFlags) + ";");
        string = string.concat("categoryID=" + AncsUtils.getCategoryIDString(this.categoryID) + ";");
        string = string.concat("categoryCount=" + this.categoryCount + ";");
        return string;
    }
}
