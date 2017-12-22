
package bledocking.munc.app.ble.Ancs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bledocking.munc.app.ble.util.SystemUtils;

class GetNotificationAttributesCommand {
    public static byte CommandID = 0;
    public int notificationUID;
    private List<AttributeID> attributeIDs = null;

    public static GetNotificationAttributesCommand parse(byte[] format) {
        GetNotificationAttributesCommand getNotificationAttributesCommand = null;
        byte position = 0;
        boolean appIdentifierLength = false;
        if(format != null && format.length > 0 && format[0] == CommandID) {
            getNotificationAttributesCommand = new GetNotificationAttributesCommand();
            int var5 = position + 1;
            getNotificationAttributesCommand.notificationUID = SystemUtils.byteArray2Int(format, var5, 4);
            var5 += 4;

            while(var5 < format.length) {
                AttributeID attributeID = new AttributeID();
                attributeID.id = format[var5];
                ++var5;
                switch(attributeID.id) {
                    case 1:
                    case 2:
                    case 3:
                        attributeID.maxLength = SystemUtils.byteArray2Int(format, var5, 2);
                        var5 += 2;
                    default:
                        getNotificationAttributesCommand.getAttributeIDs().add(attributeID);
                }
            }
        }

        return getNotificationAttributesCommand;
    }

    public GetNotificationAttributesCommand() {
        this.attributeIDs = new ArrayList();
    }

    public void addAttributeID(byte id, byte maxLength) {
        AttributeID attributeId = new AttributeID(id, maxLength);
        this.attributeIDs.add(attributeId);
    }

    public List<AttributeID> getAttributeIDs() {
        return this.attributeIDs;
    }

    public byte[] build() {
        byte[] format = new byte[5 + this.getAttributeIDsLength()];
        byte position = 0;
        format[position] = CommandID;
        int var5 = position + 1;
        SystemUtils.int2ByteArray(this.notificationUID, format, var5, 4);
        var5 += 4;
        Iterator i$ = this.attributeIDs.iterator();

        while(i$.hasNext()) {
            AttributeID attributeID = (AttributeID)i$.next();
            format[var5] = attributeID.id;
            ++var5;
            switch(attributeID.id) {
                case 1:
                case 2:
                case 3:
                    SystemUtils.int2ByteArray(attributeID.maxLength, format, var5, 2);
                    var5 += 2;
            }
        }

        return format;
    }

    public String toString() {
        String string = "";
        string = string.concat("CommandID=" + AncsUtils.getCommandIDString(CommandID) + ";");
        string = string.concat("notificationUID=" + this.notificationUID + ";");
        string = string.concat("attributeIDs=");

        AttributeID attributeID;
        for(Iterator i$ = this.attributeIDs.iterator(); i$.hasNext(); string = string.concat("<" + attributeID.toString() + ">")) {
            attributeID = (AttributeID)i$.next();
        }

        return string;
    }

    private int getAttributeIDsLength() {
        int attributeIDsLength = 0;
        Iterator i$ = this.attributeIDs.iterator();

        while(i$.hasNext()) {
            AttributeID attributeID = (AttributeID)i$.next();
            ++attributeIDsLength;
            switch(attributeID.id) {
                case 1:
                case 2:
                case 3:
                    attributeIDsLength += 2;
            }
        }

        return attributeIDsLength;
    }
}
