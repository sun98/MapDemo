package cn.nibius.mapv2.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

import com.baidu.lbsapi.panoramaview.TextMarker;
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
import com.baidu.mapapi.map.Text;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;
import com.suke.widget.SwitchButton;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.nibius.mapv2.R;
import cn.nibius.mapv2.service.ComService;
import cn.nibius.mapv2.util.AngleUtil;
import cn.nibius.mapv2.util.Constant.V2VEvent;
import cn.nibius.mapv2.util.Constant.RoadStateEvent;
import cn.nibius.mapv2.util.Constant.LightEvent;
import cn.nibius.mapv2.util.Intersection;
import cn.nibius.mapv2.util.MessagePackage;
import cn.nibius.mapv2.util.MyLocationListener;
import cn.nibius.mapv2.util.ToastUtil;
import cn.nibius.mapv2.util.Viechle;

public class MainActivity extends AppCompatActivity {
    private String TAG = "MainActivity";
    private Context context;
    private double latOffset = 0.0043953298, lngOffset = 0.0110212588;
    private double myLat = 31.0278622712, myLng = 121.4218843711; // my position with initial value
    private Viechle myCar;
    private Map intersections;

    private MessagePackage messagePackage;
    private TextToSpeech textToSpeech;
    private MKOfflineMap mOffline;

    private int MAX_SOURCE = 4;
    private Marker[] markers = new Marker[MAX_SOURCE];
    private int MAX_TEXT = 4;
    private TextOptions[] textOptions = new TextOptions[MAX_TEXT];

    private Handler updaterHandler = new Handler(); // main handler
    private Runnable mapUpdater, messageUpdater;
    private ImageView imgVelocity, imgTraffic, imgRoad, imgV2v;
    private Button btnBind, btnMap;
    private SwitchButton switchButton, switchTest;
    private boolean isUpdating = true;
    private View.OnClickListener startListen, stopListen, toggleUpdater;
    private MapView mapView;
    private BaiduMap baiduMap;
    private ComService.MyBinder binder;
    private boolean isListening = false;
    private ServiceConnection connection;
    private LocationClient locationClient = null;
    private MyLocationListener myLocationListener = new MyLocationListener();

