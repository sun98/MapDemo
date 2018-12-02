package cn.nibius.mapv2.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import cn.nibius.mapv2.util.RegexUtil;

import static cn.nibius.mapv2.util.EnDecodeUtil.String4ToInt;
import static cn.nibius.mapv2.util.EnDecodeUtil.String8ToInt;
import static cn.nibius.mapv2.util.EnDecodeUtil.bytesToHexString;
import static cn.nibius.mapv2.util.EnDecodeUtil.removeTail0;

public class ComService extends Service {
    private String TAG = "ComService";
    private boolean record = false;     // whether record messages while testing outside

    private int numPorts = 5;
    private int[] ports = {8887, 8888, 8889, 8890, 7100};    // 5 ports to listen
    private boolean stop = false;
    private IBinder myBinder = new MyBinder();      // binder for MainActivity to get values
    private Runnable[] networkRunnable = new Runnable[numPorts];
    private DatagramSocket[] sockets = new DatagramSocket[numPorts];
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
    private double currentLat = 31.0278622712, currentLng = 121.4218843711, oldLat, oldLng, speed, angle;
    // TIM
    private double obstacleLat, obstacleLng, angleTIM, angleMap, distMap;
    private int distTIM;
    private String stateTIM;
    // BSM2
//    private double otherLat, otherLng, oldOtherLat, oldOtherLng, otherAngle, otherSpeed, oldOtherSpeed;
    // 7100
    private String textV2V;
    private int idV2V;
    private boolean cancel = false, hasEvent = false;


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
        for (int i = 0; i < numPorts; i++)
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
                    /*
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
                    */
                    messageSPAT = new String(dataSPAT, 0, dataSPAT.length);
                    try {
                        JSONObject json = new JSONObject(messageSPAT);

                        JSONArray json_inter_array = new JSONArray(json.getString("intersections"));
                        JSONObject json_inter = json_inter_array.getJSONObject(0);
                        lightID = json_inter.getString("id");

                        //Log.i(TAG, "SPAT: id = " + lightID);

                        JSONArray json_stat_array = new JSONArray(json_inter.getString("states"));
                        JSONObject json_stat_0 = json_stat_array.getJSONObject(0);
                        lightState[0] = json_stat_0.getString("currState");
                        lightTime[0] = json_stat_0.getInt("timeToChange");
                        JSONObject json_stat_1 = json_stat_array.getJSONObject(1);
                        lightState[1] = json_stat_1.getString("currState");
                        lightTime[1] = json_stat_1.getInt("timeToChange");
                        JSONObject json_stat_2 = json_stat_array.getJSONObject(2);
                        lightState[2] = json_stat_2.getString("currState");
                        lightTime[2] = json_stat_2.getInt("timeToChange");
                        JSONObject json_stat_3 = json_stat_array.getJSONObject(3);
                        lightState[3] = json_stat_3.getString("currState");
                        lightTime[3] = json_stat_3.getInt("timeToChange");

                    }catch (JSONException e) {
                        e.printStackTrace();
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
                    /*
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
                    */
                    messageMAP = new String(dataMAP, 0, dataMAP.length);
                    String thisID = "";
                    try {
                        JSONObject json = new JSONObject(messageMAP);
                        JSONArray json_inter_array = new JSONArray(json.getString("intersections"));
                        JSONObject json_inter = json_inter_array.getJSONObject(0);
                        thisID = json_inter.getString("id");
                        //Log.i(TAG, "MAP: id = " + thisID);

                    }catch (JSONException e) {
                        e.printStackTrace();
                    }

                    boolean exist = false;
                    if (messageMAP != null) {
                        for (MapInfo info : maps)
                            if (Objects.equals(info.mapID, thisID))
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
                    /*
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
                    */
                    messageBSM = new String(dataBSM, 0, dataBSM.length);
                    try {
                        JSONObject json = new JSONObject(messageBSM);
                        String blob1 = json.getString("blob1");
                        //Log.i(TAG, "BSM: blob1 = " + blob1);

                        oldLat = currentLat;
                        oldLng = currentLng;
                        double newLat = String8ToInt(blob1.substring(14,22)) / 1E7;
                        double newLng = String8ToInt(blob1.substring(22,30)) / 1E7;
                        speed = Integer.parseInt(blob1.substring(38,42), 16) % Integer.parseInt("10000000000000", 2) * 0.02;
                        Log.i(TAG, "BSM: lat= "+String.valueOf(newLat)+" lng= "+String.valueOf(newLng)+" speed= "+String.valueOf(speed));

                        if (Math.abs(newLat - oldLat) > 1e-6 && Math.abs(newLng - oldLng) > 1e-6) {
                            currentLat = newLat;
                            currentLng = newLng;
                            angle = AngleUtil.getAngle(oldLng, oldLat, currentLng, currentLat);
                        }

                    }catch (JSONException e) {
                        e.printStackTrace();
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
                    messageTIM = removeTail0(messageTIM);
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

        networkRunnable[4] = new Runnable() {     // 7100 port
            String message7100;
            String pText = "<text>(.+)</text>";
            RegexUtil regexUtil = new RegexUtil();

            @Override
            public void run() {
                while (!stop) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[4].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] data7100 = packet.getData();
//                    if (MainActivity.test) message7100 = new String(data7100, 0, data7100.length);
//                    else {
                    message7100 = bytesToHexString(data7100);
                    if (record) {
                        try {
                            fos.write(("7100 " + removeTail0(message7100)).getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
//                    }
                    message7100 = removeTail0(message7100);
                    if (message7100.startsWith("<ui_request>")) {
                        message7100 = message7100.substring(0, 317);
                    } else {
                        message7100 = message7100.substring(0, 255);
                    }
//                    Log.i(TAG, "run: " + message7100);
                    switch (message7100.charAt(20)) {
                        case '1':
                        case '2':
                        case '3':
                            cancel = false;
                            hasEvent = true;
                            textV2V = regexUtil.getMatch(message7100, pText);
                            idV2V = message7100.charAt(20) - '0';
                            break;
                        case 't':
                            cancel = true;
                            hasEvent = true;
                            break;
                        default:
                            hasEvent = false;
                            break;
                    }
//                    Log.i(TAG, "run: " + textV2V);
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
                            tempMessage = "欢迎使用";

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
                    if (Objects.equals(lightID, targetMap.mapID) && lightID != null) {
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
                                Log.i(TAG, "run: Error: unknown light");
//                                    tempMessage = getString(R.string.error_6);
                            }
                        }
                    }


                    V2VEvent tempV2VEvent = V2VEvent.NOV2V;
                    if (hasEvent) {
                        if (cancel)
                            tempV2VEvent = V2VEvent.CANCEL;
                        else {
                            if (idV2V == 1) {
                                tempV2VEvent = V2VEvent.FORWARDCRASH;
                                tempMessage = getString(R.string.forward_crash_message);
                            } else if (idV2V == 2) {
                                tempV2VEvent = V2VEvent.EMERBRAKE;
                                tempMessage = getString(R.string.emergency_brake_message);
                            } else if (idV2V == 3) {
                                tempV2VEvent = V2VEvent.SIDECRASH;
                                if (textV2V.charAt(9) == 'R') {
                                    tempMessage = getString(R.string.right_cross_crash_message);
                                } else {
                                    tempMessage = getString(R.string.left_cross_crash_message);
                                }
                            } else {
                                tempV2VEvent = V2VEvent.UNKNOWNV2V;
                                tempMessage = "";
                            }
                        }
                    }
//                    Log.i(TAG, "final: " + (tempV2VEvent == V2VEvent.NOV2V));

                    MainActivity.lock.lock();
                    try {
                        messagePackage.setCurrentLat(currentLat);
                        messagePackage.setCurrentLng(currentLng);
                        messagePackage.setCurrentAngle(angle);
                        messagePackage.setCurrentSpeed(speed);
                        messagePackage.setCurrentLightEvent(tempLightEvent);
                        messagePackage.setCurrentRoadStateEvent(tempRoadStateEvent);
                        messagePackage.setCurrentV2VEvent(tempV2VEvent);
                        messagePackage.setMessage(tempMessage);
                        if (tempRoadStateEvent != RoadStateEvent.NOROADSTATE) {
                            messagePackage.setEffectiveLatR(obstacleLat);
                            messagePackage.setEffectiveLngR(obstacleLng);
                        }
                        if (tempLightEvent != LightEvent.NOLIGHT) {
                            messagePackage.setEffectiveLatS(lightLat);
                            messagePackage.setEffectiveLngS(lightLng);
                        }
                    } finally {
                        MainActivity.lock.unlock();
                    }
                    try {
                        Thread.sleep(10);
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
            // TODO: may cause memory leak?
            // consider using wait/notify method
            //for (int i = 0; i < numPorts; i++) new Thread(networkRunnable[i]).start();
            for (int i = 0; i < 3; i++) new Thread(networkRunnable[i]).start();
            new Thread(proThread).start();
        }

        public void stopListen() {
            stop = true;
        }
    }

    private class MapInfo {
        private double tmpAngles[][];
        String mapID;
        double mapLat, mapLng;
        double branchAngles[] = new double[4];

        MapInfo() {
        }

        MapInfo(String messageMAP) {
            try {
                tmpAngles = new double[4][4];

                JSONObject json = new JSONObject(messageMAP);
                JSONArray json_inter_array = new JSONArray(json.getString("intersections"));

                JSONObject json_inter = json_inter_array.getJSONObject(0);
                mapID = json_inter.getString("id");

                JSONObject json_ref = new JSONObject(json_inter.getString("refPoint"));
                mapLat = (double)json_ref.getInt("lat")/10000000;
                mapLng = (double)json_ref.getInt("long")/10000000;

                JSONArray json_approaches_array = new JSONArray(json_inter.getString("approaches"));

                for (int i = 0; i < 4; i++) {
                    JSONObject json_appr = json_approaches_array.getJSONObject(i);
                    json_appr = new JSONObject(json_appr.getString("approach"));
                    JSONArray json_driveLane_array = new JSONArray(json_appr.getString("drivingLanes"));
                    JSONObject json_drive = json_driveLane_array.getJSONObject(0);
                    JSONArray json_nodes_array = new JSONArray(json_drive.getString("nodeList"));

                    int sum[] = {0, 0};
                    for (int j = 0; j < 3; j++) {
                        sum[0] += String4ToInt(json_nodes_array.getString(j).substring(0,4));
                        sum[1] += String4ToInt(json_nodes_array.getString(j).substring(4,8));
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

            }catch (JSONException e) {
                e.printStackTrace();
            }
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
