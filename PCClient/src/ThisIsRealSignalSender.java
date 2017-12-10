import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Objects;

/**
 * Created by Nibius on 2017/7/27.
 */
public class ThisIsRealSignalSender {
    public static void main(String[] args) throws IOException, InterruptedException {
        File f = new File("src/logcat.txt");
        BufferedReader reader = new BufferedReader(new FileReader(f));
        DatagramSocket socket7 = new DatagramSocket(8887),
                socket8 = new DatagramSocket(8888),
                socket9 = new DatagramSocket(8889),
                socket0 = new DatagramSocket(8890);
        InetAddress inetAddress = InetAddress.getByName("192.168.137.25");
        String line = "";
        while ((line = reader.readLine()) != null) {
//            22,25
            switch (line.substring(33,36)) {
                case "spa": {
                    String info = line.substring(39);
                    if (!Objects.equals(info, "null")) {
                        DatagramPacket packet = new DatagramPacket(info.getBytes(), info.getBytes().length, inetAddress, 8887);
                        socket7.send(packet);
                    }
                    System.out.println("spat\t" + info);
                    break;
                }
                case "map": {
                    String info = line.substring(38);
                    if (!Objects.equals(info, "null")) {
                        DatagramPacket packet = new DatagramPacket(info.getBytes(), info.getBytes().length, inetAddress, 8888);
                        socket8.send(packet);
                    }
                    System.out.println("map\t\t" + info);
                    break;
                }
                case "bsm": {
//                    String info = "69419260c26270127e4e1c485f972d009e0d0b4000e12269bd7f000f07d181ff1a00002d014a";
                    String info = line.substring(38);
                    if (!Objects.equals(info, "null")) {
                        DatagramPacket packet = new DatagramPacket(info.getBytes(), info.getBytes().length, inetAddress, 8889);
                        socket9.send(packet);
                    }
                    System.out.println("bsm\t\t" + info);
                    break;
                }
                case "tim": {
                    String info = line.substring(38);
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
