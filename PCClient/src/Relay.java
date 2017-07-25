import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class Relay {

    public static void main(String[] args) throws IOException, InterruptedException {
        int listenPort = 8888;
        DatagramSocket responseSocket = new DatagramSocket(listenPort);
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        DatagramSocket datagramSocket = new DatagramSocket();
        final DatagramPacket[] datagramPacket = {null};
        InetAddress inetAddress = InetAddress.getByName("192.168.42.129");
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    responseSocket.receive(packet);
                    String rcvd = "Received from " + packet.getSocketAddress() + ", Data="
                            + new String(packet.getData(), 0, packet.getLength());
                    System.out.println(rcvd);
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                String currentTimeMills = String.valueOf(System.currentTimeMillis());
                byte[] bytes = packet.getData();
                datagramPacket[0] = new DatagramPacket(bytes, bytes.length, inetAddress, 8888);
                try {
                    datagramSocket.send(datagramPacket[0]);
                    System.out.println("Send " + new String(packet.getData(), 0, packet.getLength()) + " to " + inetAddress + ":8888");
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                System.out.println(message);
            }
        };
        timer.schedule(task, 0, 100);
    }
}