package cn.nibius.mapv2.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

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
    // TODO    // SPAT
    // TODO    // MAP
    private double currentLat, currentLng, oldLat, oldLng, speed, angle;   // BSM
    // TODO    // TIM

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
            @Override
            public void run() {
                // TODO
            }
        };

        networkRunnable[1] = new Runnable() {   // MAP thread
            @Override
            public void run() {
                // TODO
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
                        if (Math.abs(newLat - oldLat) > 1e-6 && Math.abs(newLng - oldLng) > 1e-6) {
                            currentLat = newLat;
                            currentLng = newLng;
                        }
                        speed = Integer.parseInt(messageBSM.substring(42, 46), 16)
                                % Integer.parseInt("10000000000000", 2) * 0.02;
                        angle = AngleUtil.getAngle(oldLng, oldLat, currentLng, currentLat);
                    }
                }
            }
        };

        networkRunnable[3] = new Runnable() {   // TIM thread
            @Override
            public void run() {
                // TODO
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
