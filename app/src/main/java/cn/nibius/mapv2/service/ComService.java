package cn.nibius.mapv2.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import cn.nibius.mapv2.activity.MainActivity;
import cn.nibius.mapv2.util.MessagePackage;
import cn.nibius.mapv2.util.Intersection;
import cn.nibius.mapv2.util.Approach;
import cn.nibius.mapv2.util.Vehicle;

import static cn.nibius.mapv2.util.EnDecodeUtil.String8ToInt;
import static cn.nibius.mapv2.util.EnDecodeUtil.removeTail0;
import static cn.nibius.mapv2.util.EnDecodeUtil.stringLatTODouble;
import static cn.nibius.mapv2.util.EnDecodeUtil.stringLngTODouble;
import static cn.nibius.mapv2.util.EnDecodeUtil.getMatch;


public class ComService extends Service {

    private String TAG = "ComService";
    private int numPorts = 5;
    private int[] ports = {8887, 8888, 8889, 7100, 9999};
    private boolean stop = false;
    private IBinder myBinder = new MyBinder();
    private Runnable[] networkRunnable = new Runnable[numPorts];
    private Handler updaterHandler = new Handler();
    private DatagramSocket[] sockets = new DatagramSocket[numPorts];
    private Runnable masterThread;
    private MessagePackage messagePackage = new MessagePackage();

    private Map intersections =  new HashMap();
    private Vehicle myCar= new Vehicle();


    @Override
    public void onCreate() {
        super.onCreate();

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
                    messageSPAT = new String(dataSPAT, 0, dataSPAT.length);
                    try {
                        JSONObject json = new JSONObject(messageSPAT);

                        JSONArray json_inter_array = new JSONArray(json.getString("intersections"));
                        JSONObject json_inter = json_inter_array.getJSONObject(0);
                        String interID = json_inter.getString("id");
                        if (!intersections.containsKey(interID)) {
                            continue;
                        }
                        Intersection inter = (Intersection) intersections.get(interID);
                        JSONArray json_stat_array = new JSONArray(json_inter.getString("states"));
                        for (int i = 0; i < json_stat_array.length(); i++) {
                            JSONObject json_laneset = json_stat_array.getJSONObject(i);
                            String lane = json_laneset.getString("laneSet");
                            if (lane.equals("FF"))
                                continue;
                            inter.currentState.put(lane, json_laneset.getInt("currState"));
                            inter.timeToChange.put(lane, json_laneset.getInt("timeToChange"));
                        }
                        intersections.put(interID, inter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                updaterHandler.postDelayed(this, 100);
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
                    messageMAP = new String(dataMAP, 0, dataMAP.length);
                    try {
                        JSONObject json = new JSONObject(messageMAP);
                        JSONArray json_inter_array = new JSONArray(json.getString("intersections"));
                        for (int k = 0; k < json_inter_array.length(); k++) {
                            JSONObject json_inter = json_inter_array.getJSONObject(k);
                            if (intersections.containsKey(json_inter.getString("id"))) {
                                continue;
                            }
                            Intersection newData = new Intersection();
                            newData.ID = json_inter.getString("id");
                            JSONObject json_ref = new JSONObject(json_inter.getString("refPoint"));
                            newData.centerLat = (double) json_ref.getInt("lat") / 10000000;
                            newData.centerLng = (double) json_ref.getInt("long") / 10000000;
                            JSONArray json_approaches_array = new JSONArray(json_inter.getString("approaches"));
                            for (int i = 0; i < json_approaches_array.length(); i++) {
                                JSONObject json_appr = json_approaches_array.getJSONObject(i);
                                json_appr = new JSONObject(json_appr.getString("approach"));
                                Approach newApp = new Approach();
                                newApp.selfID = json_appr.getInt("id");
                                newApp.selfName = json_appr.getString("name");
                                JSONArray json_driveLane_array = new JSONArray(json_appr.getString("drivingLanes"));
                                for (int j = 0; j < json_driveLane_array.length(); j++) {
                                    JSONObject json_lane = json_driveLane_array.getJSONObject(j);
                                    newApp.lanesNodesList.put(json_lane.getString("laneNumber"), json_lane.getInt("laneWidth"));
                                }
                                newData.approaches.put(newApp.selfID, newApp);
                            }
                            intersections.put(newData.ID, newData);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                updaterHandler.postDelayed(this, 100);
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
                    messageBSM = new String(dataBSM, 0, dataBSM.length);
                    try {
                        JSONObject json = new JSONObject(messageBSM);
                        String blob1 = json.getString("blob1");
                        myCar.updatePosition((double)String8ToInt(blob1.substring(14, 22)) / 1E7,
                                            (double)String8ToInt(blob1.substring(22, 30)) / 1E7,
                                            0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                updaterHandler.postDelayed(this, 100);
            }
        };


        networkRunnable[3] = new Runnable() {     // 7100 port
            String message7100;
            String pText = "<text>(.+)</text>";

            @Override
            public void run() {
                while (!stop) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[3].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] data7100 = packet.getData();
                    message7100 = new String(data7100, 0, data7100.length);
                    message7100 = removeTail0(message7100);
                    if (message7100.startsWith("<ui_request>")) {
                        message7100 = message7100.substring(0, 317);
                        Log.d(TAG,"7100: ui_request");
                    } else {
                        message7100 = message7100.substring(0, 255);
                        Log.d(TAG,"7100: normal");
                    }
                    switch (message7100.charAt(20)) {
                        case '1': // forward crash
                            myCar.updateSafety(1);
                            break;
                        case '2': // emergency break
                            myCar.updateSafety(2);
                            break;
                        case '3': // side crash
                            String textV2V = getMatch(message7100, pText);
                            if (textV2V.charAt(9) == 'R')  // Right
                                myCar.updateSafety(3);
                            else                           // Left
                                myCar.updateSafety(4);
                            break;
                        case 't':
                            myCar.updateSafety(0);
                            break;
                        default:
                            break;
                    }
                }
                updaterHandler.postDelayed(this, 100);
            }
        };


        networkRunnable[4] = new Runnable() {   // New-added thread
            String messageMAP;

            @Override
            public void run() {
                while (!stop) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[4].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataNew = packet.getData();
                    try {
                        messageMAP = new String(dataNew, 0, dataNew.length);
                        if (messageMAP.contains("GPGGA")) {
                            String info[] = messageMAP.split(",");
                            //Log.i(TAG, "GPGGA: " + messageMAP);
                            myCar.updatePosition(stringLatTODouble(info[2]),
                                    stringLngTODouble(info[4]), 1);
                        }
                        else{
                            Log.i(TAG, "not GPGGA");
                        }
                    } catch (Exception e) {
                        Log.i(TAG, "New Data: " + e.toString());
                    }
                }
                updaterHandler.postDelayed(this, 100);
            }
        };


        masterThread = new Runnable() {
            @Override
            public void run() {
                while (!stop) {

                    MainActivity.lock.lock();
                    try {
                        messagePackage.setIntersections(intersections);
                        messagePackage.setMyCar(myCar);
                    } finally {
                        MainActivity.lock.unlock();
                    }
                }
                updaterHandler.postDelayed(this, 100);
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
            for (int i = 0; i < numPorts; i++) new Thread(networkRunnable[i]).start();
            new Thread(masterThread).start();
        }

        public void stopListen() {
            stop = true;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
