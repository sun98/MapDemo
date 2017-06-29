package com.example.mapdemo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ComService extends Service {

    public static final String TAG = "nib";
    public static Boolean mainThreadFlag = true;
    public static Boolean ioThreadFlag = true;
    ServerSocket serverSocket = null;
    final int SERVER_PORT = 8888;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private MyBinder binder = new MyBinder();
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
        Log.i(TAG, "Service onCreate");
    }

    private void doListen() {
        serverSocket = null;
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            while (mainThreadFlag) {
                final Socket socket = serverSocket.accept();
//                new Thread(new ThreadReadWriterIOSocket(getApplicationContext(), socket)).start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                            ioThreadFlag = true;
                            while (ioThreadFlag) {
                                try {
                                    if (!socket.isConnected()) break;
                                    String message = readFromSocket(in);
                                    JSONObject json = new JSONObject(message);
                                    lat = json.getDouble("lat_l");
                                    lng = json.getDouble("long_l");
                                    latR = json.getDouble("lat_r");
                                    lngR = json.getDouble("long_r");
                                    latS = json.getDouble("lat_s");
                                    lngS = json.getDouble("long_s");
                                    message = json.getString("message");
                                    event = json.getInt("event");
                                    speed = json.getInt("speed");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (socket != null) {
                                    socket.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            Log.v("nib", "readFromSocket error");
            e.printStackTrace();
        }
        return msg;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service.onStartCommand()");
        mainThreadFlag = true;
        new Thread() {
            public void run() {
                doListen();
            }
        }.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 关闭线程
        mainThreadFlag = false;
        ioThreadFlag = false;
        // 关闭服务器
        try {
            Log.v(TAG, "serverSocket.close()");
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "Service onDestroy");
    }

}
