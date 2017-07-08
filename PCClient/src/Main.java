import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        final int[] c = {0};
        DatagramSocket datagramSocket = new DatagramSocket();
        final DatagramPacket[] datagramPacket = {null};
        InetAddress inetAddress = InetAddress.getByName("192.168.0.151");
        File file = new File("src/simu_hex.txt");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
//                String currentTimeMills = String.valueOf(c[0]++);
                String message = null;
                try {
                    message = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                String currentTimeMills = String.valueOf(System.currentTimeMillis());
                byte[] bytes = message.getBytes();
                datagramPacket[0] = new DatagramPacket(bytes, bytes.length, inetAddress, 8888);
                try {
                    datagramSocket.send(datagramPacket[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(message);
            }
        };
        timer.schedule(task, 0, 100);
    }
}
