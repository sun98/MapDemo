package cn.nibius.mapv2.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Objects;

import cn.nibius.mapv2.R;
import cn.nibius.mapv2.activity.MainActivity;
import cn.nibius.mapv2.util.AngleUtil;
import cn.nibius.mapv2.util.Constant.V2VEvent;
import cn.nibius.mapv2.util.Constant.LightEvent;
import cn.nibius.mapv2.util.Constant.RoadStateEvent;
import cn.nibius.mapv2.util.MessagePackage;

public class ComService extends Service {
    private String TAG = "MAPV2";
    private boolean record = false;

    private int[] ports = {8887, 8888, 8889, 8890, 8891};    // 4 ports to listen
    private boolean stop = false;
    private IBinder myBinder = new MyBinder();      // binder for MainActivity to get values
    private Runnable[] networkRunnable = new Runnable[5];
    private DatagramSocket[] sockets = new DatagramSocket[5];
    private Runnable proThread;

    // Package to be get by MainActivity
    private MessagePackage messagePackage = new MessagePackage();

    // variables to calculate in Calculation module
    // SPAT
    private String lightID;
    private String[] lightState = new String[4];
    private int[] lightTime = new int[4];
    // MAP
    private ArrayList<MapInfo> maps = new ArrayList<>();
    private double lightLat, lightLng;
    // BSM
    private double currentLat, currentLng, oldLat, oldLng, speed, angle;
    // TIM
    private double obstacleLat, obstacleLng, angleTIM, angleMap, distMap;
    private int distTIM;
    private String stateTIM;
    // BSM2
    private double otherLat, otherLng, oldOtherLat, oldOtherLng, otherAngle, otherSpeed;

    private FileOutputStream fos;


