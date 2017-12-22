

package bledocking.munc.app.ble.Ancs;


import bledocking.munc.app.ble.util.SystemUtils;

class PerformNotificationAction {
    public static byte CommandID = 2;
    public int notificationUID;
    public byte actionID;

    PerformNotificationAction() {
    }

    public static PerformNotificationAction parse(byte[] format) {
        PerformNotificationAction performNotificationAction = null;
        byte position = 0;
        if(format != null && format.length > 0 && format[0] == CommandID) {
            performNotificationAction = new PerformNotificationAction();
            int var3 = position + 1;
            performNotificationAction.notificationUID = SystemUtils.byteArray2Int(format, var3, 4);
            var3 += 4;
            performNotificationAction.actionID = format[var3];
            ++var3;
        }

        return performNotificationAction;
    }

    public void PerformNotificationAction() {
    }

    public byte[] build() {
        byte[] format = new byte[6];
        byte position = 0;
        format[position] = CommandID;
        int var3 = position + 1;
        SystemUtils.int2ByteArray(this.notificationUID, format, var3, 4);
        var3 += 4;
        format[var3] = this.actionID;
        ++var3;
        return format;
    }

    public String toString() {
        String string = "";
        string = string.concat("notificationUID=" + this.notificationUID + ";");
        string = string.concat("actionID=" + AncsUtils.getActionIDString(this.actionID));
        return string;
    }
}
