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
import cn.nibius.mapv2.util.Viechle;
import cn.nibius.mapv2.util.ViewController;


public class MainActivity extends AppCompatActivity {
    private String TAG = "MainActivity";
    private Context context;

    private double myLat = 31.0278622712, myLng = 121.4218843711;
    private Viechle myCar;
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
    private Button btnBind, btnMap;

    private boolean isUpdating = false;
    private View.OnClickListener startListen, stopListen, toggleUpdater;
    private MapView mapView;
    private BaiduMap baiduMap;
    private ComService.MyBinder binder;
    private boolean isListening = false;
    private ServiceConnection connection;
    private LocationClient locationClient = null;
    private MyLocationListener myLocationListener = new MyLocationListener();

    private ImageView canvasView;
    private Bitmap xCrossBitmap, tCrossBitmap, mapBitmap, carBitmap;
    private Canvas canvas;
    private Paint paint;
    private TextView tipView;

    public static Lock lock = new ReentrantLock();
    public static boolean test = false;

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
        carBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_directions_car_black_48dp);
        paint = new Paint();

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


        mapUpdater = new Runnable() { // main thread updating view
            LatLng currentL;
            int last_time = 0,last_state = 0;

            @Override
            public void run() {
                if (isListening) {
                    try {
                        // get message from binder
                        lock.lock();
                        try {
                            messagePackage = binder.getPackage();
                        } finally {
                            lock.unlock();
                        }

                        myCar = messagePackage.getMyCar();
                        intersections = messagePackage.getIntersections();

                        // view change
                        ViewController vc = new ViewController();
                        if (vc.toChangeView(myCar) == 1){
                            tipView.setText(R.string.cross);
                            mapView.setVisibility(View.GONE);
                            mapBitmap = xCrossBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            canvas = new Canvas(mapBitmap);

                            double x = vc.tgetViewLeft(31.027853,121.421893);
                            double y = vc.tgetViewTop(31.027853,121.421893);

                            Log.d(TAG,"x: "+String.valueOf(x)+" y: "+String.valueOf(y));

                            canvas.drawBitmap(carBitmap,(float) x*canvas.getWidth()/1920-24,
                                    (float) y*canvas.getHeight()/1080-24, paint);
                            canvasView.setImageBitmap(mapBitmap);
                            canvasView.setVisibility(View.VISIBLE);
                        } else if (vc.toChangeView(myCar) == 2){
                            tipView.setText(R.string.cross);
                            mapView.setVisibility(View.GONE);
                            mapBitmap = tCrossBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            canvas = new Canvas(mapBitmap);
                            canvas.drawBitmap(carBitmap,200,200,paint);
                            canvasView.setImageBitmap(mapBitmap);
                            canvasView.setVisibility(View.VISIBLE);
                        } else {
                            tipView.setText(R.string.welcome);
                            canvasView.setVisibility(View.GONE);
                            mapView.setVisibility(View.VISIBLE);
                            canvas = null;
                            mapBitmap = null;
                        }

                        // updating map
                        currentL = new LatLng(myCar.currentLat, myCar.currentLng);
                        markers[0].setPosition(currentL);

                        MapStatus msu = new MapStatus.Builder(baiduMap.getMapStatus()).target(currentL).rotate((float)myCar.heading).build();
                        MapStatusUpdate msus = MapStatusUpdateFactory.newMapStatus(msu);
                        baiduMap.animateMapStatus(msus);

                        // show info from message
                        TextView showInfo = findViewById(R.id.text_info);
                        String showOff = "";

                        showOff += "本车位置: "+String.valueOf(myCar.currentLat)+" "+String.valueOf(myCar.currentLng);
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
                imgVelocity.setVisibility(View.VISIBLE);
                imgTraffic.setVisibility(View.VISIBLE);
                imgRoad.setVisibility(View.VISIBLE);
                imgV2v.setVisibility(View.VISIBLE);

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
        btnBind.setOnClickListener(startListen);
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }
}
