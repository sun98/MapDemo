package cn.nibius.mapv2.util;


import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    public static String getMatch(String source, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(source);
        while (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static String lightColor(int lightNum){
        // 0x01绿灯，0x02红灯，0x03黄灯，0x04绿闪，0x05红闪
        String color = null;
        switch (lightNum) {
            case 1:
                color = "绿灯";
                break;
            case 2:
                color = "红灯";
                break;
            case 3:
                color = "黄灯";
                break;
            case 4:
                color = "绿灯闪烁";
                break;
            case 5:
                color = "红灯闪烁";
                break;
            default:
                color = "未知";
                break;
        }

        return  color;
    }
}
