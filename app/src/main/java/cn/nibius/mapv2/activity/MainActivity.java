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
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;
import com.suke.widget.SwitchButton;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.nibius.mapv2.R;
import cn.nibius.mapv2.service.ComService;
import cn.nibius.mapv2.util.AngleUtil;
import cn.nibius.mapv2.util.Constant.V2VEvent;
import cn.nibius.mapv2.util.Constant.RoadStateEvent;
import cn.nibius.mapv2.util.Constant.LightEvent;
import cn.nibius.mapv2.util.MessagePackage;
import cn.nibius.mapv2.util.MyLocationListener;
import cn.nibius.mapv2.util.ToastUtil;

public class MainActivity extends AppCompatActivity {
    private String TAG = "MainActivity";
    private Context context;
    private double latOffset = 0.0043953298, lngOffset = 0.0110212588;
    private double myLat = 31.0278622712, myLng = 121.4218843711; // my position with initial value
    private double obsLat = 0, obsLng = 0, lightLat = 0, lightLng = 0, otherLat = 0, otherLng = 0;
    private MessagePackage messagePackage;
    private LightEvent currentLightEvent = LightEvent.NOLIGHT;
    private RoadStateEvent currentRoadStateEvent = RoadStateEvent.NOROADSTATE;
    private V2VEvent currentV2VEvent = V2VEvent.NOV2V;
    private String currentMessage = "";
    private double currentSpeed = 0;
    private TextToSpeech textToSpeech;
    private MKOfflineMap mOffline;
    private int MAX_SOURCE = 4; // max number of markers
    private Marker[] markers = new Marker[MAX_SOURCE];  // markers
    private Handler updaterHandler = new Handler(); // main handler
    private Runnable mapUpdater, messageUpdater;
    private TextView textTip;   // tip text
    private ImageView imgVelocity, imgTraffic, imgRoad, imgV2v;
    private Button btnBind, btnMap;
    private SwitchButton switchButton, switchTest;
    private boolean isUpdating = true;
    private View.OnClickListener startListen, stopListen, toggleUpdater;
    private MapView mapView; // mapView object
    private BaiduMap baiduMap;  // map object
    private ComService.MyBinder binder; // binder to the ComService
    private boolean isListening = false; // whether service is bound
    private ServiceConnection connection;
    private LocationClient locationClient = null; // core class of location service
    private MyLocationListener myLocationListener = new MyLocationListener(); // interface of location service

    public static Lock lock = new ReentrantLock();
    public static boolean test = false;         // whether using pc and pad to test or real practice

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
        textTip = findViewById(R.id.tip_text);
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
        switchTest = findViewById(R.id.switch_test);
        switchTest.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                test = isChecked;
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
            float oldAngle = 0, currentAngle = 0;
            double oldMyLat = 0, oldMyLng = 0, oldOtherLat = 0, oldOtherLng = 0;
            LatLng currentL;

