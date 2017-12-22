

package bledocking.munc.app.ble.Ancs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class AppInformation {
    public String appIdentifier;
    public String displayName;
    public String negativeString;
    public String positiveString;
    private List<Attribute> attributes = new ArrayList();

    AppInformation() {
    }

    public void addAttribute(byte id, byte[] attributeBytes) {
        Attribute attribute = new Attribute(id, attributeBytes);
        this.attributes.add(attribute);
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
        string = string.concat("appIdentifier=" + this.appIdentifier + ";");
        string = string.concat("attributes=");

        Attribute attribute;
        for(Iterator i$ = this.attributes.iterator(); i$.hasNext(); string = string.concat("<" + attribute.toString() + ">")) {
            attribute = (Attribute)i$.next();
        }

        return string;
    }
}
