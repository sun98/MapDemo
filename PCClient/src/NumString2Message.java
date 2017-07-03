/**
 * Created by Nibius on 2017/7/3.
 */
public class NumString2Message {
    private long lat;
    private long lng;
    private long latR;

    private long lngR;
    private int event;

    public NumString2Message(String string) {
        String latString = string.substring(0, 8);
//        byte[] latByte = hexString2Byte(latString);
//        lat = byte2Int(latByte);
        lat = Long.valueOf(latString, 16);

        String lngString = string.substring(8, 16);
        byte[] lngByte = hexString2Byte(lngString);
        lng = byte2Int(lngByte);

        String latRString = string.substring(16, 24);
//        byte[] latRByte = hexString2Byte(latRString);
//        latR = byte2Int(latRByte);
        latR = Long.valueOf(latRString);

        String lngRString = string.substring(24, 32);
        byte[] lngRByte = hexString2Byte(lngRString);
        lngR = byte2Int(lngRByte);

        event = Integer.valueOf(string.substring(32, 34));
    }

    private int byte2Int(byte[] b) {
        int intValue = 0;
        for (int i = 0; i < b.length; i++) {
            intValue += (b[i] & 0xFF) << (8 * (3 - i));
        }
        return intValue;
    }

    private byte[] hexString2Byte(String s) {
        int len = s.length();
        byte[] b = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            b[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16));
        }
        return b;
    }

    public long getLat() {
        return lat;
    }

    public int getEvent() {
        return event;
    }

    public long getLng() {
        return lng;
    }

    public long getLatR() {
        return latR;
    }

    public long getLngR() {
        return lngR;
    }

    public static void main(String[] args) {
        String m1 = "b8fe420f4862e22bb8fc3b504862d43002";
        NumString2Message sample1 = new NumString2Message(m1);
        System.out.println(sample1.getLat() + "\t" + sample1.getLng() + "\t" + sample1.getLatR() + "\t" + sample1.getLngR() + "\t" + sample1.getEvent());
    }
}