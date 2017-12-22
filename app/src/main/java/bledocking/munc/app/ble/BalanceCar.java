package bledocking.munc.app.ble;

import android.util.Log;

public class BalanceCar {
    public String deviceId;
    public String deviceName;
    public int battery;
    public int speed;
    public int mileage;
    public byte mode;
    public boolean on;
    public boolean inSecurityMode;
    public int error;
    private static final byte PACKET_START_BYTE = -86;
    public static final byte COMMAND_ID_POWER_CONTROL = 0;
    public static final byte COMMAND_ID_SECURITYODE_CONTROL = 1;
    public static final byte COMMAND_ID_SET_MODE = 2;
    public static final byte COMMAND_ID_GET_DEVICE_INFORMATION = 3;
    public static final byte EVENT_ID_POWER_STATE = 0;
    public static final byte EVENT_ID_SECURITYODE_CHANGED = 1;
    public static final byte EVENT_ID_MODE_CHANGED = 2;
    public static final byte EVENT_ID_DEVICE_INFORMATION = 3;
    public static final byte EVENT_ID_SPEED_CHANGED = 4;
    public static final byte POWER_STATE_OFF = 0;
    public static final byte POWER_STATE_ON = 1;
    public static final byte MODE_BEGINNER = 0;
    public static final byte MODE_PLAYER = 1;
    public static final byte MODE_ENTERTAINMENT = 2;
    public static final byte SECURITY_MODE_OFF = 0;
    public static final byte SECURITY_MODE_ON = 1;
    private byte[] leftBuffer = null;
    private BalanceCar.EventReceiver eventReceiver;

    public BalanceCar() {
    }

    public void registerEventReceiver(BalanceCar.EventReceiver eventReceiver) {
        this.eventReceiver = eventReceiver;
    }

    public void dataIn(byte[] data) {
        byte[] buffer;
        if(this.leftBuffer != null && this.leftBuffer.length > 0) {
            if(data != null) {
                buffer = new byte[this.leftBuffer.length + data.length];
                System.arraycopy(this.leftBuffer, 0, buffer, 0, this.leftBuffer.length);
                System.arraycopy(data, 0, buffer, this.leftBuffer.length, data.length);
            } else {
                buffer = this.leftBuffer;
            }
        } else {
            buffer = data;
        }

        int position;
        for(position = 0; position < buffer.length && buffer[position] != -86; ++position) {
            ;
        }

        if(buffer.length > position) {
            if(buffer.length - position > 2 && buffer.length - position >= 2 + buffer[position + 1]) {
                byte payloadLength = buffer[position + 1];
                int packetLength = payloadLength + 3;
                this.parse(buffer, position, packetLength);
                this.leftBuffer = new byte[buffer.length - packetLength - position];
                System.arraycopy(buffer, position + packetLength, this.leftBuffer, 0, buffer.length - packetLength - position);
                this.dataIn((byte[])null);
            } else {
                this.leftBuffer = new byte[buffer.length - position];
                System.arraycopy(buffer, position, this.leftBuffer, 0, buffer.length - position);
            }
        } else {
            this.leftBuffer = null;
            if(buffer.length > 0) {
                Log.w("BalanceCar", "PACKET_START_BYTE not found, discard");
            }
        }

    }