            @Override
            public void run() {
                if (isListening) { // service started
                    try {
                        lock.lock();
                        try {
                            messagePackage = binder.getPackage();
                        } finally {
                            lock.unlock();
                        }
                        // handle map position update
                        myLat = messagePackage.getCurrentLat() + latOffset;
                        myLng = messagePackage.getCurrentLng() + lngOffset;

                        // IMPORTANT: get event here simultaneously to avoid error
                        currentLightEvent = messagePackage.getCurrentLightEvent();
                        currentRoadStateEvent = messagePackage.getCurrentRoadStateEvent();
                        currentV2VEvent = messagePackage.getCurrentV2VEvent();
                        currentMessage = messagePackage.getMessage();
                        currentSpeed = messagePackage.getCurrentSpeed();
                        currentAngle = (float) messagePackage.getCurrentAngle();

                        if (currentRoadStateEvent != RoadStateEvent.NOROADSTATE) {
                            obsLat = messagePackage.getEffectiveLatR();
                            obsLng = messagePackage.getEffectiveLngR();
//                            Log.i(TAG, "run: event=5, obsLat=" + obsLat + ", obsLng=" + obsLng);
                        }
                        if (currentLightEvent != LightEvent.NOLIGHT) {
                            lightLat = messagePackage.getEffectiveLatS();
                            lightLng = messagePackage.getEffectiveLngS();
//                            Log.i(TAG, "run: event=" + currentEvent + ", lightLat=" + lightLat + ", lightLng=" + lightLng);
                        }
//                        if (currentV2VEvent != V2VEvent.NOV2V) {
//                        oldOtherLat = otherLat;
//                        oldOtherLng = otherLng;
//                        otherLat = messagePackage.getOtherLat();
//                        otherLng = messagePackage.getOtherLng();
//                        Log.i(TAG, "run: " + otherLat + " " + otherLng);
//                        }

                        Log.i(TAG, "Now position: " + String.valueOf(myLat) + " " + String.valueOf(myLng));
                        currentL = new LatLng(myLat, myLng);
                        markers[0].setPosition(currentL);
                        if (oldMyLat != 0 || oldMyLng != 0) {  //not initial state
                            MapStatus.Builder builder = new MapStatus.Builder();
                            if (myLat != oldMyLat || myLng != oldMyLng) { // moving occurs
                                float d = Math.abs(currentAngle - oldAngle);
                                d = (d > 180) ? (360 - d) : d;
                                if (d > 5 && currentSpeed > 0.2) {
                                    builder.rotate(currentAngle);
                                } else {
                                    currentAngle = oldAngle;
                                }
                            }
                            builder.target(currentL);
                            oldAngle = currentAngle;
                            MapStatus mapStatus = builder.build();
                            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
                            baiduMap.animateMapStatus(mapStatusUpdate);
                        }
                        oldMyLat = myLat;
                        oldMyLng = myLng;
                        // handle event marker update
                        if (currentLightEvent == LightEvent.NOLIGHT &&
                                currentRoadStateEvent == RoadStateEvent.NOROADSTATE
//                                currentV2vEvent
                                ) {
                            for (int i = 1; i < MAX_SOURCE; i++) {
                                markers[i].setVisible(false);
                            }
                        }
                        if (currentRoadStateEvent != RoadStateEvent.NOROADSTATE) {
                            markers[3].setPosition(new LatLng(obsLat + latOffset, obsLng + lngOffset));
                            markers[3].setVisible(true);
                        }
                        if (currentLightEvent != LightEvent.NOLIGHT) {
                            markers[2].setPosition(new LatLng(lightLat + latOffset, lightLng + lngOffset));
                            markers[2].setVisible(true);
                        }
//                        if (currentV2VEvent != V2VEvent.NOV2V) {
//                        markers[1].setPosition(new LatLng(otherLat + latOffset, otherLng + lngOffset));
//                        float otherAngle = (float) AngleUtil.getAngle(oldOtherLng, oldOtherLat, otherLng, otherLat);
//                        markers[1].setRotate(otherAngle-currentAngle);
//                        markers[1].setVisible(true);
//                        }
                    } catch (Exception e) {
                        Log.i(TAG, "MapUpdater: " + e.toString());
                    }
                } else { // service not started
                    currentL = myLocationListener.getLatLng();
//                    Log.i(TAG, "run: currentL" + currentL);
                    MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(currentL);
                    baiduMap.setMapStatus(mapStatusUpdate);
                    if (currentL != null)
                        markers[0].setPosition(currentL);
                    else {
//                        ToastUtil.showShort(context, R.string.location_error);
                        markers[0].setPosition(new LatLng(myLat, myLng));
                    }
                }
                updaterHandler.postDelayed(this, 100);  // post self to recursive run
            }
        };
        messageUpdater = new Runnable() {
            int icons[] = {R.drawable.ic_add_location_black_24dp, R.drawable.ic_traffic_black_48dp, R.drawable.ic_warning_black_48dp};
            LightEvent oldLightEvent;
            RoadStateEvent oldStateEvent;
            boolean isCanceled = true;

            @Override
            public void run() {
                if (isListening) {
                    if (currentLightEvent == LightEvent.NOLIGHT
                            && currentRoadStateEvent == RoadStateEvent.NOROADSTATE
                            && currentV2VEvent == V2VEvent.NOV2V) {
                        imgTraffic.setVisibility(View.INVISIBLE);
                        imgRoad.setVisibility(View.INVISIBLE);
                        imgV2v.setVisibility(View.INVISIBLE);
                        imgVelocity.setVisibility(View.VISIBLE);
                        String speedMessage = getString(R.string.current_speed) + (int) currentSpeed * 3.6 + getString(R.string.kmh);
                        textTip.setText(speedMessage);
                    } else {
                        if (currentV2VEvent == V2VEvent.CANCEL) isCanceled = true;
                        if ((currentLightEvent != oldLightEvent
                                || currentRoadStateEvent != oldStateEvent
                                || isCanceled)
                                && !Objects.equals(currentMessage, "")) {
                            if (currentV2VEvent != V2VEvent.CANCEL) {
                                Log.i(TAG, "speak?: " + currentMessage);
                                textToSpeech.speak(currentMessage, TextToSpeech.QUEUE_FLUSH, null, null);
                                oldLightEvent = currentLightEvent;
                                oldStateEvent = currentRoadStateEvent;
                                isCanceled = false;
                            }
                        }
                        if (!Objects.equals(currentMessage, "") && currentV2VEvent != V2VEvent.CANCEL) {
                            textTip.setText(currentMessage);
                        }
                        imgVelocity.setVisibility(View.INVISIBLE);
                        if (currentLightEvent != LightEvent.NOLIGHT)
                            imgTraffic.setVisibility(View.VISIBLE);
                        else imgTraffic.setVisibility(View.INVISIBLE);
                        if (currentV2VEvent != V2VEvent.NOV2V)
                            imgV2v.setVisibility(View.VISIBLE);
                        else imgV2v.setVisibility(View.INVISIBLE);
                        if (currentRoadStateEvent != RoadStateEvent.NOROADSTATE)
                            imgRoad.setVisibility(View.VISIBLE);
                        else imgRoad.setVisibility(View.INVISIBLE);
                    }
                    Log.i(TAG, "tip text: " + textTip.getText());
                }
                updaterHandler.postDelayed(this, 100);
            }
        };
    }

    private void initMap() {
        mapView.showScaleControl(false);
        mapView.showZoomControls(false); //隐藏地图的放大/缩小按钮，以及控制大小的拖动轴。
        mapView.setLogoPosition(LogoPosition.logoPostionRightBottom);   // logo position; can't be removed
        MapStatusUpdate mapStatusUpdate;
        LatLng position = new LatLng(myLat, myLng);
        mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(position, 255);  //max scale
        baiduMap.setMapStatus(mapStatusUpdate);
        UiSettings uiSettings = baiduMap.getUiSettings();
        uiSettings.setCompassEnabled(true);     // show compass
        uiSettings.setOverlookingGesturesEnabled(false);    // unable overlooking gesture

        BitmapDescriptor bitmapDescriptor;
        bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_navigation_black_24dp);
        OverlayOptions options = new MarkerOptions().position(position).icon(bitmapDescriptor);
        markers[0] = (Marker) baiduMap.addOverlay(options);
        int resourceArray[] = {R.drawable.ic_bullseye_black_24dp, R.drawable.ic_traffic_black_24dp, R.drawable.ic_warning_black_24dp};
        for (int i = 1; i < MAX_SOURCE; ++i) {
            bitmapDescriptor = BitmapDescriptorFactory.fromResource(resourceArray[i - 1]);
            options = new MarkerOptions().position(position).icon(bitmapDescriptor); // 设置Overlay图标
            markers[i] = (Marker) (baiduMap.addOverlay(options)); // 将Marker添加到地图上。
            markers[i].setVisible(false);    //  先将Marker隐藏。在获得对应位置信息的时候再行显示。
        }
        markers[0].setVisible(true);
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
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(false);
        //可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps
        option.setLocationNotify(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setIsNeedLocationDescribe(false);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(false);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(true);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(true);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        locationClient.setLocOption(option);
        locationClient.start(); //start location service
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
//        localMapList = mOffline.getAllUpdateInfo();
//        ArrayList<MKOLSearchRecord> records=mOffline.searchCity("上海");
//        if (records == null || records.size() != 1) {
//            Log.i("nib", "cannot find sh offline");
//        }
//        int id = records.get(0).cityID;
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