    @Override
    public void onCreate() {
        super.onCreate();
        if (record) {
            String time = String.valueOf(System.currentTimeMillis());
            File file = new File(Environment.getExternalStorageDirectory(), time);
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < 5; i++)
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
                    if (MainActivity.test) messageSPAT = new String(dataSPAT, 0, dataSPAT.length);
                    else {
                        messageSPAT = bytesToHexString(dataSPAT);
                        if (record) {
                            try {
                                String tmp = removeTail0(messageSPAT);
                                fos.write(("SPAT " + tmp + "\n").getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
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
                    if (MainActivity.test) messageMAP = new String(dataMAP, 0, dataMAP.length);
                    else {
                        messageMAP = bytesToHexString(dataMAP);
                        if (record) {
                            try {
                                String tmp = removeTail0(messageMAP);
                                fos.write(("MAP: " + tmp + "\n").getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
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
                    if (MainActivity.test) messageBSM = new String(dataBSM, 0, dataBSM.length);
                    else {
                        messageBSM = bytesToHexString(dataBSM);
                        if (record) {
                            try {
                                String temp = removeTail0(messageBSM);
                                fos.write(("BSM: " + temp + "\n").getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
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
                    if (MainActivity.test) messageTIM = new String(dataTIM, 0, dataTIM.length);
                    else {
                        messageTIM = bytesToHexString(dataTIM);
                        if (record) {
                            try {
                                String temp = removeTail0(messageTIM);
                                fos.write(("TIM: " + temp + "\n").getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (messageTIM != null) {
                        obstacleLat = String8ToInt(messageTIM.substring(70, 78)) / 1E7;
                        obstacleLng = String8ToInt(messageTIM.substring(82, 90)) / 1E7;
                        stateTIM = messageTIM.substring(52, 54);
                        angleTIM = Math.abs(angle - AngleUtil.getAngle(currentLng, currentLat,
                                obstacleLng, obstacleLat));
                        angleTIM = (angleTIM > 180) ? (360 - angleTIM) : angleTIM;
                        distTIM = (int) AngleUtil.getDistance(currentLng, currentLat,
                                obstacleLng, obstacleLat);
                    }
                }
            }
        };

        networkRunnable[4] = new Runnable() {
            String messageBSM2;

            @Override
            public void run() {
                while (!stop) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[4].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataBSM = packet.getData();
                    if (MainActivity.test) messageBSM2 = new String(dataBSM, 0, dataBSM.length);
                    else {
                        messageBSM2 = bytesToHexString(dataBSM);
                        if (record) {
                            try {
                                String temp = removeTail0(messageBSM2);
                                fos.write(("BSM2 " + temp + "\n").getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (messageBSM2 != null) {
                        oldOtherLat = otherLat;
                        oldOtherLng = otherLng;
                        double newLat = String8ToInt(messageBSM2.substring(14, 22)) / 1E7;
                        double newLng = String8ToInt(messageBSM2.substring(22, 30)) / 1E7;
//                        /* currently consider only one other car */
                        if (Math.abs(newLat - oldLat) > 1e-6 && Math.abs(newLng - oldLng) > 1e-6) {
                            otherLat = newLat;
                            otherLng = newLng;
                            otherAngle = AngleUtil.getAngle(oldOtherLng, oldOtherLat, currentLng, currentLat);
                        }
                        otherSpeed = Integer.parseInt(messageBSM2.substring(42, 46), 16)
                                % Integer.parseInt("10000000000000", 2) * 0.02;
                    }
                }
            }
        };

        proThread = new Runnable() {
            @Override
            public void run() {
                while (!stop) {
                    int effLightTime = 0;
                    LightEvent tempLightEvent = LightEvent.NOLIGHT;
                    RoadStateEvent tempRoadStateEvent = RoadStateEvent.NOROADSTATE;
                    String effLightState = "init_effective_light_state",
                            tempMessage = "你好";

                    if (angleTIM < 45 && distTIM < 200 && distTIM > 20) {
                        tempRoadStateEvent = RoadStateEvent.UNKNOWNSTATE;
                    }
                    MapInfo targetMap = new MapInfo();
                    for (MapInfo iMap : maps) {
                        angleMap = Math.abs(angle - AngleUtil.getAngle(currentLng, currentLat,
                                iMap.mapLng, iMap.mapLat));
                        distMap = AngleUtil.getDistance(currentLng, currentLat,
                                iMap.mapLng, iMap.mapLat);
                        if (angleMap < 45 && distMap < 140 && distMap > 20) {
                            targetMap = iMap;
                            break;
                        }
                    }
                    if (Objects.equals(lightID, targetMap.mapID)) {
                        tempLightEvent = LightEvent.LONGLIGHT;
                        int minIndex = -1;
                        double deltaAngle[] = new double[4], minDelta = 360;
                        for (int i = 0; i < 4; i++) {
                            double t = Math.abs(angle - targetMap.branchAngles[i]);
                            deltaAngle[i] = (t > 180) ? (360 - t) : t;
                            if (deltaAngle[i] < minDelta) {
                                minDelta = deltaAngle[i];
                                minIndex = i;
                            }
                        }
                        effLightState = lightState[minIndex];
                        effLightTime = lightTime[minIndex];
                        lightLat = targetMap.mapLat;
                        lightLng = targetMap.mapLng;
                    }

//                    deal with road state
                    if (tempRoadStateEvent == RoadStateEvent.UNKNOWNSTATE) {
                        if (Objects.equals("01", stateTIM)) {
                            tempMessage = getString(R.string.in_front) + distTIM +
                                    getString(R.string.construction_message);
                            tempRoadStateEvent = RoadStateEvent.OBSTACLE;
                        } else if (Objects.equals("02", stateTIM)) {
                            tempMessage = getString(R.string.in_front) + distTIM +
                                    getString(R.string.ice_message);
                            tempRoadStateEvent = RoadStateEvent.ICE;
                        } else {
                            Log.i(TAG, "run: Error: event=5 && TIM state!=01 && TIM state!=02");
//                                tempMessage = getString(R.string.error_5);
                            tempRoadStateEvent = RoadStateEvent.UNKNOWNSTATE;
                        }
                    }

//                    deal with light state
                    if (tempLightEvent == LightEvent.LONGLIGHT) {
//                            Log.i(TAG, "run: lightLat=" + lightLat + ", lightLng=" + lightLng);
                        if (effLightTime >= 127) {
                            tempLightEvent = LightEvent.LONGLIGHT;
                            if (Objects.equals(effLightState, "02")) {
                                tempMessage = getString(R.string.red_too_long);
//                                    Log.i(TAG, "run: red 127");
                            } else if (Objects.equals(effLightState, "01"))
                                tempMessage = getString(R.string.green_too_long);
                            else if (Objects.equals(effLightState, "03"))
                                tempMessage = getString(R.string.yellow);
                            else if (Objects.equals(effLightState, "04") ||
                                    Objects.equals(effLightState, "05"))
                                tempMessage = "";
                            else
                                Log.i(TAG, "run: Error: lightTime=127 && lightState=" + effLightState);
//                                    tempMessage = getString(R.string.error_too_long);
                        } else {
                            double lightDist = AngleUtil.getDistance(currentLng, currentLat, lightLng, lightLat);
                            if (Objects.equals(effLightState, "04")) {
                                tempLightEvent = LightEvent.TIMEDGREENLIGHT;  //green light
                                if (lightDist < speed * effLightTime)
                                    tempMessage = getString(R.string.green_remain) + effLightTime +
                                            getString(R.string.origin_speed);
                                else
                                    tempMessage = getString(R.string.green_remain) + effLightTime +
                                            getString(R.string.green_stop);
                            } else if (Objects.equals(effLightState, "05")) {
                                tempLightEvent = LightEvent.TIMEDREDLIGHT;  // red light
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
                                tempLightEvent = LightEvent.UNKNOWNLIGHT;
                                Log.i(TAG, "run: Error: event=6");
//                                    tempMessage = getString(R.string.error_6);
                            }
                        }
                    }


                    V2VEvent tempV2VEvent = V2VEvent.NOV2V;
                    double otherDistance = AngleUtil.getDistance(currentLng, currentLat, otherLng, otherLat);
                    double vehicleAngle = Math.abs(angle - otherAngle);
//
                    vehicleAngle = (vehicleAngle > 180) ? (360 - vehicleAngle) : vehicleAngle;
                    if (vehicleAngle > 45 && vehicleAngle < 135 && otherDistance > 50 && otherDistance < 200) {
                        double angle1 = Math.abs(angle - AngleUtil.getAngle(oldLng, oldLat, otherLng, otherLat));
                        angle1 = (angle1 > 180) ? (360 - angle1) : angle1;
                        double angle2 = Math.abs(otherAngle - AngleUtil.getAngle(oldOtherLng, oldOtherLat, currentLng, currentLat));
                        angle2 = (angle2 > 180) ? (360 - angle2) : angle2;
                        double dis1 = otherDistance * Math.sin(Math.PI * angle1 / 180) / Math.sin(Math.PI * vehicleAngle / 180);
                        double dis2 = otherDistance * Math.sin(Math.PI * angle2 / 180) / Math.sin(Math.PI * vehicleAngle / 180);
                        if (Math.abs(dis2 / speed - dis1 / otherSpeed) < 1) {
                            tempV2VEvent = V2VEvent.CROSSCRASH;
                            tempMessage = getString(R.string.cross_crash_message);
                        }
                    }

                    if (Math.abs(angle - otherAngle) < 30 && otherDistance < 300 && otherSpeed < 0.6 * speed) {
//                        ???
                        if (speed * speed / 3 > otherDistance + 15) {
                            tempV2VEvent = V2VEvent.FORWARDCRASH;
                            tempMessage = getString(R.string.forward_crash_message);
                        }
                    }

                    MainActivity.lock.lock();
                    try {
                        messagePackage.setCurrentLat(currentLat);
                        messagePackage.setCurrentLng(currentLng);
                        messagePackage.setCurrentAngle(angle);
                        messagePackage.setCurrentSpeed(speed);
                        messagePackage.setCurrentLightEvent(tempLightEvent);
                        messagePackage.setCurrentRoadStateEvent(tempRoadStateEvent);
                        messagePackage.setMessage(tempMessage);
                        if (tempRoadStateEvent != RoadStateEvent.NOROADSTATE) {
                            messagePackage.setEffectiveLatR(obstacleLat);
                            messagePackage.setEffectiveLngR(obstacleLng);
                        }
                        if (tempLightEvent != LightEvent.NOLIGHT) {
                            messagePackage.setEffectiveLatS(lightLat);
                            messagePackage.setEffectiveLngS(lightLng);
                        }
                        if (tempV2VEvent != V2VEvent.NOV2V) {
                            messagePackage.setOtherLat(otherLat);
                            messagePackage.setOtherLng(otherLng);
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

        MapInfo() {
        }

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

    public String removeTail0(String str) {
        if (!str.substring(str.length() - 1).equals("0")) {
            return str;
        } else {
            return removeTail0(str.substring(0, str.length() - 1));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (record) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