    private boolean parse(byte[] packet, int start, int length) {
        boolean payloadLength = false;
        boolean eventId = false;
        int position = start + 1;
        int var12 = this.getUnsignedByte(packet[position]);
        byte sum = 0;

        int checksum;
        for(checksum = 0; checksum < var12 + 1; ++checksum) {
            sum += packet[position + checksum];
        }

        if(sum != packet[length - 1]) {
            Log.w("BalanceCar", "Check sum fail");
            return false;
        } else {
            ++position;
            byte var13 = packet[position];
            ++position;
            switch(var13) {
                case 0:
                    this.on = packet[position] == 1;
                    ++position;
                    if(this.on) {
                        this.eventReceiver.onPowerOn(this);
                    } else {
                        this.eventReceiver.onPowerOff(this);
                    }
                    break;
                case 1:
                    this.inSecurityMode = packet[position] == 1;
                    ++position;
                    if(this.inSecurityMode) {
                        this.eventReceiver.onEnterSecurityMode(this);
                    } else {
                        this.eventReceiver.onLeaveSecurityMode(this);
                    }
                    break;
                case 2:
                    this.mode = packet[position];
                    ++position;
                    this.eventReceiver.onModeChanged(this, this.mode);
                    break;
                case 3:
                    checksum = this.getUnsignedByte(packet[position]);
                    ++position;
                    byte[] deviceIdBytes = new byte[checksum];
                    System.arraycopy(packet, position, deviceIdBytes, 0, checksum);
                    this.deviceId = new String(deviceIdBytes);
                    position += checksum;
                    int lengthOfDeviceName = this.getUnsignedByte(packet[position]);
                    ++position;
                    byte[] deviceNameBytes = new byte[lengthOfDeviceName];
                    System.arraycopy(packet, position, deviceNameBytes, 0, lengthOfDeviceName);
                    this.deviceName = new String(deviceNameBytes);
                    position += lengthOfDeviceName;
                    this.battery = packet[position];
                    ++position;
                    this.mileage = this.getUnsignedByte(packet[position]);
                    ++position;
                    this.mileage += this.getUnsignedByte(packet[position]) << 8;
                    ++position;
                    this.speed = this.getUnsignedByte(packet[position]);
                    ++position;
                    this.speed += this.getUnsignedByte(packet[position]) << 8;
                    ++position;
                    this.mode = packet[position];
                    ++position;
                    this.on = packet[position] == 1;
                    ++position;
                    this.inSecurityMode = packet[position] == 1;
                    ++position;
                    this.eventReceiver.onDeviceInformationGot(this);
                    break;
                case 4:
                    this.speed = packet[position];
                    ++position;
                    this.speed += packet[position] << 8;
                    ++position;
                    this.eventReceiver.onSpeedChanged(this, this.speed);
            }

            byte var10000 = packet[position];
            ++position;
            return true;
        }
    }

    public void turnOn() {
        byte[] format = new byte[5];
        byte position = 0;
        format[position] = -86;
        int var4 = position + 1;
        format[var4] = 2;
        ++var4;
        format[var4] = 0;
        ++var4;
        format[var4] = 1;
        ++var4;
        format[var4] = 0;

        for(int i = 1; i < var4; ++i) {
            format[var4] += format[i];
        }

        this.eventReceiver.sendData(format);
    }

    public void turnOff() {
        byte[] format = new byte[5];
        byte position = 0;
        format[position] = -86;
        int var4 = position + 1;
        format[var4] = 2;
        ++var4;
        format[var4] = 0;
        ++var4;
        format[var4] = 1;
        ++var4;
        format[var4] = 0;

        for(int i = 1; i < var4; ++i) {
            format[var4] += format[i];
        }

        this.eventReceiver.sendData(format);
    }

    public void setMode(byte mode) {
        byte[] format = new byte[5];
        byte position = 0;
        format[position] = -86;
        int var5 = position + 1;
        format[var5] = 2;
        ++var5;
        format[var5] = 2;
        ++var5;
        format[var5] = mode;
        ++var5;
        format[var5] = 0;

        for(int i = 1; i < var5; ++i) {
            format[var5] += format[i];
        }

        this.eventReceiver.sendData(format);
    }

    public void enterSecurityMode() {
        byte[] format = new byte[5];
        byte position = 0;
        format[position] = -86;
        int var4 = position + 1;
        format[var4] = 2;
        ++var4;
        format[var4] = 1;
        ++var4;
        format[var4] = 1;
        ++var4;
        format[var4] = 0;

        for(int i = 1; i < var4; ++i) {
            format[var4] += format[i];
        }

        this.eventReceiver.sendData(format);
    }

    public void leaveSecurityMode() {
        byte[] format = new byte[5];
        byte position = 0;
        format[position] = -86;
        int var4 = position + 1;
        format[var4] = 2;
        ++var4;
        format[var4] = 1;
        ++var4;
        format[var4] = 0;
        ++var4;
        format[var4] = 0;

        for(int i = 1; i < var4; ++i) {
            format[var4] += format[i];
        }

        this.eventReceiver.sendData(format);
    }

    public void getDeviceInformation() {
        byte[] format = new byte[4];
        byte position = 0;
        format[position] = -86;
        int var4 = position + 1;
        format[var4] = 1;
        ++var4;
        format[var4] = 3;
        ++var4;
        format[var4] = 0;

        for(int i = 1; i < var4; ++i) {
            format[var4] += format[i];
        }

        this.eventReceiver.sendData(format);
    }

    private int getUnsignedByte(byte data) {
        return data & 255;
    }

    public interface EventReceiver {
        void onPowerOn(BalanceCar var1);

        void onPowerOff(BalanceCar var1);

        void onModeChanged(BalanceCar var1, byte var2);

        void onEnterSecurityMode(BalanceCar var1);

        void onLeaveSecurityMode(BalanceCar var1);

        void onDeviceInformationGot(BalanceCar var1);

        void onSpeedChanged(BalanceCar var1, int var2);

        void sendData(byte[] var1);
    }
}
