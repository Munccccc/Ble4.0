

package bledocking.munc.app.ble.Ancs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class GattNotification {
    public byte eventID = 0;
    public byte eventFlags = 0;
    public byte categoryID = 0;
    public byte categoryCount = 0;
    public int notificationUID = 0;
    List<Attribute> attributes = new ArrayList();

    public GattNotification() {
    }

    public void addAttribute(byte id, byte[] attributeBytes) {
        if(attributeBytes != null) {
            Attribute attribute = new Attribute(id, attributeBytes);
            this.attributes.add(attribute);
        }

    }

    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    public Attribute getAttribute(byte attributeID) {
        Attribute attributeFound = null;
        Iterator i$ = this.attributes.iterator();

        while(i$.hasNext()) {
            Attribute attribute = (Attribute)i$.next();
            if(attribute.id == attributeID) {
                attributeFound = attribute;
            }
        }

        return attributeFound;
    }

    public String toString() {
        String string = "";
        string = string.concat("notificationUID=" + this.notificationUID + ";");
        string = string.concat("eventID=" + AncsUtils.getEventIDString(this.eventID) + ";");
        string = string.concat("eventFlags=" + AncsUtils.getEventFlags(this.eventFlags) + ";");
        string = string.concat("categoryID=" + AncsUtils.getCategoryIDString(this.categoryID) + ";");
        string = string.concat("categoryCount=" + this.categoryCount + ";");
        string = string.concat("attributes=");

        Attribute attribute;
        for(Iterator i$ = this.attributes.iterator(); i$.hasNext(); string = string.concat("<" + attribute.toString() + ">")) {
            attribute = (Attribute)i$.next();
        }

        return string;
    }
}
