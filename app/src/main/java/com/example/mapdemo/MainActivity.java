package com.example.mapdemo;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;
import com.suke.widget.SwitchButton;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;

public class MainActivity extends Activity {
    /*
    private CheckBox carObstacle = null;
    private CheckBox roadWork = null;
    private CheckBox spdLimit = null;
    private CheckBox bestTravel = null;
    */
    private static final String ACTION_RECV_MSG = "com.example.mapdemo.action.RECEIVE_MESSAGE";
    private static final String MESSAGE_OUT = "message_output";
    private static final String MESSAGE_IN = "message_input";

    //MessageReceiver receiver;
    private LocationClient locationClient = null;       // 定位服务的核心类
    private MyLocationListener bdLocationListener = new MyLocationListener();   // 定位服务接口

    private TextView tipText;
    private TextView sourceString;
    private ImageView WarnImg;
    private MapView mMapView = null;
    private SwitchButton switchSpecial;
    private SharedPreferences isSpecial;
    private MKOfflineMap mOffline;
    private ArrayList<MKOLUpdateElement> localMapList;
    private SharedPreferences.Editor isSpecialEditor;
    private boolean IsMapStretched = false;
    private static final int MAX_SOURCE = 4;
    private int event;
    private String warnMessage;
    private boolean icon_change = true;
    private boolean getChange = false;
    private double lat = 31.0289793247, lng = 121.4250639544;

    ComuService.MyBinder binder;                        //通信服务的binder
    private ServiceConnection conn = new ServiceConnection() {                  // 用于绑定服务的连接类
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("nib", "Service connected");
            binder = (ComuService.MyBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("nib", "Service disconnected");
            binder = null;
        }

    };

    //实现地图卷屏和标志更新的线程。
    private Marker[] markers; // 储存地图上所有标志的数组。markers[0]被指定来存放本车位置。
    private LatLng prev_pt = null;
    private float prev_angle = 0;
    private Handler MapUpdater = new Handler();//调控主线程和子线程的Handler
    private Runnable MapUpdate = new Runnable() {//���µ�ͼ��Ϣ���߳�
        public void run() {
            BaiduMap mBaiduMap = mMapView.getMap();
            // 如果服务已绑定 {@code binder!=null}，显示接收到的数据，否则，显示定位结果
            if (binder != null) {
                if (!getChange) {
                    LatLng pt = new LatLng(lat, lng);
                    MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(pt);
                    mBaiduMap.setMapStatus(msu);
                    markers[0].setPosition(pt);
                } else {
                    try {
                        lat = binder.getLatL() + 0.0043953298;
                        lng = binder.getLngL() + 0.0110212588;
//                        Log.i("nib", String.valueOf(lat) + "\t" + String.valueOf(lng));
                        prevEvent = event;
                        event = binder.getEvent();
                        warnMessage = binder.getMsg();
                        LatLng pt = new LatLng(lat, lng);

                        markers[0].setPosition(pt);
                        float angle = prev_angle;
                        if (prev_pt != null) {
                            if (pt.longitude - prev_pt.longitude != 0) {
                                angle = (float) AngleUtil.getAngle(prev_pt.longitude, prev_pt.latitude, pt.longitude, pt.latitude);
                                float d = Math.abs(angle - prev_angle);
                                d = (d > 180) ? (360 - d) : d;
                                if (d < 15)
                                    angle = prev_angle;
                            }
                            if (prev_angle != angle)
//                            Log.i("angle", angle + "   " + prev_angle);
                                prev_angle = angle;
                            //angle = (float) Math.toDegrees(Math.atan((pt.latitude-prev_pt.latitude)/(pt.longitude-prev_pt.longitude)));
//                        Log.i("angle", angle + "");
                            MapStatus mMapStatus = new MapStatus.Builder().target(pt).rotate(angle).build();

                            MapStatusUpdate msu = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                            //mBaiduMap.setMapStatus(msu);
                            mBaiduMap.animateMapStatus(msu);
                        }
                        prev_pt = new LatLng(lat, lng);

                        markers[1].setVisible(false);
                        if (event > 0) {
//                            Log.i("nib", "msg:" + binder.getMsg());
                            LatLng newPt;
                            if (event == 5) {
                                newPt = new LatLng(binder.getLatR() + 0.0043953298,
                                        binder.getLngR() + 0.0110212588);
                                markers[3].setPosition(newPt);
                                markers[3].setVisible(true);
                            } else if (event >= 2) {
                                newPt = new LatLng(binder.getLatStable() + 0.0043953298,
                                        binder.getLngStable() + 0.0110212588);
                                markers[2].setPosition(newPt);
                                markers[2].setVisible(true);
                            }

//                            if (event == 1) {
//                                lat = binder.getLatR() + 0.0043953298;
//                                lng = binder.getLngR() + 0.0110212588;
//                                newPt = new LatLng(lat + 0.00004, lng - 0.0004);
//                            } else {
//                                lat = binder.getLatStable() + 0.0043953298;
//                                lng = binder.getLngStable() + 0.0110212588;
//                                newPt = new LatLng(lat, lng);
//                            }
////                            Log.i("nib", "new lat:" + newPt.latitude + " new lng:" + newPt.longitude);
//                            //markers[i].setIcon(NewIcon);
//                            if (event == 5) {
//                                markers[3].setPosition(newPt);
//                                markers[3].setVisible(true);
//                            } else if (event > 2) {
//                                markers[2].setPosition(newPt);
//                                markers[2].setVisible(true);
//                            } else {
//                                markers[event].setPosition(newPt);
//                                markers[event].setVisible(true);
//                            }
                        } else {
                            LatLng newPt = new LatLng(binder.getLatL() + 0.0043953298,
                                    binder.getLngL() + 0.0110212588);
                            markers[0].setPosition(newPt);
                            markers[0].setVisible(true);
                        }
                    } catch (Exception e) {
                        Log.i("nib", e.toString());
                    }
                }
            } else {
                LatLng latLng = bdLocationListener.getLatLng();
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.setMapStatus(msu);
                if (latLng != null)
                    markers[0].setPosition(latLng);
            }
            MapUpdater.postDelayed(this, 50);
        }
    };

