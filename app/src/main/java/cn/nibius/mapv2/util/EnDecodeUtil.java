package cn.nibius.mapv2.util;


import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;

public class EnDecodeUtil {

    public static String removeTail0(String str) {
        while (str.endsWith("0")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    public static byte[] hexString2Byte(String s) {
        int len = s.length();
        byte[] b = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            b[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        return b;
    }

    public static int String8ToInt(String s) {
        byte[] latByte = hexString2Byte(s);
        return (byte2Int(latByte));
    }

    public static int byte2Int(byte[] b) {
        int intValue = 0;
        for (int i = 0; i < b.length; i++)
            intValue += (b[i] & 0xFF) << (8 * (3 - i));
        return intValue;
    }

    public static int String4ToInt(String s) {
        if (s.charAt(0) >= '0' && s.charAt(0) <= '8')
            return String8ToInt("0000" + s);
        else return String8ToInt("ffff" + s);
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) return null;
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) stringBuilder.append(0);
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static double stringLatTODouble(String coor) {
        String dd = coor.substring(0,2);
        String mm = coor.substring(2,coor.length());

        return Double.parseDouble(dd) + Double.parseDouble(mm)/60;
    }

    public static double stringLngTODouble(String coor) {
        String dd = coor.substring(0,3);
        String mm = coor.substring(3,coor.length());

        return Double.parseDouble(dd) + Double.parseDouble(mm)/60;
    }

    public static LatLng coorConvert(LatLng sourceLatLng){
        CoordinateConverter converter  = new CoordinateConverter();
        converter.from(com.baidu.mapapi.utils.CoordinateConverter.CoordType.GPS);
        converter.coord(sourceLatLng);

        return converter.convert();
    }
}
