package com.example.mapdemo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Nibius on 2017/6/29.
 */

public class ComuService extends Service {
    private static final String ACTION_RECV_MSG = "com.example.mapdemo.action.RECEIVE_MESSAGE";
    private static final String MESSAGE_OUT = "message_output";
    private static final String MESSAGE_IN = "message_input";
    private boolean quit = false;
    private boolean isIOThread = true;
    private IBinder binder = new MyBinder();
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

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("nib", "Service is created");
//        InputStream is = null;
//        try {
//            is = getAssets().open("simu.txt");
//        } catch (IOException e2) {
//            e2.printStackTrace();
//        }
//        InputStreamReader ireader = new InputStreamReader(is);
//        final BufferedReader reader = new BufferedReader(ireader);
//        new Thread() {
//            @Override
//            public void run() {
//                while (!quit) {
//                    JSONObject json;
//                    try {
//                        String line = reader.readLine();
//                        System.out.println(line);
//                        json = new JSONObject(line);
//                        Log.i("numb", json.toString());
//                        lat = json.getDouble("lat_l");
//                        lng = json.getDouble("long_l");
//                        event = json.getInt("event");
//                        latR = json.getDouble("lat_r");
//                        lngR = json.getDouble("long_r");
////                        Log.i("nib", String.valueOf(lat)+"\t"+String.valueOf(lng)+"\t"+String.valueOf(event)+"\t"+String.valueOf(latR)+"\t"+String.valueOf(lngR));
//                        //double latR1 = latR,lngR1 = lngR;
//
//                        latS = json.getDouble("lat_s");
//                        lngS = json.getDouble("long_s");
//                        speed = json.getInt("speed");
////                        Log.i("nib", String.valueOf(latS)+"\t"+String.valueOf(lngS)+"\t"+String.valueOf(speed));
//                        switch (event) {
//                            case 1:
//                                message = "��·ǰ���г������У�����ٻ�ĵ�";
//                                break;
//                            case 2:
//                                int BTD;
//                                BTD = (int) AngleUtil.getDistance(lng, lat, lngS, latS);
//                                message = "ǰ��" + BTD + "���к��̵�,���鳵��" + speed + "km/h";
//                                break;
//                            case 3:
//                                int SLD;
//                                SLD = (int) AngleUtil.getDistance(lng, lat, lngS, latS);
//                                message = "ǰ��" + SLD + "����������ʾ,����" + speed + "km/h";
//                                break;
//                            default:
//                                break;
//                        }
//                        Thread.sleep(100);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("nib", "Service onStartCommand");
        quit = false;
        new Thread() {
            public void run() {
                try {
                    serverSocket = new ServerSocket(SERVER_PORT);
                    Log.i("nib", "serverSocket");
                    while (!quit) {
                        try {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Socket socket = serverSocket.accept();
                                        Log.i("nib", "socket accepted");
                                        Log.i("nib", "new Thread running");
                                        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                                        isIOThread = true;
                                        while (isIOThread) {
                                            try {
                                                String msg = readFromSocket(in);
                                                NumString2Message numString2Message = new NumString2Message(msg);
                                                lat = numString2Message.getLat();
                                                lng = numString2Message.getLng();
                                                latS = numString2Message.getLatS();
                                                lngS = numString2Message.getLngS();
                                                event = numString2Message.getEvent();
//                                                JSONObject json = new JSONObject(msg);
//                                                lat = json.getDouble("lat_l");
//                                                lng = json.getDouble("long_l");
//                                                latR = json.getDouble("lat_r");
//                                                lngR = json.getDouble("long_r");
//                                                latS = json.getDouble("lat_s");
//                                                lngS = json.getDouble("long_s");
//                                                message = json.getString("message");
//                                                event = json.getInt("event");
//                                                speed = json.getInt("speed");
//                                                Log.i("nib", String.valueOf(lat) + "\t" + String.valueOf(lng) + "\t" + String.valueOf(event) + "\t" + String.valueOf(latR) + "\t" + String.valueOf(lngR));
//                                            } catch (JSONException e) {
//                                                e.printStackTrace();
//                                            }
                                            } catch (StringIndexOutOfBoundsException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Log.i("nib", e.toString());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        return super.onStartCommand(intent, flags, startId);
    }

    /* 从socket的输入流中读取数据 */
    private String readFromSocket(InputStream in) {
        int MAX_BUFFER_BYTES = 20480;
        String msg = "";
        byte[] tempbuffer = new byte[MAX_BUFFER_BYTES];
        try {
            int numReadedBytes = in.read(tempbuffer, 0, tempbuffer.length);
            msg = new String(tempbuffer, 0, numReadedBytes, "utf-8");
        } catch (Exception e) {
            Log.i("nib", "readFromSocket error");
            e.printStackTrace();
        }
        return msg;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("nib", "Service onBind");
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("nib", "service onDestroy");
        this.quit = true;
        isIOThread = false;
    }
}
