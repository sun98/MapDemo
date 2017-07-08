package com.example.mapdemo;

/**
 * Created by Nibius at 2017/7/3 15:14.
 */
public class NumString2Message {
    private double lat;
    private double lng;
    private double latS;
    private double lngS;
    private int event;

    public NumString2Message(String string) {
        if (string != null) {
            String latString = string.substring(0, 8);
//        TODO: 由于位数不够，无法判断纬度的正负，暂时只考虑正数
//        byte[] latByte = hexString2Byte(latString);
//        lat = byte2Int(latByte) * 1.0 / 100000000;
            lat = Long.valueOf(latString, 16) * 1.0 / 100000000;

            String lngString = string.substring(8, 16);
            byte[] lngByte = hexString2Byte(lngString);
            lng = byte2Int(lngByte) * 1.0 / 10000000;

            String latRString = string.substring(16, 24);
//        byte[] latRByte = hexString2Byte(latRString);
//        latS = byte2Int(latRByte) * 1.0 / 100000000;
            latS = Long.valueOf(latRString, 16) * 1.0 / 100000000;

            String lngRString = string.substring(24, 32);
            byte[] lngRByte = hexString2Byte(lngRString);
            lngS = byte2Int(lngRByte) * 1.0 / 10000000;

            event = Integer.valueOf(string.substring(32, 34));
        } else {
            lat = 0;
            lng = 0;
            latS = 0;
            lngS = 0;
            event = 0;
        }
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

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getLatS() {
        return latS;
    }

    public double getLngS() {
        return lngS;
    }

    public int getEvent() {
        return event;
    }

    public static void main(String[] args) {
        String m1 = "191B0080CE74E500191B0080CE74E50001";
        NumString2Message sample1 = new NumString2Message(m1);
        System.out.println(sample1.getLat() + "\t" + sample1.getLng() + "\t" + sample1.getEvent());
    }
}
