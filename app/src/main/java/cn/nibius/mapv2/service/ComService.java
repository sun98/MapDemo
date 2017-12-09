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

import cn.nibius.mapv2.R;
import cn.nibius.mapv2.activity.MainActivity;
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
    private double lightLat, lightLng;
    // BSM
    private double currentLat, currentLng, oldLat, oldLng, speed, angle;
    // TIM
    private double obstacleLat, obstacleLng, angleTIM, distTIM;
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
                        lightState[2] = messageSPAT.substring(50, 52);
                        lightState[3] = messageSPAT.substring(84, 86);
//                        lightState[2] = messageSPAT.substring(118, 120);
//                        lightState[3] = messageSPAT.substring(152, 154);
                        lightTime[0] = Integer.parseInt(messageSPAT.substring(56, 58), 16);
                        lightTime[1] = Integer.parseInt(messageSPAT.substring(90, 92), 16);
                        lightTime[2] = Integer.parseInt(messageSPAT.substring(56, 58), 16);
                        lightTime[3] = Integer.parseInt(messageSPAT.substring(90, 92), 16);
//                        lightTime[2] = Integer.parseInt(messageSPAT.substring(124, 126), 16);
//                        lightTime[3] = Integer.parseInt(messageSPAT.substring(158, 160), 16);
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
//                        /* if the distance is to small, ignore this movement */
                        if (Math.abs(newLat - oldLat) > 1e-6 && Math.abs(newLng - oldLng) > 1e-6) {
                            currentLat = newLat;
                            currentLng = newLng;
                            angle = AngleUtil.getAngle(oldLng, oldLat, currentLng, currentLat);
                        }
                        speed = Integer.parseInt(messageBSM.substring(42, 46), 16)
                                % Integer.parseInt("10000000000000", 2) * 0.02;
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
                        angleTIM = Math.abs(angle - AngleUtil.getAngle(currentLng, currentLat,
                                obstacleLng, obstacleLat));
                        angleTIM = (angleTIM > 180) ? (360 - angleTIM) : angleTIM;
                        distTIM = AngleUtil.getDistance(currentLng, currentLat,
                                obstacleLng, obstacleLat);
                    }
                }
            }
        };

        proThread = new Runnable() {
            @Override
            public void run() {
                while (!stop) {
                    int tempEvent = 0, effLightTime = 0;
                    String effLightState = "init_effective_light_state",
                            tempMessage = "init_temp_message";

                    if (angleTIM < 45 && distTIM < 200 && distTIM > 50) {
                        tempEvent = 5;
                    }
                    for (MapInfo iMap : maps) {
                        if (Objects.equals(lightID, iMap.mapID)) {
                            tempEvent = 2;
                            int minIndex = -1;
                            double deltaAngle[] = new double[4], minDelta = 360;
                            for (int i = 0; i < 4; i++) {
                                double t = Math.abs(angle - iMap.branchAngles[i]);
                                deltaAngle[i] = (t > 180) ? (360 - t) : t;
                                if (deltaAngle[i] < minDelta) {
                                    minDelta = deltaAngle[i];
                                    minIndex = i;
                                }
                            }
                            effLightState = lightState[minIndex];
                            effLightTime = lightTime[minIndex];
                            lightLat = iMap.mapLat;
                            lightLng = iMap.mapLng;
                            break;
                        }
                    }
                    switch (tempEvent) {
                        case 5:
                            if (Objects.equals("01", stateTIM))
                                tempMessage = getString(R.string.in_front) + distTIM +
                                        getString(R.string.construction_message);
                            else if (Objects.equals("02", stateTIM))
                                tempMessage = getString(R.string.in_front) + distTIM +
                                        getString(R.string.ice_message);
                            else
                                tempMessage = getString(R.string.error_5);
                            break;
                        case 2:
                            if (effLightTime == 127) {
                                if (Objects.equals(effLightState, "02"))
                                    tempMessage = getString(R.string.red_too_long);
                                else if (Objects.equals(effLightState, "01"))
                                    tempMessage = getString(R.string.green_too_long);
                                else tempMessage = getString(R.string.error_too_long);
                            } else {
                                double lightDist = AngleUtil.getDistance(currentLng, currentLat, lightLng, lightLat);
                                if (Objects.equals(effLightState, "04")) {
                                    tempEvent = 3;  //green light
                                    if (lightDist < speed * effLightTime)
                                        tempMessage = getString(R.string.green_remain) + effLightTime +
                                                getString(R.string.origin_speed);
                                    else
                                        tempMessage = getString(R.string.green_remain) + effLightTime +
                                                getString(R.string.green_stop);
                                } else if (Objects.equals(effLightState, "05")) {
                                    tempEvent = 4;  // red light
                                    if (lightDist > speed * effLightTime)
                                        tempMessage = getString(R.string.red_remain) + effLightTime +
                                                getString(R.string.origin_speed);
                                    else {
                                        double sugSpeed = (int) (((int) (speed - effLightTime * 1.5 + Math.sqrt(1.5 * 1.5 * effLightTime * effLightTime - 2 * 1.5 * effLightTime * speed + 2 * 1.5 * lightDist)) * 3.6));
                                        if (sugSpeed > 1)
                                            tempMessage = getString(R.string.red_remain) +
                                                    effLightTime + getString(R.string.red_slow) +
                                                    sugSpeed + getString(R.string.kmh);
                                        else
                                            tempMessage = getString(R.string.red_remain) +
                                                    effLightTime + getString(R.string.red_stop);
                                    }
                                } else {
                                    tempEvent = 6;
                                    tempMessage = getString(R.string.error_6);
                                }
                            }
                    }

                    MainActivity.lock.lock();
                    try {
                        messagePackage.setCurrentLat(currentLat);
                        messagePackage.setCurrentLng(currentLng);
                        messagePackage.setCurrentAngle(angle);
                        messagePackage.setCurrentSpeed(speed);
                        messagePackage.setCurrentEvent(tempEvent);
                        messagePackage.setMessage(tempMessage);
                        if (tempEvent == 5) {
                            messagePackage.setEffectiveLatR(obstacleLat);
                            messagePackage.setEffectiveLngR(obstacleLng);
                        } else if (tempEvent >= 3) {
                            messagePackage.setEffectiveLatS(lightLat);
                            messagePackage.setEffectiveLngS(lightLng);
                        }

                    } finally {
                        MainActivity.lock.unlock();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

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
        private int indexes[][][] = {
                {{156, 160}, {172, 176}, {188, 192}},
                {{272, 276}, {288, 292}, {304, 308}},
                {{390, 394}, {406, 410}, {422, 426}},
                {{506, 510}, {522, 526}, {538, 542}}
        };
        private double tmpAngles[][];
        String mapID;
        double mapLat, mapLng;
        double branchAngles[] = new double[4];

        MapInfo(String messageMAP) {
            tmpAngles = new double[4][4];
            mapID = messageMAP.substring(44, 48);
            mapLat = String8ToInt(messageMAP.substring(56, 64)) / 1E7;
            mapLng = String8ToInt(messageMAP.substring(68, 76)) / 1E7;
            for (int i = 0; i < 4; i++) {
                int sum[] = {0, 0};
                for (int j = 0; j < 3; j++) {
                    sum[0] += String4ToInt(messageMAP.substring(indexes[i][j][0], indexes[i][j][0] + 4));
                    sum[1] += String4ToInt(messageMAP.substring(indexes[i][j][1], indexes[i][j][1] + 4));
                    tmpAngles[i][j] = AngleUtil.getAngleInt(0, 0, sum[0], sum[1]);
                }
                double result = 0;
                int count = 0;
                for (int j = 0; j < 3; j++) {
                    if (!Double.isNaN(tmpAngles[i][j])) {
                        result += tmpAngles[i][j];
                        count++;
                    }
                }
                branchAngles[i] = result / count;
            }
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

