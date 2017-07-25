import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class SendUDP {
    public static void main(String[] args) throws Exception {

        String address = "192.168.137.223";
        DatagramSocket socket0 = new DatagramSocket(8887);
        DatagramSocket socket1 = new DatagramSocket(8888);
        DatagramSocket socket2 = new DatagramSocket(8889);

        Thread thread0 = new Thread(() -> {
            System.out.println("port0 send start.");
            while (true) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(address);
                    String outMessage = "000" + String.valueOf(System.currentTimeMillis());
                    DatagramPacket packet0 = new DatagramPacket(outMessage.getBytes(), outMessage.getBytes().length, inetAddress, 8887);
                    socket0.send(packet0);
                    System.out.println("send " + outMessage + " to " + inetAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread thread1 = new Thread(() -> {
            System.out.println("port0 send start.");
            while (true) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(address);
                    String outMessage = "111" + String.valueOf(System.currentTimeMillis());
                    DatagramPacket packet0 = new DatagramPacket(outMessage.getBytes(), outMessage.getBytes().length, inetAddress, 8888);
                    socket1.send(packet0);
                    System.out.println("send " + outMessage + " to " + inetAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread thread2 = new Thread(() -> {
            System.out.println("port0 send start.");
            while (true) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(address);
                    String outMessage = "222" + String.valueOf(System.currentTimeMillis());
                    DatagramPacket packet0 = new DatagramPacket(outMessage.getBytes(), outMessage.getBytes().length, inetAddress, 8889);
                    socket2.send(packet0);
                    System.out.println("send " + outMessage + " to " + inetAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread0.start();
        thread1.start();
        thread2.start();

        // Use this port to send broadcast packet
//        @SuppressWarnings("resource") final DatagramSocket detectSocket = new DatagramSocket(8888);
//        // Send packet thread
//        new Thread(() -> {
//            System.out.println("Send thread started.");
//            while (true) {
//                try {
//                    byte[] buf = new byte[1024];
//                    int packetPort = 8888;
//                    // Broadcast address
//                    InetAddress hostAddress = InetAddress.getByName("192.168.137.64");
//                    BufferedReader stdin = new BufferedReader(
//                            new InputStreamReader(System.in));
////                    String outMessage = stdin.readLine();
//                    String outMessage = String.valueOf(System.currentTimeMillis());
//                    if (outMessage.equals("bye"))
//                        break;
//                    buf = outMessage.getBytes();
//                    System.out.println("Send " + outMessage + " to " + hostAddress);
//                    // Send packet to hostAddress:9999, server that listen
//                    // 9999 would reply this packet
//                    DatagramPacket out = new DatagramPacket(buf,
//                            buf.length, hostAddress, packetPort);
//                    detectSocket.send(out);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//
//        // Receive packet thread.
//        new Thread(() -> {
//            System.out.println("Receive thread started.");
//            while (true) {
//                byte[] buf = new byte[1024];
//                DatagramPacket packet = new DatagramPacket(buf, buf.length);
//                try {
//                    detectSocket.receive(packet);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                String rcvd = "Received from " + packet.getSocketAddress() + ", Data="
//                        + new String(packet.getData(), 0, packet.getLength());
//                System.out.println(rcvd);
//            }
//        });
    }
}
