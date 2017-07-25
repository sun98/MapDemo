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
import android.view.Menu;
import android.view.MenuItem;
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
import com.baidu.mapapi.model.LatLng;
import com.suke.widget.SwitchButton;

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
    private SharedPreferences.Editor isSpecialEditor;
    private boolean IsMapStretched = false;
    private static final int MAX_SOURCE = 4;    //ͬʱ��ʾ�ĳ����������Ŀ��
    private int event;
    private boolean icon_change = true;
    private boolean getChange = false;
    private double lat = 31.03533, lng = 121.44262;

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

    //ʵ�ֵ�ͼ�����ͱ�־���µ��̡߳�
    private Marker[] markers; // �����ͼ�����б�־�����顣markers[0]��ָ������ű���λ�á�
    private LatLng prev_pt = null;
    private float prev_angle = 0;
    private Handler MapUpdater = new Handler();//�������̺߳����̵߳�Handler
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
                        lat = binder.getLatL();
                        lng = binder.getLngL();
//                        Log.i("nib", String.valueOf(lat) + "\t" + String.valueOf(lng));
                        event = binder.getEvent();
                        LatLng pt = new LatLng(lat, lng);

                        markers[0].setPosition(pt);
                        float angle = prev_angle;
                        if (prev_pt != null) {
                            if (pt.longitude - prev_pt.longitude != 0) {
                                angle = (float) AngleUtil.getAngle(prev_pt.longitude, prev_pt.latitude, pt.longitude, pt.latitude);
                                if (Math.abs(angle - prev_angle) < 15)
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
                            if (event == 1) {
                                lat = binder.getLatR();
                                lng = binder.getLngR();
                                newPt = new LatLng(lat + 0.00004, lng - 0.0004);
                            } else {
                                lat = binder.getLatStable();
                                lng = binder.getLngStable();
                                newPt = new LatLng(lat, lng);
                            }
                            Log.i("nib", "new lat:" + newPt.latitude + " new lng:" + newPt.longitude);
                            //markers[i].setIcon(NewIcon);
                            markers[event].setPosition(newPt);
                            markers[event].setVisible(true);
                        }
                    } catch (Exception e) {
                        Log.i("nib", e.toString());
                    }
                }
            } else {
                LatLng latLng = bdLocationListener.getLatLng();
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.setMapStatus(msu);
                markers[0].setPosition(latLng);
            }
            //��������Ϣ����ʱ������������µ�marker.
            MapUpdater.postDelayed(this, 100);

        }
    };

    private TextToSpeech mTextToSpeech = null;
    private Queue<String> messageQueue = null;
    private boolean MsgUsed = false;
    private Button pause_map = null;
    Runnable MsgUpdate = new Runnable() {//���¾�ʾ��Ϣ���г�����
        private int srcArr[] =
                {R.drawable.icon2, R.drawable.trafficlight, R.drawable.spdlimit};

        @Override
        public void run() {
            if (getChange) {
                sourceString.setText(binder.getSource());
                if (event > 0 && !MsgUsed) {
                    String warnMsg = binder.getMsg();
                    tipText.setText(warnMsg);
                    WarnImg.setImageResource(srcArr[event - 1]);
                    mTextToSpeech.speak(warnMsg, TextToSpeech.QUEUE_ADD, null);
                    //mTextToSpeech.playSilence(1000, TextToSpeech.QUEUE_ADD, null);
                    MsgUsed = true;
                } else {
                    if (event == 0) {
                        MsgUsed = false;
                        WarnImg.setImageResource(R.drawable.icon_car);
                        tipText.setText(R.string.hello_world);
                    }
                }
            }
            MapUpdater.postDelayed(this, 50);
        }
    };

    //������Ϣ�����߳̿��ص�Observable��.�����ر�ʱ,�߳̽�������.
    private class IsUpdateEnabled extends Observable {
        private boolean isOpen = true;

        public boolean getState() {
            return isOpen;
        }

        public void setState(boolean state) {
            if (this.isOpen != state) {
                this.isOpen = state;
                setChanged();//����һ���ڲ���־λע�����ݷ����˱仯
            }
            notifyObservers();//�ص�����󶨵�Observer��update()����
        }

    }

    //������Ϣ�����߳̿��ص�watcher�ࡣ
    private class Watcher implements Observer {
        public Watcher(Observable obj) {
            obj.addObserver(this); //Ϊ���۲�Ķ�����ӹ۲���
        }

        //���۲���״̬�ı�ʱ��ִ��update()����
        public void update(Observable obj, Object arg) {
            boolean flag = ((IsUpdateEnabled) obj).getState();
            if (!flag) { //ֹͣ�߳�
                MapUpdater.removeCallbacks(MapUpdate);
                MapUpdater.removeCallbacks(MsgUpdate);
            } else {   //�����߳�
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
        mMapView.showZoomControls(false);            //���ص�ͼ�ķŴ�/��С��ť���Լ����ƴ�С���϶��ᡣ
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(255);        //����ͼ�ŵ����
        BaiduMap mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapStatus(msu);
//        mBaiduMap.setOnMapClickListener(new myMapClickListener());
        LatLng pt = new LatLng(lat, lng);
        msu = MapStatusUpdateFactory.newLatLngZoom(pt, 175);
        mBaiduMap.setMapStatus(msu);        // ����ͼ�ŵ���󣬲������ĵ���Ϊpt.

        BitmapDescriptor bitmap;
        if (isSpecialCar) bitmap = BitmapDescriptorFactory.fromResource(R.drawable.arrow_red);
        else bitmap = BitmapDescriptorFactory.fromResource(R.drawable.arrow_icon);
        OverlayOptions opt = new MarkerOptions().position(pt).icon(bitmap);    // ����Overlay������ʾ ����ɫ��ͷ��
        markers[0] = (Marker) (mBaiduMap.addOverlay(opt)); // ��Marker��ӵ���ͼ�ϡ�
        int resourceArray[] = {R.drawable.icon2, R.drawable.trafficlight, R.drawable.spdlimit};
        for (int i = 1; i < MAX_SOURCE; ++i) {
            bitmap = BitmapDescriptorFactory.fromResource(resourceArray[i - 1]);
            opt = new MarkerOptions().position(pt).icon(bitmap);    // ����Overlay������ʾ ����ɫ��ͷ��
            markers[i] = (Marker) (mBaiduMap.addOverlay(opt)); // ��Marker��ӵ���ͼ�ϡ�
            markers[i].setVisible(false);    //  �Ƚ�Marker���ء��ڻ�ö�Ӧλ����Ϣ��ʱ��������ʾ��
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


//        声明LocationClient对象
        locationClient = new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(bdLocationListener);
        initLocation();
        locationClient.start();

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
                    //�����ʶ�����
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
//                mServiceIntent.putExtra(MESSAGE_IN, "begin");
                startService(mServiceIntent);
                Log.i("nib", "service started");
                boolean r = bindService(mServiceIntent, conn, Service.BIND_AUTO_CREATE);
                Log.i("nib", String.valueOf(r));
                getChange = true;
            }
        });
        isSpecial = getSharedPreferences("config", MODE_APPEND);
        isSpecialEditor = isSpecial.edit();
        switchSpecial = (SwitchButton) findViewById(R.id.switch_special);
        switchSpecial.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                isSpecialEditor.putBoolean("is_special", isChecked);
                isSpecialEditor.apply();
                initializeMap(mMapView, isChecked);
            }
        });

        /*
         * ��̬ע��receiver  
        IntentFilter filter = new IntentFilter(ACTION_RECV_MSG);  
        filter.addCategory(Intent.CATEGORY_DEFAULT);  
        receiver = new MessageReceiver();  
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);  
		*/
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
        } else
            icon_change = true;
        ((IsUpdateEnabled) mapEnabled).setState(!state);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuitem1:
                Toast.makeText(this, "Menu Item 1 selected", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menuitem2:
                Toast.makeText(this, "Menu item 2 selected", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            default:
                break;
        }
        return true;
    }
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

