

package bledocking.munc.app.ble.Ancs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bledocking.munc.app.ble.util.SystemUtils;

class GetAppAttributesResponse {
    public static byte CommandID = 1;
    public String appIdentifier;
    private List<Attribute> attributes = null;

    public GetAppAttributesResponse() {
        this.attributes = new ArrayList();
    }

    public void addAttribute(byte id, byte[] attributeBytes) {
        Attribute attribute = new Attribute(id, attributeBytes);
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

    public static GetAppAttributesResponse parse(byte[] format) {
        GetAppAttributesResponse getAppAttributesResponse = null;
        if(format != null && format.length > 0 && format[0] == CommandID) {
            byte position = 0;
            int appIdentifierLength = 0;
            getAppAttributesResponse = new GetAppAttributesResponse();
            int var9 = position + 1;

            for(int i = var9; format[i++] != 0; ++appIdentifierLength) {
                ;
            }

            byte[] appIdentifierBytes = new byte[appIdentifierLength];
            System.arraycopy(format, var9, appIdentifierBytes, 0, appIdentifierLength);
            getAppAttributesResponse.appIdentifier = new String(appIdentifierBytes);
            var9 += getAppAttributesResponse.appIdentifier.length() + 1;

            while(var9 < format.length) {
                byte var10000 = format[var9];
                ++var9;
                int length = SystemUtils.byteArray2Int(format, var9, 2);
                var9 += 2;
                Attribute attribute = new Attribute(length);
                System.arraycopy(format, var9, attribute.attribute, 0, attribute.length);
                var9 += attribute.length;
                getAppAttributesResponse.getAttributes().add(attribute);
            }
        }

        return getAppAttributesResponse;
    }

    public byte[] build() {
        byte[] format = new byte[1 + this.appIdentifier.length() + 1 + this.getAttributesLength()];
        byte position = 0;
        format[position] = CommandID;
        int var5 = position + 1;
        System.arraycopy(this.appIdentifier.getBytes(), 0, format, var5, this.appIdentifier.length());
        var5 += this.appIdentifier.length();
        format[var5] = 0;
        ++var5;

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
