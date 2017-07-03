/**
 * Created by Nibius on 2017/7/3.
 */
public class NumString2Message {
    private long lat, lng;
    private int event;

    public NumString2Message(String string) {
        String latString = string.substring(0, 8);
        byte[] latByte = hexString2Byte(latString);
        lat = byte2Int(latByte);

        String lngString = string.substring(8, 16);
        byte[] lngByte = hexString2Byte(lngString);
        lng = byte2Int(lngByte);
        event = Integer.valueOf(string.substring(16, 18));
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

    public static void main(String[] args) {
        String m1 = "191B0080CE74E50001";
        NumString2Message sample1 = new NumString2Message(m1);
        System.out.println(sample1.getLat() + "\t" + sample1.getLng() + "\t" + sample1.getEvent());
    }
}
