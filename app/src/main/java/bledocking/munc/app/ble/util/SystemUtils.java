package bledocking.munc.app.ble.util;

import java.lang.reflect.Method;

public class SystemUtils {
    public static final String MTK_PLATFORM_KEY = "ro.mediatek.platform";

    public SystemUtils() {
    }

    public static boolean int2ByteArray(int intValue, byte[] b, int pos, int length) {
        boolean result = false;
        if((double)intValue < Math.pow(2.0D, (double)(length * 8))) {
            for(int i = length - 1; i >= 0; --i) {
                b[pos + i] = (byte)(intValue >> 8 * i & 255);
            }

            result = true;
        }

        return result;
    }

    public static int byteArray2Int(byte[] b, int pos, int length) {
        int intValue = 0;

        for(int i = length - 1; i >= 0; --i) {
            intValue += (b[pos + i] & 255) << 8 * i;
        }

        return intValue;
    }

    public static String get(String key) throws IllegalArgumentException {
        String ret = "";

        try {
            Class e = Class.forName("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class};
            Method get = e.getMethod("get", paramTypes);
            Object[] params = new Object[]{new String(key)};
            ret = (String)get.invoke(e, params);
        } catch (IllegalArgumentException var6) {
            throw var6;
        } catch (Exception var7) {
            ret = "";
        }

        return ret;
    }

    public static boolean isMediatekPlatform() {
        String platform = get("ro.mediatek.platform");
        return platform != null && (platform.startsWith("MT") || platform.startsWith("mt"));
    }
}
