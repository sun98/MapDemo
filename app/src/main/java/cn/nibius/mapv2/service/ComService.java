package cn.nibius.mapv2.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
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
import java.util.HashMap;
import java.util.Map;

import cn.nibius.mapv2.activity.MainActivity;
import cn.nibius.mapv2.util.MessagePackage;
import cn.nibius.mapv2.util.Intersection;
import cn.nibius.mapv2.util.Approach;
import cn.nibius.mapv2.util.Viechle;
import cn.nibius.mapv2.util.ViewController;

import static cn.nibius.mapv2.util.EnDecodeUtil.String8ToInt;
import static cn.nibius.mapv2.util.EnDecodeUtil.String4ToInt;
import static cn.nibius.mapv2.util.EnDecodeUtil.bytesToHexString;
import static cn.nibius.mapv2.util.EnDecodeUtil.removeTail0;


public class ComService extends Service {

    private String TAG = "ComService";
    private boolean record = false;
    private int numPorts = 4;
    private int[] ports = {8887, 8888, 8889, 7100};
    private boolean stop = false;
    private IBinder myBinder = new MyBinder();
    private Runnable[] networkRunnable = new Runnable[numPorts];
    private Handler updaterHandler = new Handler();
    private DatagramSocket[] sockets = new DatagramSocket[numPorts];
    private Runnable masterThread;
    private MessagePackage messagePackage = new MessagePackage();
    private FileOutputStream fos;

    private Map intersections =  new HashMap();
    private Viechle myCar= new Viechle();


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

                    messageSPAT = new String(dataSPAT, 0, dataSPAT.length);
                    try {
                        JSONObject json = new JSONObject(messageSPAT);

                        JSONArray json_inter_array = new JSONArray(json.getString("intersections"));
                        JSONObject json_inter = json_inter_array.getJSONObject(0);
                        String newID = json_inter.getString("id");

                        if (!intersections.containsKey(newID)) {
                            continue;
                        }

                        Intersection newData = (Intersection) intersections.get(newID);
                        JSONArray json_stat_array = new JSONArray(json_inter.getString("states"));

                        JSONObject json_stat_0 = json_stat_array.getJSONObject(0);
                        String lane = json_stat_0.getString("laneSet");
                        newData.currentState.put(lane, json_stat_0.getInt("currState"));
                        newData.timeToChange.put(lane, json_stat_0.getInt("timeToChange"));

                        intersections.put(newID, newData);

                        Log.d(TAG,"SPAT ID now: "+String.valueOf(((Intersection) intersections.get(newID)).ID));

                    } catch (JSONException e) {
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

                    messageMAP = new String(dataMAP, 0, dataMAP.length);
                    try {
                        JSONObject json = new JSONObject(messageMAP);

                        JSONArray json_inter_array = new JSONArray(json.getString("intersections"));
                        JSONObject json_inter = json_inter_array.getJSONObject(0);

                        Intersection newData = new Intersection();
                        newData.ID = json_inter.getString("id");

                        JSONObject json_ref = new JSONObject(json_inter.getString("refPoint"));
                        newData.centerLat = (double) json_ref.getInt("lat") / 10000000;
                        newData.centerLng = (double) json_ref.getInt("long") / 10000000;

                        JSONArray json_approaches_array = new JSONArray(json_inter.getString("approaches"));
                        for (int i = 0; i < 4; i++) {
                            JSONObject json_appr = json_approaches_array.getJSONObject(i);
                            json_appr = new JSONObject(json_appr.getString("approach"));
                            int id_tmp = json_appr.getInt("id");

                            Approach newApp = new Approach();
                            newApp.selfID = id_tmp;
                            newApp.selfName = json_appr.getString("name");

                            JSONArray json_driveLane_array = new JSONArray(json_appr.getString("drivingLanes"));
                            JSONObject json_drive = json_driveLane_array.getJSONObject(0);
                            newApp.lanewidth.put(json_drive.getString("laneNumber"), json_drive.getInt("laneWidth"));

                            newData.approaches.put(id_tmp, newApp);
                        }

                        intersections.put(newData.ID, newData);

                    } catch (JSONException e) {
                        e.printStackTrace();
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

                    messageBSM = new String(dataBSM, 0, dataBSM.length);
                    try {
                        Log.d(TAG,"BSM: "+messageBSM);
                        JSONObject json = new JSONObject(messageBSM);
                        String blob1 = json.getString("blob1");

                        myCar.update((double)String8ToInt(blob1.substring(14, 22)) / 1E7,
                                (double)String8ToInt(blob1.substring(22, 30)) / 1E7);

                        /*
                        myCar.speed = Integer.parseInt(blob1.substring(42, 46),16);
                        myCar.heading = (double)String4ToInt(blob1.substring(46, 50)) / 1E2;
                        */

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
            //RegexUtil regexUtil = new RegexUtil();

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
//                    if (MainActivity.test) message7100 = new String(data7100, 0, data7100.length);
//                    else {
                    message7100 = bytesToHexString(data7100);

                    Log.d(TAG,"7100: "+message7100);
                    /*
                    if (record) {
                        try {
                            fos.write(("7100 " + removeTail0(message7100)).getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    */
//                    }
                    message7100 = removeTail0(message7100);
                    if (message7100.startsWith("<ui_request>")) {
                        message7100 = message7100.substring(0, 317);
                    } else {
                        message7100 = message7100.substring(0, 255);
                    }
//                    Log.i(TAG, "run: " + message7100);
                    Log.d(TAG,"7100 at 20: "+message7100.charAt(20));
                    switch (message7100.charAt(20)) {
                        case '1':
                        case '2':
                        case '3':
                            /*
                            cancel = false;
                            hasEvent = true;
                            textV2V = regexUtil.getMatch(message7100, pText);
                            idV2V = message7100.charAt(20) - '0';
                            */
                            break;
                        case 't':
                            /*
                            cancel = true;
                            hasEvent = true;
                            */
                            break;
                        default:
                            //hasEvent = false;
                            break;
                    }
//                    Log.i(TAG, "run: " + textV2V);
                }
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
        if (record) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