    public static Lock lock = new ReentrantLock();
    public static boolean test = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        initVariables();  // initialize variables
        initLocation(); // initialize and start location service, have some problems yet
        initMap();    // initialize map status
        initOfflineMap();   // download offline map for Shanghai
        bindService(new Intent(context, ComService.class), connection, BIND_AUTO_CREATE);    // bind service on activity created
    }

    private void initVariables() {
        context = getApplicationContext();
        mapView = findViewById(R.id.bmap);
        //textTip = findViewById(R.id.tip_text);
        imgVelocity = findViewById(R.id.img_velocity);
        imgTraffic = findViewById(R.id.img_traffic);
        imgRoad = findViewById(R.id.img_road);
        imgV2v = findViewById(R.id.img_v2v);
        switchButton = findViewById(R.id.switch_special);
        switchButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                BitmapDescriptor bitmap;
                if (isChecked)
                    bitmap = BitmapDescriptorFactory.fromResource(R.drawable.ic_navigation_red_24dp);
                else
                    bitmap = BitmapDescriptorFactory.fromResource(R.drawable.ic_navigation_black_24dp);
                markers[0].setIcon(bitmap);
            }
        });

        startListen = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binder.startListen();
                isListening = true;
                view.setOnClickListener(stopListen);
            }
        };
        stopListen = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binder.stopListen();
                isListening = false;
                view.setOnClickListener(startListen);
            }
        };
        toggleUpdater = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isUpdating) {
                    updaterHandler.removeCallbacks(mapUpdater);
                    updaterHandler.removeCallbacks(messageUpdater);
                    view.setBackgroundResource(R.drawable.play_icon);
                } else {
                    updaterHandler.post(mapUpdater);
                    updaterHandler.post(messageUpdater);
                    view.setBackgroundResource(R.drawable.pause_icon);
                }
                isUpdating = !isUpdating;
            }
        };

        btnBind = findViewById(R.id.btn_service);
        btnBind.setOnClickListener(startListen);
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

                        TextView showInfo = findViewById(R.id.text_info);
                        String showOff = "";

                        currentL = new LatLng(myCar.currentLat + latOffset, myCar.currentLng + lngOffset);
                        markers[0].setPosition(currentL);

                        MapStatus msu = new MapStatus.Builder(baiduMap.getMapStatus()).target(currentL).rotate((float)myCar.angle).build();
                        MapStatusUpdate msus = MapStatusUpdateFactory.newMapStatus(msu);
                        baiduMap.animateMapStatus(msus);

                        showOff += "本车位置: "+String.valueOf(myCar.currentLat + latOffset)+" "+String.valueOf(myCar.currentLng + lngOffset);
                        showOff += "\n速度: "+String.valueOf(myCar.speed)+" 方向角: "+String.valueOf(myCar.angle);

                        for(Object i : intersections.values()){
                            Intersection inter = (Intersection)i;
                            LatLng lightPos = new LatLng(inter.centerLat + latOffset,inter.centerLng + lngOffset);
                            markers[2].setPosition(lightPos);
                            markers[2].setVisible(true);

                            showOff += "\n收到的路口标识符: "+inter.ID;
                            showOff += "\n路口位置: "+String.valueOf(inter.centerLat + latOffset)+" "+String.valueOf(inter.centerLng + lngOffset);

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
                            }
                            else {
                                showOff += "\n信号灯状态: "+String.valueOf(last_state);
                            }

                            if(time != -1){
                                textOptions[0].text(String.valueOf(time)).position(lightPos);
                                baiduMap.addOverlay(textOptions[0]);
                                showOff += "\n信号灯剩余时间: "+String.valueOf(time);
                                last_time = time;
                            }
                            else {
                                showOff += "\n信号灯剩余时间: "+String.valueOf(last_time);
                            }
                            break;
                        }

                        showInfo.setText(showOff);

                    } catch (Exception e) {
                        Log.i(TAG, "MapUpdater: " + e.toString());
                    }
                } else {     // Service not started
                    currentL = myLocationListener.getLatLng();
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
                if (isListening) {
                    imgVelocity.setVisibility(View.VISIBLE);
                    imgTraffic.setVisibility(View.VISIBLE);
                    imgRoad.setVisibility(View.INVISIBLE);
                    imgV2v.setVisibility(View.INVISIBLE);
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

        LatLng tmp = new LatLng(0,0);
        textOptions[0] = new TextOptions().text("empty").position(tmp).fontSize(60);

        updaterHandler.post(messageUpdater);
        updaterHandler.post(mapUpdater);
    }

    private void initLocation() {   // initialize location service for joy(when service not started)
        locationClient.registerLocationListener(myLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系
        option.setScanSpan(1000);
        option.setIsNeedAddress(false);
        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps
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
        // 设置监听
        mOffline.init(new MKOfflineMapListener() {
            @Override
            public void onGetOfflineMapState(int type, int state) {
                switch (type) {
                    case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
                        // 离线地图下载更新事件类型
                        MKOLUpdateElement update = mOffline.getUpdateInfo(state);
                        Log.i(TAG, "offlineMap " + update.cityName + " ," + update.ratio);
                        if (update.ratio % 10 == 0)
                            ToastUtil.showShort(context, getString(R.string.download_offline) + update.ratio + "%");
                        break;
                    case MKOfflineMap.TYPE_NEW_OFFLINE:
                        // 有新离线地图安装
                        Log.i(TAG, "TYPE_NEW_OFFLINE");
                        break;
                    case MKOfflineMap.TYPE_VER_UPDATE:
                        // 版本更新提示
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
//        binder.stopListen();
//        isListening = false;
//        btnBind.setOnClickListener(startListen);
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }
}
