import java.io.IOException;
import java.net.*;

public class ShowSpeed {
    public static void main(String[] args) throws SocketException {
        boolean test = true;

        final String[] messageBSM = new String[2];
        DatagramPacket packet9 = new DatagramPacket(new byte[2048], 2048), packet1 = new DatagramPacket(new byte[2048], 2048);
        DatagramSocket socket9 = new DatagramSocket(8889), socket1 = new DatagramSocket(8891);
        socket9.setSoTimeout(3000);
        socket1.setSoTimeout(3000);
        new Thread(() -> {
            int i = 0;
            while (true) {
                try {
                    socket9.receive(packet9);
                    byte[] dataBSM = packet9.getData();
                    if (test) messageBSM[0] = new String(dataBSM, 0, dataBSM.length);
                    else {
                        messageBSM[0] = bytesToHexString(dataBSM);
                    }
                    double speed = Integer.parseInt(messageBSM[0].substring(42, 46), 16)
                            % Integer.parseInt("10000000000000", 2) * 0.02;
                    System.out.println("car1 = " + String.format(String.valueOf(speed), "%.2f") + " m/s");
                } catch (SocketTimeoutException e) {
                    System.out.println("car1 接收超时" + i++);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(() -> {
            int i = 0;
            while (true) {
                try {
                    socket1.receive(packet1);
                    byte[] dataBSM = packet1.getData();
                    if (test) messageBSM[1] = new String(dataBSM, 0, dataBSM.length);
                    else {
                        messageBSM[1] = bytesToHexString(dataBSM);
                    }
                    double speed = Integer.parseInt(messageBSM[0].substring(42, 46), 16)
                            % Integer.parseInt("10000000000000", 2) * 0.02;
                    System.out.println("\t\t\t\tcar2 = " + String.format(String.valueOf(speed), "%.2f") + " m/s");
                } catch (SocketTimeoutException e) {
                    System.out.println("\t\t\t\tcar2 接收超时" + i++);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) return null;
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) stringBuilder.append(0);
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