    private TextToSpeech mTextToSpeech = null;
    private Queue<String> messageQueue = null;
    private int prevEvent = 0;
    private int event_ = 0, prevEvent_ = 0;
    private Button pause_map = null;
    Runnable MsgUpdate = new Runnable() {//更新地图信息的线程
        private int srcArr[] =
                {R.drawable.icon2, R.drawable.trafficlight, R.drawable.spdlimit};

        @Override
        public void run() {
            if (getChange) {
//                sourceString.setText(binder.getSource());
//                Log.i("nib", "run: event=" + event + "\tprev=" + prevEvent);
                prevEvent_ = event_;
                event_ = binder.getEvent();
                if (event_ > 0) {
                    String warnMsg = binder.getMsg();
                    if (event_ != prevEvent_)
//                        mTextToSpeech.speak(warnMessage, TextToSpeech.QUEUE_ADD, null);
                        mTextToSpeech.speak(warnMsg, TextToSpeech.QUEUE_ADD, null);
//                    tipText.setText(warnMessage);
                    tipText.setText(warnMsg);
                    if (event_ == 5) WarnImg.setImageResource(srcArr[2]);
                    else if (event_ > 2) WarnImg.setImageResource(srcArr[1]);
                    else WarnImg.setImageResource(srcArr[event_ - 1]);
                    //mTextToSpeech.playSilence(1000, TextToSpeech.QUEUE_ADD, null);
                } else if (event_ == 0) {
                    WarnImg.setImageResource(R.drawable.icon_car);
                    tipText.setText(R.string.hello_world);
                }
            }
            MapUpdater.postDelayed(this, 50);
        }
    };

    //控制消息更新线程开关的Observable类.卷屏关闭时,线程将不运行.
    private class IsUpdateEnabled extends Observable {
        private boolean isOpen = true;

        public boolean getState() {
            return isOpen;
        }

        public void setState(boolean state) {
            if (this.isOpen != state) {
                this.isOpen = state;
                setChanged();//设置一个内部标志位注明数据发生了变化
            }
            notifyObservers();//回调和其绑定的Observer的update()方法
        }

    }

    //监听消息更新线程开关的watcher类。
    private class Watcher implements Observer {
        public Watcher(Observable obj) {
            obj.addObserver(this);//为被观察的对象添加观察者
        }

        //被观察者状态改变时，执行update()方法
        public void update(Observable obj, Object arg) {
            boolean flag = ((IsUpdateEnabled) obj).getState();
            if (!flag) { //停止线程
                MapUpdater.removeCallbacks(MapUpdate);
                MapUpdater.removeCallbacks(MsgUpdate);
            } else {   //启动线程
                MapUpdater.post(MapUpdate);
                MapUpdater.post(MsgUpdate);
            }
        }
    }

    Observable mapEnabled;
    Observer myWatcher;

    private class myMapClickListener implements OnMapClickListener {
        //������ͼ����õ�listener��
        LayoutParams original;

