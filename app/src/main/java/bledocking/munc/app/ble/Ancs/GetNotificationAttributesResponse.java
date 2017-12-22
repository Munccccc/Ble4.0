

package bledocking.munc.app.ble.Ancs;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bledocking.munc.app.ble.util.SystemUtils;

class GetNotificationAttributesResponse {
    public static byte CommandID = 0;
    public int notificationUID;
    private List<Attribute> attributes = null;

    public GetNotificationAttributesResponse() {
        this.attributes = new ArrayList();
    }

    public void addAttribute(byte id, byte[] attributeBytes) {
        Log.i("addAttribute", "id=" + id + "attributeBytes length=" + attributeBytes.length);
        Attribute attribute = new Attribute(id, attributeBytes);
        Log.i("addAttribute", "attribute=" + attribute.toString());
        this.attributes.add(attribute);
    }

    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    public byte[] getAttribute(byte id) {
        byte[] attributeData = null;
        Iterator i$ = this.attributes.iterator();

        while(i$.hasNext()) {
            Attribute attribute = (Attribute)i$.next();
            if(attribute.id == id) {
                attributeData = attribute.attribute;
            }
        }

        return attributeData;
    }

    public static GetNotificationAttributesResponse parse(byte[] format) {
        GetNotificationAttributesResponse getNotificationAttributesResponse = null;
        if(format != null && format.length > 0 && format[0] == CommandID) {
            byte position = 0;
            boolean appIdentifierLength = false;
            getNotificationAttributesResponse = new GetNotificationAttributesResponse();
            int var7 = position + 1;
            getNotificationAttributesResponse.notificationUID = SystemUtils.byteArray2Int(format, var7, 4);
            var7 += 4;

            while(var7 < format.length) {
                byte var10000 = format[var7];
                ++var7;
                int length = SystemUtils.byteArray2Int(format, var7, 2);
                var7 += 2;
                Attribute attribute = new Attribute(length);
                System.arraycopy(format, var7, attribute.attribute, 0, attribute.length);
                var7 += attribute.length;
                getNotificationAttributesResponse.getAttributes().add(attribute);
            }
        }

        return getNotificationAttributesResponse;
    }

    public byte[] build() {
        byte[] format = new byte[5 + this.getAttributesLength()];
        byte position = 0;
        format[position] = CommandID;
        int var5 = position + 1;
        SystemUtils.int2ByteArray(this.notificationUID, format, var5, 4);
        var5 += 4;

        Attribute attribute;
        for(Iterator i$ = this.attributes.iterator(); i$.hasNext(); var5 += attribute.length) {
            attribute = (Attribute)i$.next();
            format[var5] = attribute.id;
            ++var5;
            SystemUtils.int2ByteArray(attribute.length, format, var5, 2);
            var5 += 2;
            System.arraycopy(attribute.attribute, 0, format, var5, attribute.length);
        }

        return format;
    }

    public String toString() {
        String string = "";
        string = string.concat("CommandID=" + AncsUtils.getCommandIDString(CommandID) + ";");
        string = string.concat("notificationUID=" + this.notificationUID + ";");
        string = string.concat("attributes=");

        Attribute attribute;
        for(Iterator i$ = this.attributes.iterator(); i$.hasNext(); string = string.concat("<" + attribute.toString() + ">")) {
            attribute = (Attribute)i$.next();
        }

        return string;
    }

    private int getAttributesLength() {
        int attributesLength = 0;

        Attribute attribute;
        for(Iterator i$ = this.attributes.iterator(); i$.hasNext(); attributesLength += attribute.length) {
            attribute = (Attribute)i$.next();
            ++attributesLength;
            attributesLength += 2;
        }

        return attributesLength;
    }
}
