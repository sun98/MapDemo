package cn.nibius.mapv2.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Objects;

import cn.nibius.mapv2.util.AngleUtil;
import cn.nibius.mapv2.util.MessagePackage;

public class ComService extends Service {
    private boolean test = true;    // true for PC-android test; false for real environment
    private int[] ports = {8887, 8888, 8889, 8890};    // 4 ports to listen
    private boolean stop = false;
    private IBinder myBinder = new MyBinder();      // binder for MainActivity to get values
    private Runnable[] networkRunnable = new Runnable[4];
    private DatagramSocket[] sockets = new DatagramSocket[4];
    private Runnable proThread;

    // Package to be get by MainActivity
    private MessagePackage messagePackage = new MessagePackage();

    // variables to calculate in Calculation module
    // SPAT
    private String lightID;
    private String[] lightState = new String[4];
    private int[] lightTime = new int[4];
    // MAP
    private LinkedList<MapInfo> maps = new LinkedList<>();
    // BSM
    private double currentLat, currentLng, oldLat, oldLng, speed, angle;
    // TIM
    private double obstacleLat, obstacleLng;
    private String stateTIM;

    @Override
    public void onCreate() {
        super.onCreate();
        for (int i = 0; i < 4; i++)
            try {
                sockets[i] = new DatagramSocket(ports[i]);
            } catch (SocketException e) {
                e.printStackTrace();
            }

        networkRunnable[0] = new Runnable() {   // SPAT thread
            String messageSPAT;

            @Override
            public void run() {
                while (!stop) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[0].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataSPAT = packet.getData();
                    if (test) messageSPAT = new String(dataSPAT, 0, dataSPAT.length);
                    else messageSPAT = bytesToHexString(dataSPAT);
                    if (messageSPAT != null) {
                        lightID = messageSPAT.substring(22, 26);
                        lightState[0] = messageSPAT.substring(50, 52);
                        lightState[1] = messageSPAT.substring(84, 86);
                        lightState[2] = messageSPAT.substring(118, 120);
                        lightState[3] = messageSPAT.substring(152, 154);
                        lightTime[0] = Integer.parseInt(messageSPAT.substring(56, 58), 16);
                        lightTime[1] = Integer.parseInt(messageSPAT.substring(90, 92), 16);
                        lightTime[2] = Integer.parseInt(messageSPAT.substring(124, 126), 16);
                        lightTime[3] = Integer.parseInt(messageSPAT.substring(158, 160), 16);

                    }
                }
            }
        };

        networkRunnable[1] = new Runnable() {   // MAP thread
            String messageMAP;

            @Override
            public void run() {
                while (!stop) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[1].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataMAP = packet.getData();
                    if (test) messageMAP = new String(dataMAP, 0, dataMAP.length);
                    else messageMAP = bytesToHexString(dataMAP);
                    boolean exist = false;
                    if (messageMAP != null) {
                        for (MapInfo info : maps)
                            if (Objects.equals(info.mapID, messageMAP.substring(44, 48)))
                                exist = true;
                        if (!exist) maps.add(new MapInfo(messageMAP));
                    }
                }
            }
        };

        networkRunnable[2] = new Runnable() {   // BSM thread
            String messageBSM;

            @Override
            public void run() {
                while (!stop) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[2].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataBSM = packet.getData();
                    if (test) messageBSM = new String(dataBSM, 0, dataBSM.length);
                    else messageBSM = bytesToHexString(dataBSM);
                    if (messageBSM != null) {
                        oldLat = currentLat;
                        oldLng = currentLng;
                        double newLat = String8ToInt(messageBSM.substring(14, 22)) / 1E7;
                        double newLng = String8ToInt(messageBSM.substring(22, 30)) / 1E7;
                        /* if the distance is to small, ignore this movement */
//                        if (Math.abs(newLat - oldLat) > 1e-6 && Math.abs(newLng - oldLng) > 1e-6) {
                        currentLat = newLat;
                        currentLng = newLng;
//                        }
                        speed = Integer.parseInt(messageBSM.substring(42, 46), 16)
                                % Integer.parseInt("10000000000000", 2) * 0.02;
                        angle = AngleUtil.getAngle(oldLng, oldLat, currentLng, currentLat);
                    }
                }
            }
        };

        networkRunnable[3] = new Runnable() {   // TIM thread
            String messageTIM;

            @Override
            public void run() {
                while (!stop) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[3].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataTIM = packet.getData();
                    if (test) messageTIM = new String(dataTIM, 0, dataTIM.length);
                    else messageTIM = bytesToHexString(dataTIM);
                    if (messageTIM != null) {
                        obstacleLat = String8ToInt(messageTIM.substring(70, 78)) / 1E7;
                        obstacleLng = String8ToInt(messageTIM.substring(82, 90)) / 1E7;
                        stateTIM = messageTIM.substring(52, 54);
                    }
                }
            }
        };

        proThread = new Runnable() {
            @Override
            public void run() {
                // TODO, this is a timing loop thread
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    public class MyBinder extends Binder {
        public MessagePackage getPackage() {
            return messagePackage;
        }

        public void startListen() {
            for (int i = 0; i < 4; i++) new Thread(networkRunnable[i]).start();
            new Thread(proThread).start();
        }

        public void stopListen() {
            stop = true;
        }
    }

    private class MapInfo {
        private int index[] = {
                156, 160, 160, 164, 172, 176, 176, 180, 188, 192, 192, 196,
                272, 276, 276, 280, 288, 292, 292, 296, 304, 308, 308, 312,
                390, 394, 394, 398, 406, 410, 410, 414, 422, 426, 426, 430,
                506, 510, 510, 514, 522, 526, 526, 530, 538, 542, 542, 546
        };
        String mapID;
        double mapLat, mapLng;
        int[] offset = new int[24];

        public MapInfo(String messageMAP) {
            mapID = messageMAP.substring(44, 48);
            mapLat = String8ToInt(messageMAP.substring(56, 64)) / 1E7;
            mapLng = String8ToInt(messageMAP.substring(68, 76)) / 1E7;
            for (int i = 0; i < 24; i++)
                offset[i] = String4ToInt(messageMAP.substring(index[2 * i], index[2 * i + 1]));
        }
    }

    public int String8ToInt(String s) {
        byte[] latByte = hexString2Byte(s);
        return (byte2Int(latByte));
    }

    private int byte2Int(byte[] b) {
        int intValue = 0;
        for (int i = 0; i < b.length; i++)
            intValue += (b[i] & 0xFF) << (8 * (3 - i));
        return intValue;
    }

    public int String4ToInt(String s) {
        if (s.charAt(0) >= '0' && s.charAt(0) <= '8')
            return String8ToInt("0000" + s);
        else return String8ToInt("ffff" + s);
    }

    private byte[] hexString2Byte(String s) {
        int len = s.length();
        byte[] b = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            b[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));

        return b;
    }

    public String bytesToHexString(byte[] src) {
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
}
