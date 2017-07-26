package com.example.mapdemo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by Nibius on 2017/6/29.
 */

public class ComuService extends Service {
    private static final String TAG = "nib";
    private static final String ACTION_RECV_MSG = "com.example.mapdemo.action.RECEIVE_MESSAGE";
    private static final String MESSAGE_OUT = "message_output";
    private static final String MESSAGE_IN = "message_input";
    private boolean quit = false, mapflag = false, tim_flag = false;
    private IBinder binder = new MyBinder();
    final int PORT[] = new int[]{8887, 8888, 8889, 8890};
    private int indexs[][][] = {
            {{156, 160}, {172, 176}, {188, 192}},
            {{272, 276}, {288, 292}, {304, 308}},
            {{390, 394}, {406, 410}, {422, 426}},
            {{506, 510}, {522, 526}, {538, 542}}
    };
    private DatagramSocket[] sockets = new DatagramSocket[4];
    private Runnable[] runnables = new Runnable[4];

    private String source, LightState, msgBSM, msgMAP, msgSPAT, MAP, msgTIM, TIMstate;
    private String message;
    private int event = 0, LightTime = 0;
    private double lat = 31.0228316014, lng = 121.4331369169, prev_lat = 0, prev_lng = 0, latR = 0, lngR = 0, latS = 0, lngS = 0, angle = 0, speed = 0, speed_sug = 0;
    private double latS_tmp = 0, lngS_tmp = 0;

    class MyBinder extends Binder {
        String getSource() {
            return source;
        }

        String getMsg() {
            return message;
        }

        int getEvent() {
            return event;
        }

        double getLatL() {
            return lat;
        }

        double getLngL() {
            return lng;
        }

        double getLatR() {
            return latR;
        }

        double getLngR() {
            return lngR;
        }

        double getLatStable() {
            return latS_tmp;
        }

        double getLngStable() {
            return lngS_tmp;
        }

        double getSpd() {
            return speed;
        }

