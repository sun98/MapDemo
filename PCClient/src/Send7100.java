import java.io.*;
import java.net.*;

public class Send7100 {
    public static void main(String[] args) throws IOException, InterruptedException {
        DatagramSocket socket = new DatagramSocket(7100);
        InetAddress address = InetAddress.getByName("192.168.0.193");
        String line = "";
        File f = new File("src/usb0.txt");
        while (true) {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            while ((line = reader.readLine()) != null) {
                DatagramPacket packet = new DatagramPacket(line.getBytes(), line.getBytes().length, address, 7100);
                socket.send(packet);
                System.out.println("sending\t" + line);
                Thread.sleep(500);
            }
        }
    }
}
