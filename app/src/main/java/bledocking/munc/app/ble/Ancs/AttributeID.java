

package bledocking.munc.app.ble.Ancs;

class AttributeID {
    public byte id;
    public int maxLength;

    public AttributeID() {
    }

    public AttributeID(byte id, int maxLength) {
        this.id = id;
        this.maxLength = maxLength;
    }

    public String toString() {
        String string = "";
        string = string.concat("id=" + this.id + ";");
        string = string.concat("maxLength=" + this.maxLength);
        return string;
    }
}
