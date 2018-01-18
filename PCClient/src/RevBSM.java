import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.util.Objects;

public class RevBSM {
    static DatagramSocket socket1;
    static InetAddress inetAddress;


    public static void main(String[] args) throws SocketException, UnknownHostException, InterruptedException {
        socket1 = new DatagramSocket(8891);
        inetAddress = InetAddress.getByName("192.168.0.193");
        RandomAccessFile rf = null;
        try {
            rf = new RandomAccessFile("src/1514258737023.txt", "r");
            long len = rf.length();
            long start = rf.getFilePointer();
            long nextend = start + len - 1;
            String line;
            rf.seek(nextend);
            int c = -1;
            while (nextend > start) {
//                喵喵瞄？
                Thread.sleep(1);
                c = rf.read();
                if (c == '\n' || c == '\r') {
                    line = rf.readLine();
                    if (line != null) {
                        String str = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                        send(str);
                    }
                    nextend--;
                }
                nextend--;
                rf.seek(nextend);
                if (nextend == 0) {
                    String str = new String(rf.readLine().getBytes("ISO-8859-1"), "UTF-8");
                    send(str);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rf != null)
                    rf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void send(String str) throws IOException {
        switch (str.substring(0, 4)) {
            case "BSM:": {
                String info = str.substring(5);
                if (!Objects.equals(info, "null")) {
                    DatagramPacket packet = new DatagramPacket(info.getBytes(), info.getBytes().length, inetAddress, 8891);
                    socket1.send(packet);
                    System.out.println("bsm\t\t" + str);
                }
                break;
            }
        }
    }
}
