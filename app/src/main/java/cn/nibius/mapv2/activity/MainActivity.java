package cn.nibius.mapv2.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.LogoPosition;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.nibius.mapv2.R;
import cn.nibius.mapv2.service.ComService;
import cn.nibius.mapv2.util.Intersection;
import cn.nibius.mapv2.util.MessagePackage;
import cn.nibius.mapv2.util.MyLocationListener;
import cn.nibius.mapv2.util.ToastUtil;
import cn.nibius.mapv2.util.Vehicle;
import cn.nibius.mapv2.util.ViewController;


public class MainActivity extends AppCompatActivity {
    private String TAG = "MainActivity";
    private Context context;
    private double myLat = 31.0278622712, myLng = 121.4218843711;
    private Vehicle myCar;
    private Map intersections;
    private MessagePackage messagePackage;
    private TextToSpeech textToSpeech;
    private MKOfflineMap mOffline;
    private int MAX_SOURCE = 4;
    private Marker[] markers = new Marker[MAX_SOURCE];
    private int MAX_TEXT = 4;
    private TextOptions[] textOptions = new TextOptions[MAX_TEXT];
    private Handler updaterHandler = new Handler();
    private Runnable mapUpdater, messageUpdater;
    private ImageView imgVelocity, imgTraffic, imgRoad, imgV2v;
    private Button btnMap;
    private boolean isListening = false;
    private View.OnClickListener toggleUpdater;
    private MapView mapView;
    private BaiduMap baiduMap;
    private ComService.MyBinder binder;
    private ServiceConnection connection;
    private LocationClient locationClient = null;
    private MyLocationListener myLocationListener = new MyLocationListener();
    private ImageView canvasView;
    private Bitmap xCrossBitmap, tCrossBitmap, mapBitmap, carBitmap;
    private Canvas canvas;
    private Paint paint;
    private TextView tipView;


    public static Lock lock = new ReentrantLock();


