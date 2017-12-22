package bledocking.munc.app.util;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.Arrays;

public class LockData {

    /**
     * 报文起始符，设定为ASCII字符’$$‘
     */
    protected byte[] start = {0x24, 0x24};
    /**
     * 报文头为14字节固定长度
     */
    protected byte[] head = new byte[24];
    /**
     * 报文数据部分
     */
    protected byte[] data = new byte[0];
    /**
     * 最后一位,奇偶校验字节
     */
    protected byte[] parity = new byte[]{};

    {
        head[0] = start[0];
        head[1] = start[1];
        head[3] = (byte) 0xfe;
        head[20] = 0x00;
        head[21] = 0x01;
    }

    public LockData() {
    }

    ;

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public LockData(byte[] packet) {
        parity = new byte[]{packet[packet.length - 2], packet[packet.length - 1]};
        data = Arrays.copyOfRange(packet, head.length, packet.length - 1);
        head = Arrays.copyOfRange(packet, 0, head.length);
    }

    /**
     * 发送指令
     */
    public byte getFun() {
        return head[2];
    }

    /**
     * 应答指令
     */
    public byte getAckFun() {
        return head[3];
    }

    /**
     * 设置功能
     */
    public void setAuthCode(String authCode) {
        System.arraycopy(convertString2Bytes(authCode), 0, head, 4, 16);
    }

    /**
     * 设置功能
     */
      public void setCommand(byte command_byte) {
        head[2] = command_byte;
    }

    /**
     * 设置报文数据单元部分数据
     */
    public void setData(byte[] data) {
        if (null == data) {
            throw new NullPointerException("data is null");
        }
        this.data = data;
        int length = data.length;
        // 舍去前两个字节
        byte[] dataLen = intToByteArray(length);
        head[22] = dataLen[2];
        head[23] = dataLen[3];
    }

    /**
     * 获取整个数据包
     */
    public byte[] getPacketData() {
        byte[] packetData = new byte[head.length + data.length + 1 + 1];
        System.arraycopy(head, 0, packetData, 0, head.length);
        System.arraycopy(data, 0, packetData, head.length, data.length);
        // 抑或校验
        byte result = 0;
        for (int i = 2; i < packetData.length - 2; i++) {
            LogUtils.e("blueT", packetData[i] + "");
            result ^= packetData[i];
            print(new byte[]{result});
        }
        packetData[packetData.length - 2] = result;
        packetData[packetData.length - 1] = 0x23;
        print(packetData);
        return packetData;
    }

    public static void print(byte[] datas) {
        String befConver = "";
        for (int i = 0; i < datas.length; i++) {
            befConver += datas[i] + " ";
            System.out.print("0x" + Integer.toHexString(new Byte(datas[i]).intValue()) + " ");
        }
        System.out.println("");
        System.out.println(befConver);
    }

    public static byte[] convertString2Bytes(String str) {
        byte[] result = new byte[str.length()];
        for (int i = 0; i < str.length(); i++) {
            result[i] = (byte) str.charAt(i);
        }
        return result;
    }

    public static byte[] intToByteArray(final int integer) {
        int byteNum = (40 - Integer.numberOfLeadingZeros(integer < 0 ? ~integer : integer)) / 8;
        byte[] byteArray = new byte[4];

        for (int n = 0; n < byteNum; n++)
            byteArray[3 - n] = (byte) (integer >>> (n * 8));

        return (byteArray);
    }
}