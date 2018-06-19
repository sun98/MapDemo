import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.Packet;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;

import java.io.IOException;

public class PcapParser {
    public static void main(String[] args) throws IOException {
        final Pcap pcap = Pcap.openStream("D:\\All_Projects\\Android_Projects\\MapDemo - copy\\PCClient\\src\\usb0.pcap");
        pcap.loop(packet -> {
            if (packet.hasProtocol(Protocol.UDP)) {
                UDPPacket udpPacket = (UDPPacket) packet.getPacket(Protocol.UDP);
                Buffer buffer = udpPacket.getPayload();
                if (buffer != null) {
                    System.out.println(buffer);
                }
            }
            return true;
        });
    }
}