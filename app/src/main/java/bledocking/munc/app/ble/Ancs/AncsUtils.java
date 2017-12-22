

package bledocking.munc.app.ble.Ancs;

public class AncsUtils {
    public static final String GATT_ANCS_SERVICE = "7905F431-B5CE-4E99-A40F-4B1E122D00D0";
    public static final String GATT_ANCS_NOTIFICATION_SOURCE = "9FBF120D-6301-42D9-8C58-25E699A21DBD";
    public static final String GATT_ANCS_CONTROL_POINT = "69D1D8F3-45E1-49A8-9821-9BBDFDAAD9D9";
    public static final String GATT_ANCS_DATA_SOURCE = "22EAC6E9-24D6-4BB5-BE44-B36ACE7C7BFB";
    public static final byte EVENT_ID_ADDED = 0;
    public static final byte EVENT_ID_MODIFIED = 1;
    public static final byte EVENT_ID_REMOVED = 2;
    public static final byte EVENT_FLAG_SILENT = 1;
    public static final byte EVENT_FLAG_IMPORTANT = 2;
    public static final byte EVENT_FLAG_PREEXISTING = 4;
    public static final byte EVENT_FLAG_POSITIVE_ACTION = 8;
    public static final byte EVENT_FLAG_NEGATIVE_ACTION = 16;
    public static final byte COMMAND_ID_GET_NOTIFICATION_ATTRIBUTES = 0;
    public static final byte COMMAND_ID_GET_APP_ATTRIBUTES = 1;
    public static final byte COMMAND_ID_PERFORM_NOTIFICATION_ACTION = 2;
    public static final byte ATTRIBUTE_ID_APP_IDENTIFIER = 0;
    public static final byte ATTRIBUTE_ID_TITLE = 1;
    public static final byte ATTRIBUTE_ID_SUBTITLE = 2;
    public static final byte ATTRIBUTE_ID_MESSAGE = 3;
    public static final byte ATTRIBUTE_ID_MESSAGE_SIZE = 4;
    public static final byte ATTRIBUTE_ID_DATE = 5;
    public static final byte ATTRIBUTE_ID_POSITIVE_ACTION_LABEL = 6;
    public static final byte ATTRIBUTE_ID_NEGATIVE_ACTION_LABEL = 7;
    public static final byte APP_ATTRIBUTE_ID_DISPLAY_NAME = 0;
    public static final byte CATEGORY_ID_OTHER = 0;
    public static final byte CATEGORY_ID_INCOMING_CALL = 1;
    public static final byte CATEGORY_ID_MISSED_CALL = 2;
    public static final byte CATEGORY_ID_VOICE_MAIL = 3;
    public static final byte CATEGORY_ID_SOCIAL = 4;
    public static final byte CATEGORY_ID_SCHEDULE = 5;
    public static final byte CATEGORY_ID_EMAIL = 6;
    public static final byte CATEGORY_ID_NEWS = 7;
    public static final byte CATEGORY_ID_HEALTH_AND_FITNESS = 8;
    public static final byte CATEGORY_ID_BUSINESS_AND_FINANCE = 9;
    public static final byte CATEGORY_ID_LOCATION = 10;
    public static final byte CATEGORY_ID_ENTERTAINMENT = 11;
    public static final byte ACTION_ID_POSITIVE = 0;
    public static final byte ACTION_ID_NEGATICE = 1;
    public static final String APP_PACKAGE_NAME_INCOMING_CALL = "android.intent.action.INCOMING_CALL";
    public static final String APP_PACKAGE_NAME_MISS_CALL = "android.intent.action.MISS_CALL";
    public static final String APP_PACKAGE_NAME_SMS = "android.provider.Telephony.SMS_RECEIVED";
    public static final byte SIZE_OF_LENGTH = 2;
    public static final byte SIZE_OF_NOTIFCATIONUID = 4;

    public AncsUtils() {
    }

    static String getAttrIDString(byte attrID) {
        String temp = "";
        switch(attrID) {
            case 0:
                temp = "app identifier";
                break;
            case 1:
                temp = "title";
                break;
            case 2:
                temp = "subtitle";
                break;
            case 3:
                temp = "message";
                break;
            case 4:
                temp = "message size";
                break;
            case 5:
                temp = "date";
                break;
            default:
                temp = "_unknown_";
        }

        return temp;
    }

    static String getCommandIDString(byte commandID) {
        String temp = "";
        switch(commandID) {
            case 0:
                temp = "get notification attributes";
                break;
            case 1:
                temp = "get application attributes";
                break;
            default:
                temp = "unknown command id : " + Byte.toString(commandID);
        }

        return temp;
    }

    static String getEventIDString(byte eventID) {
        String temp = "";
        switch(eventID) {
            case 0:
                temp = "event added";
                break;
            case 1:
                temp = "event modified";
                break;
            case 2:
                temp = "event removed";
                break;
            default:
                temp = "unknown event id : " + Byte.toString(eventID);
        }

        return temp;
    }

    static String getCategoryIDString(byte categoryID) {
        String temp = "";
        switch(categoryID) {
            case 0:
                temp = "other";
                break;
            case 1:
                temp = "incoming call";
                break;
            case 2:
                temp = "missed call";
                break;
            case 3:
                temp = "voice mail";
                break;
            case 4:
                temp = "social";
                break;
            case 5:
                temp = "schedule";
                break;
            case 6:
                temp = "email";
                break;
            case 7:
                temp = "news";
                break;
            case 8:
                temp = "fitness";
                break;
            case 9:
                temp = "finance";
                break;
            case 10:
                temp = "location";
                break;
            case 11:
                temp = "entertainment";
                break;
            default:
                temp = "unknown category id : " + Byte.toString(categoryID);
        }

        return temp;
    }

    static String getEventFlags(byte eventFlags) {
        String temp = "";
        if((eventFlags & 1) != 0) {
            temp = temp.concat("slient");
        }

        if((eventFlags & 2) != 0) {
            temp = temp.concat("important");
        }

        return temp;
    }

    static String getActionIDString(byte actionID) {
        String temp = "";
        switch(actionID) {
            case 0:
                temp = "Positive";
                break;
            case 1:
                temp = "Negative";
        }

        return temp;
    }

    static String getPacketString(byte[] format) {
        String string = "";

        for(int i = 0; i < format.length; ++i) {
            string = string.concat(String.format("%02x ", new Object[]{Byte.valueOf(format[i])}));
        }

        return string;
    }
}
