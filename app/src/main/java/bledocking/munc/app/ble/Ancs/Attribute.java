

package bledocking.munc.app.ble.Ancs;

class Attribute {
    public byte id;
    public int length;
    public byte[] attribute;

    public Attribute(int length) {
        this.length = length;
        this.attribute = new byte[length];
    }

    public Attribute(byte id, byte[] attribute) {
        this.id = id;
        this.length = attribute.length;
        this.attribute = new byte[this.length];
        System.arraycopy(attribute, 0, this.attribute, 0, this.length);
    }

    public String toString() {
        String string = "";
        string = string.concat("id=" + this.id + ";");
        string = string.concat("length=" + this.length + ";");
        string = string.concat("attribute=" + new String(this.attribute));
        return string;
    }
}
