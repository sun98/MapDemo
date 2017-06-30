import org.omg.PortableServer.THREAD_POLICY_ID;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {

    private static final int PC_PORT = 8888;
    private static final int PHONE_PORT = 8888;

    public static void main(String[] args) {
        try {
            /*adb 指令*/

            /*转发端口，实现连接*/
            Runtime.getRuntime().exec("adb forward tcp:" + String.valueOf(PC_PORT) + " tcp:" + String.valueOf(PHONE_PORT));
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Socket socket = null;
        BufferedReader reader = null;
        try {
            InetAddress serveraddr = InetAddress.getByName("127.0.0.1");
            System.out.println(serveraddr);
            System.out.println("Connecting...");
            socket = new Socket(serveraddr, PC_PORT);
            System.out.println("Connected");
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            File file = new File("src/simu.txt");
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                out.write(line.getBytes());
                out.flush();
                System.out.println("Send: " + line);
                Thread.sleep(100);
            }
            reader.close();
//            while (true) {
//                /*每隔一秒写一条消息（当前时间毫秒数）发送过去*/
//                String currentTimeMills = String.valueOf(System.currentTimeMillis());
//                out.write(currentTimeMills.getBytes());
//                out.flush();
//                System.out.println("Send " + currentTimeMills);
//                Thread.sleep(100);
//            }
        } catch (UnknownHostException e1) {
            System.out.println("ERROR1:" + e1.toString());
        } catch (Exception e2) {
            System.out.println("ERROR2:" + e2.toString());
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                    System.out.println("socket.close()");
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.out.println("ERROR:" + e.toString());
            }
        }
    }
}
