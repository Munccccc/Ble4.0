

package bledocking.munc.app.ble.Ancs;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bledocking.munc.app.ble.util.SystemUtils;

class GetAppAttributesCommand {
    public static byte CommandID = 1;
    public String appIdentifier = null;
    private List<AttributeID> attributeIDs = null;

    public static GetAppAttributesCommand parse(byte[] format) {
        GetAppAttributesCommand getAppAttributesCommand = null;
        byte position = 0;
        int appIdentifierLength = 0;
        if(format != null && format.length > 0 && format[0] == CommandID) {
            getAppAttributesCommand = new GetAppAttributesCommand();
            int var7 = position + 1;

            int i;
            for(i = var7; i < format.length && format[i++] != 0; ++appIdentifierLength) {
                ;
            }

            if(i == format.length) {
                Log.i("GetAppAttributesCommand", "bad format:appIdentifier not completed");
                return null;
            }

            byte[] appIdentifierBytes = new byte[appIdentifierLength];
            System.arraycopy(format, var7, appIdentifierBytes, 0, appIdentifierLength);
            getAppAttributesCommand.appIdentifier = new String(appIdentifierBytes);
            var7 += getAppAttributesCommand.appIdentifier.length() + 1;

            while(var7 < format.length) {
                AttributeID attributeID = new AttributeID();
                attributeID.id = format[var7];
                ++var7;
                switch(attributeID.id) {
                    case 1:
                    case 2:
                    case 3:
                        if(var7 + 2 > format.length) {
                            Log.i("GetAppAttributesCommand", "no max length field found");
                            return null;
                        }

                        attributeID.maxLength = SystemUtils.byteArray2Int(format, var7, 2);
                        var7 += 2;
                    default:
                        getAppAttributesCommand.getAttributeIDs().add(attributeID);
                }
            }
        }

        return getAppAttributesCommand;
    }

    public GetAppAttributesCommand() {
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
        byte[] format = new byte[1 + this.appIdentifier.length() + 1 + this.getAttributeIDsLength()];
        byte position = 0;
        format[position] = CommandID;
        int var5 = position + 1;
        System.arraycopy(this.appIdentifier.getBytes(), 0, format, var5, this.appIdentifier.length());
        var5 += this.appIdentifier.length();
        format[var5] = 0;
        ++var5;
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
        string = string.concat("appIdentifier=" + this.appIdentifier + ";");
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
