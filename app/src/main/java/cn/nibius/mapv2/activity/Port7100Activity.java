package cn.nibius.mapv2.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import cn.nibius.mapv2.R;

import static cn.nibius.mapv2.util.EnDecodeUtil.bytesToHexString;
import static cn.nibius.mapv2.util.EnDecodeUtil.removeTail0;

public class Port7100Activity extends AppCompatActivity {
    private Button btnStart, btnStop;
    private TextView text7100;
    private Thread thread7100;
    private Runnable runnable7100;
    private DatagramSocket socket;
    private Boolean stop = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_port7100);
        runnable7100 = new Runnable() {
            String message7100;

            @Override
            public void run() {
                try {
                    socket = new DatagramSocket(7100);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                while (!stop) {
                    DatagramPacket packet = new DatagramPacket(new byte[512], 512);
                    try {
                        socket.receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    Log.i("nib", "run: received");
                    byte[] data7100 = packet.getData();
                    if (MainActivity.test) message7100 = new String(data7100, 0, data7100.length);
                    else {
                        message7100 = bytesToHexString(data7100);
                    }
                    message7100 = removeTail0(message7100);
                    // TODO: many ?? at end of message7100
                    Log.i("nib", "run: " + message7100);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            text7100.setText(message7100);
                        }
                    });
                }
            }
        };
        thread7100 = new Thread(runnable7100);
        btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stop) {
                    stop = false;
                    thread7100.start();
                }
            }
        });
        btnStop = findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop = true;
            }
        });
        text7100 = findViewById(R.id.text7100);
        text7100.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

}
