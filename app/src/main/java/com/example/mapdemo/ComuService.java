package com.example.mapdemo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;

/**
 * Created by Nibius on 2017/6/29.
 */

public class ComuService extends Service {
    private static final String ACTION_RECV_MSG = "com.example.mapdemo.action.RECEIVE_MESSAGE";
    private static final String MESSAGE_OUT = "message_output";
    private static final String MESSAGE_IN = "message_input";
    private boolean quit = false;
    private IBinder binder = new MyBinder();
    //    private int Index_data = 0;
    final int SERVER_PORT = 8888;
    ServerSocket serverSocket = null;
    private String message;
    private int event = 0, speed = 0;
    private double lat = 0, lng = 0, latR = 0, lngR = 0, latS = 0, lngS = 0;

    public class MyBinder extends Binder {
        public String getMsg() {
            return message;
        }

        public int getEvent() {
            return event;
        }

        public double getLatL() {
            return lat;
        }

        public double getLngL() {
            return lng;
        }

        public double getLatR() {
            return latR;
        }

        public double getLngR() {
            return lngR;
        }

        public double getLatStable() {
            return latS;
        }

        public double getLngStable() {
            return lngS;
        }

        public int getSpd() {
            return speed;
        }
    }

    public ComuService() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("nib", "Service is created");
        InputStream is = null;
        try {
            is = getAssets().open("simu.txt");
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        InputStreamReader ireader = new InputStreamReader(is);
        final BufferedReader reader = new BufferedReader(ireader);
        new Thread() {
            @Override
            public void run() {
                while (!quit) {
                    JSONObject json;
                    try {
                        String line = reader.readLine();
                        System.out.println(line);
                        json = new JSONObject(line);
                        Log.i("numb", json.toString());
                        lat = json.getDouble("lat_l");
                        lng = json.getDouble("long_l");
                        event = json.getInt("event");
                        latR = json.getDouble("lat_r");
                        lngR = json.getDouble("long_r");
//                        Log.i("nib", String.valueOf(lat)+"\t"+String.valueOf(lng)+"\t"+String.valueOf(event)+"\t"+String.valueOf(latR)+"\t"+String.valueOf(lngR));
                        //double latR1 = latR,lngR1 = lngR;

                        latS = json.getDouble("lat_s");
                        lngS = json.getDouble("long_s");
                        speed = json.getInt("speed");
//                        Log.i("nib", String.valueOf(latS)+"\t"+String.valueOf(lngS)+"\t"+String.valueOf(speed));
                        switch (event) {
                            case 1:
                                message = "��·ǰ���г������У�����ٻ�ĵ�";
                                break;
                            case 2:
                                int BTD;
                                BTD = (int) AngleUtil.getDistance(lng, lat, lngS, latS);
                                message = "ǰ��" + BTD + "���к��̵�,���鳵��" + speed + "km/h";
                                break;
                            case 3:
                                int SLD;
                                SLD = (int) AngleUtil.getDistance(lng, lat, lngS, latS);
                                message = "ǰ��" + SLD + "����������ʾ,����" + speed + "km/h";
                                break;
                            default:
                                break;
                        }
                        Thread.sleep(100);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

	/*
    @Override
    protected void onHandleIntent(Intent intent) {
        for (int i = 0; i < 5; i++) {
            String resultTxt = msgRecv + " "
                + DateFormat.format("MM/dd/yy hh:mm:ss", System.currentTimeMillis());
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(ACTION_RECV_MSG);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            MapData result = new MapData();
            Bundle bundle = new Bundle();
            //�����ݷŵ�Bundle����
            bundle.putSerializable(MESSAGE_OUT, result);
            broadcastIntent.putExtras(bundle);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
            SystemClock.sleep(1000);
        }

    }
	*/

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        Log.i("nib", "Service is binded");
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.quit = true;
    }
}
