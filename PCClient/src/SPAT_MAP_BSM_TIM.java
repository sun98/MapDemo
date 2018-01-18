import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Objects;

/**
 * Created by Nibius on 2017/7/27.
 */
public class SPAT_MAP_BSM_TIM {
    public static void main(String[] args) throws IOException, InterruptedException {
        File f = new File("src/1514258737023.txt");
        DatagramSocket socket7 = new DatagramSocket(8887),
                socket8 = new DatagramSocket(8888),
                socket9 = new DatagramSocket(8889),
                socket0 = new DatagramSocket(8890);
        InetAddress inetAddress = InetAddress.getByName("192.168.0.193");
        String line = "";
        while (true) {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            while ((line = reader.readLine()) != null) {
                switch (line.substring(0, 4)) {
                    case "SPAT": {
                        String info = line.substring(5);
                        if (!Objects.equals(info, "null")) {
                            DatagramPacket packet = new DatagramPacket(info.getBytes(), info.getBytes().length, inetAddress, 8887);
                            socket7.send(packet);
                        }
                        System.out.println("spat\t" + info);
                        break;
                    }
                    case "MAP:": {
                        String info = line.substring(5);
                        if (!Objects.equals(info, "null")) {
                            DatagramPacket packet = new DatagramPacket(info.getBytes(), info.getBytes().length, inetAddress, 8888);
                            socket8.send(packet);
                        }
                        System.out.println("map\t\t" + info);
                        break;
                    }
                    case "BSM:": {
                        String info = line.substring(5);
                        if (!Objects.equals(info, "null")) {
                            DatagramPacket packet = new DatagramPacket(info.getBytes(), info.getBytes().length, inetAddress, 8889);
                            socket9.send(packet);
                        }
                        System.out.println("bsm\t\t" + info);
                        break;
                    }
                    case "TIM:": {
                        String info = line.substring(5);
                        if (!Objects.equals(info, "null")) {
                            DatagramPacket packet = new DatagramPacket(info.getBytes(), info.getBytes().length, inetAddress, 8890);
                            socket0.send(packet);
                        }
                        System.out.println("tim\t\t" + info);
                        break;
                    }
                    default:
                        break;
                }
                Thread.sleep(50);
            }
        }
    }
}
