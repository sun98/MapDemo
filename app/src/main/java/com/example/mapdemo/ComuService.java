package com.example.mapdemo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
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

    private String source, LightState, msgBSM, msgMAP, msgSPAT, msgTIM, TIMstate;
    private String message = "前方绿灯时间较长";
    private int event = 4, LightTime = 0;
    private int event_lock = -1;
    private double flagDistance = 1e6;
    private double LightDistance = 0, LightDistance_tmp = 0;
    private double TimDistance = 0;
    private double lat = 31.0275818566, lng = 121.4220209306, prev_lat = 0, prev_lng = 0, latR = 31.0290615881, lngR = 121.4257645656, latS = 0, lngS = 0, angle = 0, speed = 0, speed_sug = 0;
    private double latS_tmp = 31.0278092865, lngS_tmp = 121.4219172864;

    class MyBinder extends Binder {
        int getEventLock() {
            return event_lock;
        }

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
        final boolean test = false;
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
                while (!quit) {
                    DatagramPacket packetSPAT = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[0].receive(packetSPAT);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataSPAT = packetSPAT.getData();
                    if (test) msgSPAT = new String(dataSPAT, 0, dataSPAT.length);
                    else msgSPAT = bytesToHexString(dataSPAT);
//                    XLog.i("get spat:\t" + msgSPAT);
//                    Log.i(TAG, "\t" + msgMAP.substring(44, 48) + "\t" + msgSPAT.substring(22, 26));
                    if (msgSPAT != null && msgMAP != null && msgMAP.substring(44, 48).equals(msgSPAT.substring(22, 26)) && "007a".equals(msgSPAT.substring(22, 26))) {//此信号信息和有效交叉路口一致
                        double lightAngle = AngleUtil.getAngle(lng, lat, lngS_tmp, latS_tmp);
                        double angleDiffe = Math.abs(angle - lightAngle);
                        if (angleDiffe > 180)
                            angleDiffe = 360 - angleDiffe;
                        LightDistance = AngleUtil.getDistance(lng, lat, lngS_tmp, latS_tmp);
//                        Log.i(TAG, "angle= " + angleDiffe + "\tdistance" + LightDistance);
                        if (!"FF".equals(msgSPAT.substring(44, 46)) && angleDiffe < 45 && LightDistance < 3) {
                            event = 0;
                            flagDistance = 1e6;
                        } else if (!"FF".equals(msgSPAT.substring(44, 46)) && angleDiffe < 45 && LightDistance < flagDistance && LightDistance < 200) {
                            event = 2;
                            //LightDistance = AngleUtil.getDistance(lng, lat, lngS, latS);
                            flagDistance = LightDistance;
                            double angles[][] = new double[4][4];
                            for (int i = 0; i < 4; i++) {
                                int sum[] = {0, 0};
                                for (int j = 0; j < 3; j++) {
                                    sum[0] += String4ToInt(msgMAP.substring(indexs[i][j][0], indexs[i][j][0] + 4));
                                    sum[1] += String4ToInt(msgMAP.substring(indexs[i][j][1], indexs[i][j][1] + 4));
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
//                            XLog.i("run: angles[" + i + "]= " + angles[i][3]);
                            }
//                        XLog.i("run: angle= " + angle);
                            int minIndex = -1;
                            double deltaAngle[] = new double[4], minDelta = 360;
                            for (int i = 0; i < 4; i++) {
                                double t = Math.abs(angle - angles[i][3]);
                                deltaAngle[i] = (t > 180) ? (360 - t) : t;
                                if (deltaAngle[i] < minDelta) {
                                    minDelta = deltaAngle[i];
                                    minIndex = i;
                                }
                            }
                            if (minIndex == 0) {//车的方向角 和 红绿灯与北路上一点的方向角 基本相同
                                LightState = msgSPAT.substring(50, 52);
                                LightTime = Integer.parseInt(msgSPAT.substring(56, 58), 16);
                            } else if (minIndex == 1) {//车的方向角 和 红绿灯与东路上一点的方向角 基本相同
                                LightState = msgSPAT.substring(84, 86);
                                LightTime = Integer.parseInt(msgSPAT.substring(90, 92), 16);
                            } else if (minIndex == 2) {//车的方向角 和 红绿灯与南路上一点的方向角 基本相同
                                LightState = msgSPAT.substring(50, 52);
                                LightTime = Integer.parseInt(msgSPAT.substring(56, 58), 16);
//                            LightState = msgSPAT.substring(118, 120);
//                            LightTime = Integer.parseInt(msgSPAT.substring(124, 126), 16);
                            } else if (minIndex == 3) {//车的方向角 和 红绿灯与西路上一点的方向角 基本相同
                                LightState = msgSPAT.substring(84, 86);
                                LightTime = Integer.parseInt(msgSPAT.substring(90, 92), 16);
//                            LightState = msgSPAT.substring(152, 154);
//                            LightTime = Integer.parseInt(msgSPAT.substring(158, 160), 16);
                            } else {
//                                Log.w(TAG, "run: minIndex = " + minIndex);
                            }
                        } else {
                            event = 0;
                        }
                    }
//                    Log.i("nib", "S: " + lng + " " + lat + " " + lngS_tmp + " " + latS_tmp + " " + LightState + " " + event);
//                    XLog.i("event: " + event + "\tmessage: " + message);
                    switch (event) {
                        case 5:

                            if ("01".equals(TIMstate))
                                message = "前方" + (int) TimDistance + "米有道路施工，请绕道行驶";
                            if ("02".equals(TIMstate))
                                message = "前方" + (int) TimDistance + "米为冰雪路况，请减速慢行";
                            tim_flag = true;
                            break;
                        case 2:
//                            XLog.i("LightState: " + LightState + "\tLightTime: " + LightTime + "\tLightDistance: " + LightDistance);
                            if ("02".equals(LightState)) {
                                if (LightTime == 127) {
                                    message = "前方红灯时间较长";//多于8秒
                                }
                            } else if ("01".equals(LightState)) {
                                if (LightTime == 127) {
                                    message = "前方绿灯时间较长"; //多于10秒
                                }
                            } else if ("04".equals(LightState)) {
                                if (LightTime != 127) {
                                    event = 3;
                                    if (LightDistance < speed * LightTime)
                                        message = "前方绿灯剩余" + LightTime + "秒，建议保持原速通行";
                                    else
                                        message = "前方绿灯剩余" + LightTime + "秒，无法通过路口，建议停车等待";
                                } else {
                                    message = "前方红灯时间较长";
                                }
                            } else if ("05".equals(LightState)) {
                                event = 4;
//                                Log.i(TAG, "LightTime = " + LightTime + "\tspeed" + speed + "\tLightDistance" + LightDistance);
                                if (LightTime != 127) {
                                    if (LightDistance > speed * LightTime)
                                        message = "前方红灯剩余" + LightTime + "秒，建议保持原速通行";
                                    else {
                                        speed_sug = (int) (((int) (speed - LightTime * 1.5 + Math.sqrt(1.5 * 1.5 * LightTime * LightTime - 2 * 1.5 * LightTime * speed + 2 * 1.5 * LightDistance)) * 3.6));
                                        if (speed_sug > 1)
                                            message = "前方红灯剩余" + LightTime + "秒，建议缓慢减速至" + speed_sug + "km/h";
                                        else
                                            message = "前方红灯剩余" + LightTime + "秒，建议缓慢减速停车等候";
                                    }
                                } else {
                                    message = "前方绿灯时间较长";
                                }
                            }
//                            else if ("03".equals(LightState)) {
//                                message = "前方黄灯";
//                            }
                            break;
                        default:
                            break;
                    }
                    event_lock = event;
                }
            }
        };
        runnables[1] = new Runnable() {
            @Override
            public void run() {
                while (!quit) {
                    DatagramPacket packetMAP = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[1].receive(packetMAP);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataMAP = packetMAP.getData();
                    if (test)
                        msgMAP = new String(dataMAP, 0, dataMAP.length);
                    else
                        msgMAP = bytesToHexString(dataMAP);
//                    XLog.i("get map:\t" + msgMAP);
                    if (msgMAP != null && "007a".equals(msgMAP.substring(44, 48))) {
                        latS_tmp = (int) String8ToInt(msgMAP.substring(56, 64)) / 1E7;//msgMAP中交叉路口经纬度
                        lngS_tmp = (int) String8ToInt(msgMAP.substring(68, 76)) / 1E7;
//                        LightDistance_tmp = AngleUtil.getDistance(lng, lat, lngS_tmp, latS_tmp);
                    }
                }
            }
        };
        runnables[2] = new Runnable() {
            @Override
            public void run() {
                while (!quit) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[2].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataBSM = packet.getData();
                    if (test) msgBSM = new String(dataBSM, 0, dataBSM.length);
                    else msgBSM = bytesToHexString(dataBSM);
//                    XLog.i("get bsm:\t" + msgBSM);
                    if (msgBSM != null) {
                        Double new_lat = String8ToInt(msgBSM.substring(14, 22)) / 1E7;//自身经纬度
                        Double new_lng = String8ToInt(msgBSM.substring(22, 30)) / 1E7;
//                        Log.i(TAG, "new lat: " + new_lat + "\tnew lng: " + new_lng + "\nlat: " + lat + "\tlng: " + lng);
                        if (Math.abs(new_lat - lat) > 1e-6 && Math.abs(new_lng - lng) > 1e-6) {
//                            Log.i(TAG, "yes: ");
                            lat = new_lat;
                            lng = new_lng;
                        }
                        speed = Integer.parseInt(msgBSM.substring(42, 46), 16) % Integer.parseInt("10000000000000", 2) * 0.02;//自身速度
                        angle = AngleUtil.getAngle(prev_lng, prev_lat, lng, lat);//行驶方向角
//                        Log.i(TAG, "run: angle = " + angle);
                        prev_lng = lng;
                        prev_lat = lat;
                    }
                }
            }
        };
        runnables[3] = new Runnable() {
            @Override
            public void run() {
                while (!quit) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    try {
                        sockets[3].receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] dataTIM = packet.getData();
                    if (test) msgTIM = new String(dataTIM, 0, dataTIM.length);
                    else msgTIM = bytesToHexString(dataTIM);
//                    XLog.i("get tim:\t" + msgTIM);
                    if (msgTIM != null) {
                        latR = String8ToInt(msgTIM.substring(70, 78)) / 1E7;
                        lngR = String8ToInt(msgTIM.substring(82, 90)) / 1E7;
                        TimDistance = AngleUtil.getDistance(lng, lat, lngR, latR);
                        Double timAngle = Math.abs(angle - AngleUtil.getAngle(lng, lat, lngR, latR));
                        timAngle = (timAngle > 180) ? (360 - timAngle) : timAngle;
                        if (!tim_flag && (timAngle < 45) && TimDistance < 200 && TimDistance > 50) {
//                            Log.i(TAG, "run: timAngle=" + timAngle + "\ttimDistance" + TimDistance);
                            event = 5;
                            TIMstate = msgTIM.substring(52, 54);
                        } else if (tim_flag && TimDistance < 50) {
                            tim_flag = false;
                            event = 0;
                        }
                    }
                }
            }
        };
        Log.i("nib", "onStartCommand");
        for (int i = 0; i < 4; i++) {
            new Thread(runnables[i]).start();
        }
        return super.onStartCommand(intent, flags, startId);
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
