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
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DecimalFormat;
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
import cn.nibius.mapv2.util.EnDecodeUtil;


public class MainActivity extends AppCompatActivity {
    private String TAG = "MainActivity";
    private Context context;
    private double myLat = 31.0278622712, myLng = 121.4218843711;
    private LatLng currentL, interL;
    private Vehicle myCar;
    private Map intersections;
    private MessagePackage messagePackage;
    private TextToSpeech textToSpeech;
    private MKOfflineMap mOffline;
    private int MAX_SOURCE = 2;
    private Marker[] markers = new Marker[MAX_SOURCE];
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
    private TextView velocityView, warningView, tipView;
    private ViewController vc;
    private boolean gotMessage = false;
    private DecimalFormat df;
    private int viewPos = 0, lastViewPos = 0;
    public static Lock lock = new ReentrantLock();

    private static String getIP(){
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

    private void speak(TextToSpeech textToSpeech, String text){
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
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
        velocityView = findViewById(R.id.velocity_text);
        warningView = findViewById(R.id.warning_text);
        tipView = findViewById(R.id.tip_text);
        warningView.setTextColor(0xffff0000);
        xCrossBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.xcross);
        tCrossBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.tcross);
        xCrossBitmap  = Bitmap.createScaledBitmap(xCrossBitmap, xCrossBitmap.getWidth()/2, xCrossBitmap.getHeight()/2, false);
        tCrossBitmap  = Bitmap.createScaledBitmap(tCrossBitmap, tCrossBitmap.getWidth()/2, tCrossBitmap.getHeight()/2, false);
        carBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_directions_car_black_48dp);
        paint = new Paint();

        TextView showInfo = findViewById(R.id.text_info);
        showInfo.setText("本机LAN IP: " + getIP());
        myCar = new Vehicle();
        df = new DecimalFormat("#####0.0");

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

        toggleUpdater = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isListening) {
                    binder.stopListen();
                    isListening = false;
                    view.setBackgroundResource(R.drawable.play_icon);
                    speak(textToSpeech, "监听已停止");
                } else {
                    binder.startListen();
                    isListening = true;
                    view.setBackgroundResource(R.drawable.pause_icon);
                    speak(textToSpeech, "监听已开始");
                }
            }
        };

        btnMap = findViewById(R.id.btn_map);
        btnMap.setOnClickListener(toggleUpdater);
        baiduMap = mapView.getMap();
        locationClient = new LocationClient(getApplicationContext());

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
                        gotMessage = true;

                        vc = new ViewController(myCar, intersections);
                        viewPos = vc.viewPos();
                        if (viewPos == 1){
                            currentL = new LatLng(myCar.currentLat, myCar.currentLng);
                            mapBitmap = xCrossBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            canvas = new Canvas(mapBitmap);
                            canvas.drawBitmap(carBitmap,(float) vc.getViewLeft()*canvas.getWidth(),
                                    (float) vc.getViewTop()*canvas.getHeight(), paint);
                            canvasView.setImageBitmap(mapBitmap);
                            if(canvasView.getVisibility() == View.GONE)
                                canvasView.setVisibility(View.VISIBLE);
                            if(mapView.getVisibility() == View.VISIBLE)
                                mapView.setVisibility(View.GONE);

                        } else if (viewPos == 2){
                            currentL = new LatLng(myCar.currentLat, myCar.currentLng);
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
                            currentL = EnDecodeUtil.coorConvert(currentL);
                            MapStatus mapStatus = new MapStatus.Builder(baiduMap.getMapStatus()).target(currentL).rotate((float)myCar.heading).build();
                            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
                            baiduMap.animateMapStatus(mapStatusUpdate);

                            markers[0].setPosition(currentL);
                            if (vc.viewPos() == 3){
                                Intersection nextInter = (Intersection) intersections.get(vc.nextIntersection());
                                interL = new LatLng(nextInter.centerLat, nextInter.centerLng);
                                interL = EnDecodeUtil.coorConvert(interL);
                                markers[1].setPosition(interL);
                                markers[1].setVisible(true);
                            }
                            else
                                markers[1].setVisible(false);
                        }
                    } catch (Exception e) {
                        Log.i(TAG, "MapUpdater: " + e.toString());
                    }

                } else {
                    if(canvasView.getVisibility() == View.VISIBLE)
                        canvasView.setVisibility(View.GONE);
                    if(mapView.getVisibility() == View.GONE)
                        mapView.setVisibility(View.VISIBLE);
                    currentL = myLocationListener.getLatLng();
                    MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(currentL);
                    baiduMap.animateMapStatus(mapStatusUpdate);
                    if (currentL != null)
                        markers[0].setPosition(currentL);
                    else
                        markers[0].setPosition(new LatLng(myLat, myLng));
                    markers[1].setVisible(false);
                }
                Log.i(TAG, "Locate: "+String.valueOf(currentL));
                updaterHandler.postDelayed(this, 100);
            }
        };

        messageUpdater = new Runnable() {
            @Override
            public void run() {
                String velocity;
                if (isListening && gotMessage) {
                    imgVelocity.setVisibility(View.INVISIBLE);
                    imgTraffic.setVisibility(View.INVISIBLE);
                    imgRoad.setVisibility(View.INVISIBLE);
                    imgV2v.setVisibility(View.INVISIBLE);

                    try {
                        switch (myCar.safetyMessage) {
                            case 1: // forward crash
                                imgRoad.setVisibility(View.VISIBLE);
                                imgV2v.setVisibility(View.VISIBLE);
                                warningView.setText(getString(R.string.forward_crash_message));
                                speak(textToSpeech, getString(R.string.forward_crash_message));
                                break;
                            case 2: // emergency break
                                imgRoad.setVisibility(View.VISIBLE);
                                imgV2v.setVisibility(View.VISIBLE);
                                warningView.setText(getString(R.string.emergency_brake_message));
                                speak(textToSpeech, getString(R.string.emergency_brake_message));
                                break;
                            case 3: // right side crash
                                imgRoad.setVisibility(View.VISIBLE);
                                imgV2v.setVisibility(View.VISIBLE);
                                warningView.setText(getString(R.string.right_cross_crash_message));
                                speak(textToSpeech, getString(R.string.right_cross_crash_message));
                                break;
                            case 4: // left side crash
                                imgRoad.setVisibility(View.VISIBLE);
                                imgV2v.setVisibility(View.VISIBLE);
                                warningView.setText(getString(R.string.left_cross_crash_message));
                                speak(textToSpeech, getString(R.string.left_cross_crash_message));
                                break;
                            case 0:
                                warningView.setText("");
                                break;
                            default:
                                break;
                        }

                        vc = new ViewController(myCar, intersections);
                        velocity = df.format(myCar.speed);
                        velocityView.setText(getString(R.string.current_speed) + velocity + getString(R.string.m_per_s));

                        viewPos = vc.viewPos();
                        if (lastViewPos == 0 && viewPos > 0)
                            speak(textToSpeech, "您已进入路口");
                        else if (lastViewPos > 0 && viewPos == 0)
                            speak(textToSpeech, "您已离开路口");
                        lastViewPos = viewPos;

                        if (vc.needRemind())
                            speak(textToSpeech, "前方通过隧道");

                        if (viewPos != 0) {
                            imgTraffic.setVisibility(View.VISIBLE);
                            String nextInterID = vc.nextIntersection();
                            Intersection nextInter = (Intersection) intersections.get(nextInterID);
                            String currentLane = vc.currentLane(nextInterID);
                            int currentState = (int) nextInter.currentState.get(currentLane);
                            int timeToChange = (int) nextInter.timeToChange.get(currentLane);
                            tipView.setText(getString(R.string.light_ahead)
                                    + EnDecodeUtil.lightColor(currentState)
                                    + getString(R.string.time_left) + String.valueOf(timeToChange)
                                    + getString(R.string.second));
                        } else {
                            imgTraffic.setVisibility(View.INVISIBLE);
                            tipView.setText(getString(R.string.welcome));
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

        bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_black_48dp);
        options = new MarkerOptions().position(position).icon(bitmapDescriptor);
        markers[1] = (Marker) (baiduMap.addOverlay(options));
        markers[1].setVisible(false);

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
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }
}