        @Override
        //�����ͼ�ϵ���һ��γ��ʱ���õķ���
        public void onMapClick(LatLng arg0) {
            if (!IsMapStretched) {
                original = (LayoutParams) mMapView.getLayoutParams();        //��ԭʼ��λ�ò������棬�Ա�֮��ص����״̬��
                mMapView.setLayoutParams(new LayoutParams(320, 250));
//                mMapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                //ֱ������Layout������
                IsMapStretched = true;
            } else {
                mMapView.setLayoutParams(original);
                IsMapStretched = false;
            }
        }

        @Override
        public boolean onMapPoiClick(MapPoi arg0) {
            return false;
        }

    }

    protected void initializeMap(MapView mMapView, boolean isSpecialCar) {

        mMapView.showScaleControl(false);
        mMapView.showZoomControls(false); //隐藏地图的放大/缩小按钮，以及控制大小的拖动轴。
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(255);        //将地图放到最大
        BaiduMap mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapStatus(msu);
//        mBaiduMap.setOnMapClickListener(new myMapClickListener());
        LatLng pt = new LatLng(lat, lng);
        msu = MapStatusUpdateFactory.newLatLngZoom(pt, 175);
        mBaiduMap.setMapStatus(msu);        // 将地图放到最大，并且中心点设为pt.

        BitmapDescriptor bitmap;
        bitmap = BitmapDescriptorFactory.fromResource(R.drawable.arrow_icon);
        OverlayOptions opt = new MarkerOptions().position(pt).icon(bitmap);    // 设置Overlay对象演示 （红色箭头）
        markers[0] = (Marker) (mBaiduMap.addOverlay(opt));  // 将Marker添加到地图上。
        int resourceArray[] = {R.drawable.icon2, R.drawable.trafficlight, R.drawable.spdlimit};
        for (int i = 1; i < MAX_SOURCE; ++i) {
            bitmap = BitmapDescriptorFactory.fromResource(resourceArray[i - 1]);
            opt = new MarkerOptions().position(pt).icon(bitmap);    // 设置Overlay对象演示 （红色箭头）
            markers[i] = (Marker) (mBaiduMap.addOverlay(opt)); // 将Marker添加到地图上。
            markers[i].setVisible(false);    //  先将Marker隐藏。在获得对应位置信息的时候再行显示。
        }

        markers[0].setVisible(true);
        MapUpdater.postDelayed(MapUpdate, 3000);
        MapUpdater.postDelayed(MsgUpdate, 3000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        carObstacle = (CheckBox)this.findViewById(R.id.checkBox1);
		roadWork = (CheckBox)this.findViewById(R.id.checkBox2);
		spdLimit = (CheckBox)this.findViewById(R.id.checkBox3);
		bestTravel = (CheckBox)this.findViewById(R.id.checkBox4);
		*/
        SDKInitializer.initialize(getApplicationContext());
        mapEnabled = new IsUpdateEnabled();
        myWatcher = new Watcher(mapEnabled);
        setContentView(R.layout.activity_main);
        mMapView = (MapView) findViewById(R.id.bmapView);

        markers = new Marker[MAX_SOURCE];
        initializeMap(mMapView, false);

        initOfflineMap();
//        mOffline.start(289);
        localMapList = mOffline.getAllUpdateInfo();
        ArrayList<MKOLSearchRecord> records = mOffline.searchCity("上海");
        if (records == null || records.size() != 1) {
            Log.i("nib", "cannot find sh offline");
        }
        int id = records.get(0).cityID;
        mOffline.start(id);

//        声明LocationClient对象
        locationClient = new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(bdLocationListener);
//        initLocation();
//        locationClient.start();

        Button mStartBtn = (Button) findViewById(R.id.begin);
        pause_map = (Button) findViewById(R.id.mapEnable);
        tipText = (TextView) findViewById(R.id.show);
        sourceString = (TextView) findViewById(R.id.text_source_string);
        WarnImg = (ImageView) findViewById(R.id.WarningImage);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle("SpeedGuiding");
        messageQueue = new LinkedList<String>();
        mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int supported = mTextToSpeech.setLanguage(Locale.CHINESE);
                    if ((supported != TextToSpeech.LANG_AVAILABLE) && (supported !=
                            TextToSpeech.LANG_COUNTRY_AVAILABLE)) {
                        displayToast("Cannot support the language");
                    }
                }
            }
        });
        final Intent mServiceIntent = new Intent(MainActivity.this, ComuService.class);
        mStartBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mServiceIntent.putExtra(MESSAGE_IN, "begin");
                startService(mServiceIntent);
                Log.i("nib", "service started");
                boolean r = bindService(mServiceIntent, conn, Service.BIND_AUTO_CREATE);
                Log.i("nib", String.valueOf(r));
                getChange = true;
            }
        });
