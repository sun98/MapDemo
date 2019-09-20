import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class Send {
    public static void main(String[] args) throws IOException, InterruptedException {

        DatagramSocket datagramSocket = new DatagramSocket();
        final DatagramPacket[] datagramPacket = {null,null,null};

        File ip = new File("ip.txt");
        BufferedReader ip_reader = new BufferedReader(new FileReader(ip));
        String ip_text = ip_reader.readLine();
        InetAddress inetAddress = InetAddress.getByName(ip_text); //Test Phone LAN
        
        File file[] = {new File("broad0.txt"),new File("broad1.txt"),new File("broad2.txt")};
        BufferedReader reader[] = {new BufferedReader(new FileReader(file[0])),new BufferedReader(new FileReader(file[1])),
            new BufferedReader(new FileReader(file[2]))};

        String msg0 = reader[0].readLine();
        String msg1 = reader[1].readLine();
        String msg2 = reader[2].readLine();

        Timer timer[] = {new Timer(),new Timer(),new Timer()};
        TimerTask task[] = new TimerTask[3];

        task[0] = new TimerTask() {  // SPAT
            @Override
            public void run() {

                String message = null;
                /*
                try {
                    message = reader[0].readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
                message = msg0;

                byte[] bytes = message.getBytes();
                datagramPacket[0] = new DatagramPacket(bytes, bytes.length, inetAddress, 8887);
                try {
                    datagramSocket.send(datagramPacket[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("-> "+ip_text+": SPAT");
            }
        };

        task[1] = new TimerTask() {  // MAP
            @Override
            public void run() {

                String message = null;
                /*
                try {
                    message = reader[1].readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
                message = msg1;

                byte[] bytes = message.getBytes();
                datagramPacket[1] = new DatagramPacket(bytes, bytes.length, inetAddress, 8888);
                try {
                    datagramSocket.send(datagramPacket[1]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("-> "+ip_text+": MAP");
            }
        };

        task[2] = new TimerTask() {  // BSM
            @Override
            public void run() {

                String message = null;
                /*
                try {
                    message = reader[2].readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
                message = msg2;

                byte[] bytes = message.getBytes();
                datagramPacket[2] = new DatagramPacket(bytes, bytes.length, inetAddress, 8889);
                try {
                    datagramSocket.send(datagramPacket[2]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("-> "+ip_text+": BSM");
            }
        };

        timer[0].schedule(task[0], 0, 100);
        timer[1].schedule(task[1], 0, 100);
        timer[2].schedule(task[2], 0, 100);
    }
}