        double getAngle() {
            return angle;
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
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.i("nib", "Service onStartCommand");
        quit = false;
        for (int i = 0; i < 4; i++) {
            try {
                sockets[i] = new DatagramSocket(PORT[i]);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        runnables[0] = new Runnable() {
            @Override
            public void run() {
//                Log.i("nib", "run: 1 " + System.currentTimeMillis());
                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                try {
                    sockets[0].receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] data = packet.getData();
                msgSPAT = bytesToHexString(data);
            }
        };
        runnables[1] = new Runnable() {
            @Override
            public void run() {
                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                try {
                    sockets[1].receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] data = packet.getData();
                msgMAP = bytesToHexString(data);
            }
        };
        runnables[2] = new Runnable() {
            @Override
            public void run() {
                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                try {
                    sockets[2].receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] data = packet.getData();
                msgBSM = bytesToHexString(data);
            }
        };
        runnables[3] = new Runnable() {
            @Override
            public void run() {
                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                try {
                    sockets[3].receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] data = packet.getData();
                msgTIM = bytesToHexString(data);
            }
        };
        new Thread() {
            public void run() {
                Log.i("nib", "Datagram socket");
                while (!quit) {
                    for (int i = 0; i < 4; i++) {
                        new Thread(runnables[i]).start();
                    }
//                        if (msgSPAT != null)
//                            Log.i("nib", "run: spat:" + removeTail0(msgSPAT));
//                        if (msgMAP != null)
//                            Log.i("nib", "run: map:" + removeTail0(msgMAP));
//                        if (msgBSM != null)
//                            Log.i("nib", "run: bsm:" + removeTail0(msgBSM));
//                        if (msgTIM != null)
//                            Log.i("nib", "run: tim:" + removeTail0(msgTIM));
                    if (msgBSM != null) {
                        lat = String8ToInt(msgBSM.substring(14, 22)) / 1E7;//自身经纬度
                        lng = String8ToInt(msgBSM.substring(22, 30)) / 1E7;
//                        Log.i("nib", "run: " + msgBSM + "\n" + msgBSM.substring(28, 36) + " " + msgBSM.substring(36, 44));
                        speed = Integer.parseInt(msgBSM.substring(42, 46), 16) * 0.02;//自身速度
                        angle = AngleUtil.getAngle(prev_lng, prev_lat, lng, lat);//行驶方向角
                        prev_lng = lng;
                        prev_lat = lat;
                    }
                    double TimDistance = 0;
                    if (msgTIM != null) {
                        latR = String8ToInt(msgTIM.substring(70, 78)) / 1E7;
                        lngR = String8ToInt(msgTIM.substring(82, 90)) / 1E7;
                        TimDistance = AngleUtil.getDistance(lng, lat, lngR, latR);
                        if (!tim_flag && (Math.abs(angle - AngleUtil.getAngle(lng, lat, lngR, latR)) < 45) && TimDistance < 200 && TimDistance > 190) {
                            event = 1;
                            TIMstate = msgTIM.substring(52, 54);
                        } else if (tim_flag && TimDistance < 190) {
                            tim_flag = false;
                            event = 0;
                        }
                    }
                    double LightDistance = 0, LightDistance_tmp;
                    if (msgMAP != null) {
                        latS_tmp = String8ToInt(msgMAP.substring(56, 64)) / 1E7;//msgMAP中交叉路口经纬度
                        lngS_tmp = String8ToInt(msgMAP.substring(68, 76)) / 1E7;
                        LightDistance_tmp = AngleUtil.getDistance(lng, lat, lngS_tmp, latS_tmp);
                        if ((Math.abs(angle - AngleUtil.getAngle(lng, lat, lngS_tmp, latS_tmp)) < 45) && LightDistance_tmp < 200
//                                && LightDistance_tmp > 140
                                ) { //此交叉路口是有效的

                            MAP = msgMAP;
                            latS = latS_tmp;
                            lngS = lngS_tmp;
                            mapflag = true;
                        } else {
                            LightDistance_tmp = AngleUtil.getDistance(lng, lat, lngS, latS);
                            if (!((Math.abs(angle - AngleUtil.getAngle(lng, lat, lngS, latS)) < 45) && LightDistance_tmp < 200
//                                    && LightDistance_tmp > 140
                            )) { //之前的有效交叉路口已经无效

                                mapflag = false;
                                event = 0;
                            }
                        }
                    }
                    if (msgSPAT != null && MAP != null && MAP.substring(44, 48).equals(msgSPAT.substring(22, 26)) && mapflag) {       //此信号信息和有效交叉路口一致
//                        Log.i("nib", "run: " + String4ToInt(MAP.substring(156, 160)) + " " + String4ToInt(MAP.substring(160, 164)));
//                        Log.i("nib", "angle: " + Math.abs(angle - AngleUtil.getAngleInt(0, 0, String4ToInt(MAP.substring(156, 160)), String4ToInt(MAP.substring(160, 164)))) + "\n"
//                                + Math.abs(angle - AngleUtil.getAngleInt(0, 0, String4ToInt(MAP.substring(272, 276)), String4ToInt(MAP.substring(276, 280)))) + "\n"
//                                + Math.abs(angle - AngleUtil.getAngleInt(0, 0, String4ToInt(MAP.substring(390, 394)), String4ToInt(MAP.substring(394, 398)))) + "\n"
//                                + Math.abs(angle - AngleUtil.getAngleInt(0, 0, String4ToInt(MAP.substring(522, 526)), String4ToInt(MAP.substring(526, 530)))));
//                        Log.i("nib", "state: " + msgSPAT.substring(50, 52) + "\n" +
//                                msgSPAT.substring(84, 86) + "\n" +
//                                msgSPAT.substring(118, 120) + "\n" +
//                                msgSPAT.substring(152, 154)
//                        );
                        double angles[][] = new double[4][4];
                        for (int i = 0; i < 4; i++) {
                            int sum[] = {0, 0};
                            for (int j = 0; j < 3; j++) {
                                sum[0] += String4ToInt(MAP.substring(indexs[i][j][0], indexs[i][j][0] + 4));
                                sum[1] += String4ToInt(MAP.substring(indexs[i][j][1], indexs[i][j][1] + 4));
                                angles[i][j] = AngleUtil.getAngleInt(0, 0, sum[0], sum[1]);
                            }
                            double result = 0;
                            int count = 0;
                            for (int j = 0; j < 3; j++) {
                                if (!Double.isNaN(angles[i][j])) {
                                    result += angles[i][j];
                                    count++;
                                }
                            }
                            angles[i][3] = result / count;
                            Log.i(TAG, "run: angles[" + i + "]= " + angles[i][3]);
                        }
                        Log.i(TAG, "run: angle= " + angle);
                        if (Math.abs(angle - angles[0][3]) < 15) {//车的方向角 和 红绿灯与北路上一点的方向角 基本相同
                            LightState = msgSPAT.substring(50, 52);
                            LightTime = Integer.parseInt(msgSPAT.substring(56, 58), 16);
                        } else if (Math.abs(angle - angles[1][3]) < 15) {//车的方向角 和 红绿灯与东路上一点的方向角 基本相同
                            LightState = msgSPAT.substring(84, 86);
                            LightTime = Integer.parseInt(msgSPAT.substring(90, 92), 16);
                        } else if (Math.abs(angle - angles[2][3]) < 15) {//车的方向角 和 红绿灯与南路上一点的方向角 基本相同
                            LightState = msgSPAT.substring(118, 120);
                            LightTime = Integer.parseInt(msgSPAT.substring(124, 126), 16);
                        } else if (Math.abs(angle - angles[3][3]) < 15) {//车的方向角 和 红绿灯与西路上一点的方向角 基本相同
                            LightState = msgSPAT.substring(152, 154);
                            LightTime = Integer.parseInt(msgSPAT.substring(158, 160), 16);
                        }
                        if (!"FF".equals(LightState)) {
                            event = 2;
                            LightDistance = AngleUtil.getDistance(lng, lat, lngS, latS);
                        }
                    }
//                    Log.i("nib", "S: " + lng + " " + lat + " " + lngS_tmp + " " + latS_tmp + " " + LightState + " " + event);
                    switch (event) {
                        case 1:
                            if ("01".equals(TIMstate))
                                message = "前方" + (int) TimDistance + "米有道路施工，请绕道行驶";

                            if ("02".equals(TIMstate))
                                message = "前方" + (int) TimDistance + "米为冰雪路况，请减速慢行";

                            tim_flag = true;
                            break;
                        case 2:
                            Log.i("nib", "Light: " + LightState + " " + LightTime + " " + LightDistance);
                            if ("02".equals(LightState)) {
                                if (LightDistance > speed * LightTime)
                                    message = "前方红灯剩余" + LightTime + "秒，建议保持原速通行";
                                else {
                                    speed_sug = (int) (((int) (speed - LightTime * 1.5 + Math.sqrt(1.5 * 1.5 * LightTime * LightTime - 2 * 1.5 * LightTime * speed + 2 * 1.5 * LightDistance)) * 3.6));
                                    if (speed_sug > 1)
                                        message = "前方红灯剩余" + LightTime + "秒，建议缓慢减速至" + speed_sug + "千米每时";
                                    else
                                        message = "前方红灯剩余" + LightTime + "秒，建议缓慢减速停车等候";
                                }
                            } else if ("01".equals(LightState)) {
                                if (LightDistance < speed * LightTime)
                                    message = "前方绿灯剩余" + LightTime + "秒，建议保持原速通行";
                                else
                                    message = "前方绿灯剩余" + LightTime + "秒，无法通过路口，建议停车等待";
                            }
                        default:
                            break;
                    }
                    Log.i("nib", "run: " + event + " " + message);
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
    }

    public int String8ToInt(String s) {
        byte[] latByte = hexString2Byte(s);
        return (byte2Int(latByte));
    }

    public int String4ToInt(String s) {
        if (s.charAt(0) >= '0' && s.charAt(0) <= '8')
            return String8ToInt("0000" + s);
        else return String8ToInt("ffff" + s);

    }

    private int byte2Int(byte[] b) {
        int intValue = 0;
        for (int i = 0; i < b.length; i++) {
            intValue += (b[i] & 0xFF) << (8 * (3 - i));
        }
        return intValue;
    }

    private byte[] hexString2Byte(String s) {
        int len = s.length();
        byte[] b = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            b[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16));
        }
        return b;
    }

    public String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public String removeTail0(String str) {
//      如果字符串尾部不为0，返回字符串
        if (!str.substring(str.length() - 1).equals("0")) {
            return str;
        } else {
//          否则将字符串尾部删除一位再进行递归
            return removeTail0(str.substring(0, str.length() - 1));
        }
    }
}
