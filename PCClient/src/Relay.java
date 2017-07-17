import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        int listenPort = 9999;
        DatagramSocket responseSocket = new DatagramSocket(listenPort);
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        DatagramSocket datagramSocket = new DatagramSocket();
        final DatagramPacket datagramPacket = null;
        InetAddress inetAddress = InetAddress.getByName("192.168.0.151");
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                responseSocket.receive(packet);
//                String currentTimeMills = String.valueOf(System.currentTimeMillis());
                byte[] bytes = packet.getData();
                datagramPacket = new DatagramPacket(bytes, bytes.length, inetAddress, 8888);
                try {
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(message);
            }
        };
        timer.schedule(task, 0, 100);
    }
}