    public static String getIP(Context context){

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address))
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (SocketException ex){
            ex.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        initVariables();
        initLocation();
        initMap();
        initOfflineMap();
        bindService(new Intent(context, ComService.class), connection, BIND_AUTO_CREATE);
    }


    private void initVariables() {
        context = getApplicationContext();
        mapView = findViewById(R.id.bmap);
        canvasView = findViewById(R.id.view_canvas);
        imgVelocity = findViewById(R.id.img_velocity);
        imgTraffic = findViewById(R.id.img_traffic);
        imgRoad = findViewById(R.id.img_road);
        imgV2v = findViewById(R.id.img_v2v);
        tipView = findViewById(R.id.tip_text);

        xCrossBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.xcross);
        tCrossBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.tcross);
        xCrossBitmap  = Bitmap.createScaledBitmap(xCrossBitmap, xCrossBitmap.getWidth()/2, xCrossBitmap.getHeight()/2, false);
        tCrossBitmap  = Bitmap.createScaledBitmap(tCrossBitmap, tCrossBitmap.getWidth()/2, tCrossBitmap.getHeight()/2, false);
        carBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_directions_car_black_48dp);
        paint = new Paint();

        TextView showInfo = findViewById(R.id.text_info);
        showInfo.setText("本机LAN IP: " + getIP(context));

        toggleUpdater = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isListening) {
                    binder.stopListen();
                    isListening = false;
                    view.setBackgroundResource(R.drawable.play_icon);
                    Log.i(TAG, "stop listen");
                } else {
                    binder.startListen();
                    isListening = true;
                    view.setBackgroundResource(R.drawable.pause_icon);
                    Log.i(TAG, "start listen");
                }
            }
        };

        btnMap = findViewById(R.id.btn_map);
        btnMap.setOnClickListener(toggleUpdater);

        baiduMap = mapView.getMap();
        locationClient = new LocationClient(getApplicationContext());

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int supported = textToSpeech.setLanguage(Locale.CHINESE);
                    if (supported != TextToSpeech.LANG_AVAILABLE && supported != TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                        ToastUtil.showShort(context, R.string.lang_not_support);
                    }
                }
            }
        });


        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.i(TAG, "onServiceConnected: ");
                binder = (ComService.MyBinder) iBinder;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                binder = null;
            }
        };


        mapUpdater = new Runnable() {
            LatLng currentL;
            int last_time = 0,last_state = 0;

            @Override
            public void run() {
                if (isListening) {
                    try {
                        lock.lock();
                        try {
                            messagePackage = binder.getPackage();
                        } finally {
                            lock.unlock();
                        }
                        myCar = messagePackage.getMyCar();
                        intersections = messagePackage.getIntersections();

                        ViewController vc = new ViewController(myCar);

                        if (vc.toChangeView() == 1){
                            mapBitmap = xCrossBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            canvas = new Canvas(mapBitmap);
                            canvas.drawBitmap(carBitmap,(float) vc.getViewLeft()*canvas.getWidth(),
                                    (float) vc.getViewTop()*canvas.getHeight(), paint);
                            canvasView.setImageBitmap(mapBitmap);
                            if(canvasView.getVisibility() == View.GONE)
                                canvasView.setVisibility(View.VISIBLE);
                            if(mapView.getVisibility() == View.VISIBLE)
                                mapView.setVisibility(View.GONE);

                        } else if (vc.toChangeView() == 2){
                            mapBitmap = tCrossBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            canvas = new Canvas(mapBitmap);
                            canvas.drawBitmap(carBitmap,(float) vc.getViewLeft()*canvas.getWidth(),
                                    (float) vc.getViewTop()*canvas.getHeight(), paint);
                            canvasView.setImageBitmap(mapBitmap);
                            if(canvasView.getVisibility() == View.GONE)
                                canvasView.setVisibility(View.VISIBLE);
                            if(mapView.getVisibility() == View.VISIBLE)
                                mapView.setVisibility(View.GONE);

                        } else {
                            if(canvasView.getVisibility() == View.VISIBLE)
                                canvasView.setVisibility(View.GONE);
                            if(mapView.getVisibility() == View.GONE)
                                mapView.setVisibility(View.VISIBLE);
                            canvas = null;
                            mapBitmap = null;

                            currentL = new LatLng(myCar.currentLat, myCar.currentLng);
                            Log.i(TAG, "Loc: "+String.valueOf(currentL));
                            /*
                            markers[0].setPosition(currentL);
                            MapStatus msu = new MapStatus.Builder(baiduMap.getMapStatus()).target(currentL).rotate((float)myCar.heading).build();
                            MapStatusUpdate msus = MapStatusUpdateFactory.newMapStatus(msu);
                            baiduMap.animateMapStatus(msus);
                            markers[2].setVisible(false);
                            */
                        }

                        TextView showInfo = findViewById(R.id.text_info);
                        String showOff = "本机LAN IP: " + getIP(context);

                        showOff += "\n本车位置: "+String.valueOf(myCar.currentLat)+" "+String.valueOf(myCar.currentLng);
                        showOff += "\n速度: "+String.valueOf(myCar.speed)+" 方向角: "+String.valueOf(myCar.heading);

                        for(Object i : intersections.values()){
                            Intersection inter = (Intersection)i;
                            LatLng lightPos = new LatLng(inter.centerLat,inter.centerLng);
                            markers[2].setPosition(lightPos);
                            markers[2].setVisible(true);

                            showOff += "\n收到的路口标识符: "+inter.ID;
                            showOff += "\n路口位置: "+String.valueOf(inter.centerLat)+" "+String.valueOf(inter.centerLng);
                            int time = -1, state = -1;
                            for(Object t : inter.timeToChange.values()){
                                time = (int)t;
                                break;
                            }
                            for(Object s : inter.currentState.values()){
                                state = (int)s;
                                break;
                            }
                            if(state != -1){
                                showOff += "\n信号灯状态: "+String.valueOf(state);
                                last_state = state;
                            } else {
                                showOff += "\n信号灯状态: "+String.valueOf(last_state);
                            }
                            if(time != -1){
                                textOptions[0].text(String.valueOf(time)).position(lightPos);
                                baiduMap.addOverlay(textOptions[0]);
                                showOff += "\n信号灯剩余时间: "+String.valueOf(time);
                                last_time = time;
                            } else {
                                showOff += "\n信号灯剩余时间: "+String.valueOf(last_time);
                            }
                            break;
                        }
                        showInfo.setText(showOff);

                    } catch (Exception e) {
                        Log.i(TAG, "MapUpdater: " + e.toString());
                    }

                } else {
                    currentL = myLocationListener.getLatLng();
                    Log.i(TAG, "currentLoc: "+String.valueOf(currentL));
                    MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(currentL);
                    baiduMap.setMapStatus(mapStatusUpdate);

                    markers[2].setVisible(false);
                    if (currentL != null)
                        markers[0].setPosition(currentL);
                    else {
                        markers[0].setPosition(new LatLng(myLat, myLng));
                    }
                }
                updaterHandler.postDelayed(this, 100);
            }
        };


        messageUpdater = new Runnable() {

            @Override
            public void run() {
                if (isListening && myCar != null && intersections != null) {
                    /*
                    imgVelocity.setVisibility(View.INVISIBLE);
                    imgTraffic.setVisibility(View.INVISIBLE);
                    imgRoad.setVisibility(View.INVISIBLE);
                    imgV2v.setVisibility(View.INVISIBLE);
                    */

                    ViewController vc = new ViewController(myCar);
                    boolean emergency = false;

                    switch (myCar.safetyMessage) {
                        case 1: // forward crash
                            imgRoad.setVisibility(View.VISIBLE);
                            imgV2v.setVisibility(View.VISIBLE);
                            tipView.setText("前方碰撞预警！");
                            emergency = true;
                            break;
                        case 2: // emergency break
                            imgRoad.setVisibility(View.VISIBLE);
                            imgV2v.setVisibility(View.VISIBLE);
                            tipView.setText("请紧急制动！");
                            emergency = true;
                            break;
                        case 3: // right side crash
                            imgRoad.setVisibility(View.VISIBLE);
                            imgV2v.setVisibility(View.VISIBLE);
                            tipView.setText("右侧碰撞预警！");
                            emergency = true;
                            break;
                        case 4: // left side crash
                            imgRoad.setVisibility(View.VISIBLE);
                            imgV2v.setVisibility(View.VISIBLE);
                            tipView.setText("左侧碰撞预警！");
                            emergency = true;
                            break;
                        case 0:
                            imgRoad.setVisibility(View.INVISIBLE);
                            imgV2v.setVisibility(View.INVISIBLE);
                            emergency = false;
                            break;
                        default:
                            imgRoad.setVisibility(View.INVISIBLE);
                            imgV2v.setVisibility(View.INVISIBLE);
                            emergency = false;
                            break;
                    }

                    try {
                        if (!emergency && vc.toChangeView() == 1) {
                            imgTraffic.setVisibility(View.VISIBLE);
                            tipView.setText(R.string.cross);

                        } else if (!emergency && vc.toChangeView() == 2) {
                            imgTraffic.setVisibility(View.VISIBLE);
                            tipView.setText(R.string.cross);

                            Log.i(TAG, "inter: " + vc.pointedIntersection(intersections));
                            Intersection nextInter = (Intersection) intersections.get(
                                    vc.pointedIntersection(intersections));
                            int currentLane = vc.pointedLane(nextInter);
                            Log.i(TAG, "currentLane: " + String.valueOf(currentLane));
                            int currentState = (int) nextInter.currentState.get(currentLane);
                            int timeToChange = (int) nextInter.timeToChange.get(currentLane);
                            tipView.setText("前方红绿灯状态：" + String.valueOf(currentState)
                                    + "剩余时间：" + String.valueOf(timeToChange) + "秒");

                        } else if (!emergency) {
                            Intersection nextInter = (Intersection) intersections.get(
                                    vc.pointedIntersection(intersections));
                            int currentLane = vc.pointedLane(nextInter);
                            int currentState = (int) nextInter.currentState.get(currentLane);
                            int timeToChange = (int) nextInter.timeToChange.get(currentLane);
                            imgTraffic.setVisibility(View.INVISIBLE);
                            tipView.setText("前方红绿灯状态：" + String.valueOf(currentState)
                                    + "剩余时间：" + String.valueOf(timeToChange) + "秒");
                        }
                    } catch (Exception e) {
                        Log.i(TAG, "MessageUpdater: " + e.toString());
                    }
                }
                updaterHandler.postDelayed(this, 100);
            }
        };
    }

    private void initMap() {
        mapView.showScaleControl(false);
        mapView.showZoomControls(false);
        mapView.setLogoPosition(LogoPosition.logoPostionRightBottom);
        MapStatusUpdate mapStatusUpdate;
        LatLng position = new LatLng(myLat, myLng);
        mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(position, 255);
        baiduMap.setMapStatus(mapStatusUpdate);
        UiSettings uiSettings = baiduMap.getUiSettings();
        uiSettings.setCompassEnabled(true);
        uiSettings.setOverlookingGesturesEnabled(false);

        BitmapDescriptor bitmapDescriptor;
        bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_navigation_black_24dp);
        OverlayOptions options = new MarkerOptions().position(position).icon(bitmapDescriptor);
        markers[0] = (Marker) baiduMap.addOverlay(options);
        markers[0].setVisible(true);

        int resourceArray[] = {R.drawable.ic_bullseye_black_24dp, R.drawable.ic_traffic_black_24dp, R.drawable.ic_warning_black_24dp};
        for (int i = 1; i < MAX_SOURCE; i++) {
            bitmapDescriptor = BitmapDescriptorFactory.fromResource(resourceArray[i - 1]);
            options = new MarkerOptions().position(position).icon(bitmapDescriptor); // 设置Overlay图标
            markers[i] = (Marker) (baiduMap.addOverlay(options)); // 将Marker添加到地图上。
            markers[i].setVisible(false);    //  先将Marker隐藏。在获得对应位置信息的时候再行显示。
        }

        updaterHandler.post(messageUpdater);
        updaterHandler.post(mapUpdater);
    }

    private void initLocation() {
        locationClient.registerLocationListener(myLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        option.setIsNeedAddress(false);
        option.setOpenGps(true);
        option.setLocationNotify(true);
        option.setIsNeedLocationDescribe(false);
        option.setIsNeedLocationPoiList(false);
        option.setIgnoreKillProcess(true);
        option.SetIgnoreCacheException(true);
        option.setEnableSimulateGps(false);

        locationClient.setLocOption(option);
        locationClient.start();
    }

    private void initOfflineMap() {
        Log.i(TAG, "initOfflineMap: ");
        mOffline = new MKOfflineMap();
        mOffline.init(new MKOfflineMapListener() {
            @Override
            public void onGetOfflineMapState(int type, int state) {
                switch (type) {
                    case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
                        MKOLUpdateElement update = mOffline.getUpdateInfo(state);
                        Log.i(TAG, "offlineMap " + update.cityName + " ," + update.ratio);
                        if (update.ratio % 10 == 0)
                            ToastUtil.showShort(context, getString(R.string.download_offline) + update.ratio + "%");
                        break;
                    case MKOfflineMap.TYPE_NEW_OFFLINE:
                        Log.i(TAG, "TYPE_NEW_OFFLINE");
                        break;
                    case MKOfflineMap.TYPE_VER_UPDATE:
                        break;
                }
            }
        });

        mOffline.start(289);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        binder.stopListen();
        isListening = false;
        updaterHandler.removeCallbacks(mapUpdater);
        updaterHandler.removeCallbacks(messageUpdater);
        unbindService(connection);
        super.onDestroy();
        textToSpeech.shutdown();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause: ");
        binder.stopListen();
        isListening = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }
}