//        isSpecial = getSharedPreferences("config", MODE_APPEND);
//        isSpecialEditor = isSpecial.edit();
        switchSpecial = (SwitchButton) findViewById(R.id.switch_special);
        switchSpecial.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
//                isSpecialEditor.putBoolean("is_special", isChecked);
//                isSpecialEditor.apply();
//                initializeMap(mMapView, isChecked);
                BitmapDescriptor bitmap;
                if (isChecked) bitmap = BitmapDescriptorFactory.fromResource(R.drawable.arrow_red);
                else bitmap = BitmapDescriptorFactory.fromResource(R.drawable.arrow_icon);
                markers[0].setVisible(false);
                markers[0] = (Marker) mMapView.getMap().addOverlay(new MarkerOptions().position(new LatLng(lat, lng)).icon(bitmap));
                markers[0].setVisible(true);
            }
        });

//        try {
//            XLog.init(
//                    new LogConfiguration.Builder().logLevel(LogLevel.INFO).build(),
//                    new AndroidPrinter(),
//                    new FilePrinter.Builder(Environment.getExternalStorageDirectory().getPath() + "/xlog").fileNameGenerator(new DateFileNameGenerator()).build()
//            );
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//        }


        /*
         * ��̬ע��receiver  
        IntentFilter filter = new IntentFilter(ACTION_RECV_MSG);  
        filter.addCategory(Intent.CATEGORY_DEFAULT);  
        receiver = new MessageReceiver();  
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);  
		*/
    }

    private void initOfflineMap() {
        Log.i("nib", "initOfflineMap: ");
        mOffline = new MKOfflineMap();
        // 设置监听
        mOffline.init(new MKOfflineMapListener() {
            @Override
            public void onGetOfflineMapState(int type, int state) {
                switch (type) {
                    case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
                        // 离线地图下载更新事件类型
                        MKOLUpdateElement update = mOffline.getUpdateInfo(state);
                        Log.e("offlineMap", update.cityName + " ," + update.ratio);
                        Log.e("a", "TYPE_DOWNLOAD_UPDATE");
                        break;
                    case MKOfflineMap.TYPE_NEW_OFFLINE:
                        // 有新离线地图安装
                        Log.e("b", "TYPE_NEW_OFFLINE");
                        break;
                    case MKOfflineMap.TYPE_VER_UPDATE:
                        // 版本更新提示
                        break;
                }

            }
        });
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps
        option.setLocationNotify(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        locationClient.setLocOption(option);
    }

    private void displayToast(String s) {
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }

    public void MapOnOff(View view) {
        boolean state = ((IsUpdateEnabled) mapEnabled).getState();
        if (icon_change) {
            pause_map.setBackgroundResource(R.drawable.play_icon);
            icon_change = false;
        } else {
            icon_change = true;
            pause_map.setBackgroundResource(R.drawable.pause_icon);
        }
        ((IsUpdateEnabled) mapEnabled).setState(!state);

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menuitem1:
//                Toast.makeText(this, "Menu Item 1 selected", Toast.LENGTH_SHORT).show();
//                break;
//            case R.id.menuitem2:
//                Toast.makeText(this, "Menu item 2 selected", Toast.LENGTH_SHORT).show();
//                break;
//            case R.id.action_settings:
//                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
//                break;
//            default:
//                break;
//        }
//        return true;
//    }
    /*
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}*/

    public void onDestroy() {
        super.onDestroy();
        if (((IsUpdateEnabled) mapEnabled).getState()) {
            MapUpdater.removeCallbacks(MapUpdate);
            MapUpdater.removeCallbacks(MsgUpdate);
        }
        unbindService(conn);
        stopService(new Intent(MainActivity.this, ComuService.class));
        mMapView.onDestroy();
        if (mTextToSpeech != null)
            mTextToSpeech.shutdown();//shutdown TTS
        messageQueue = null;
    }

    public void onResume() {
        super.onResume();
        if (!((IsUpdateEnabled) mapEnabled).getState()) {
            MapUpdater.post(MapUpdate);
            MapUpdater.post(MsgUpdate);
        }
        mMapView.onResume();
    }

    public void onPause() {
        super.onPause();
        if (((IsUpdateEnabled) mapEnabled).getState()) {
            MapUpdater.removeCallbacks(MapUpdate);
            MapUpdater.removeCallbacks(MsgUpdate);
        }
        mMapView.onPause();
    }
}

