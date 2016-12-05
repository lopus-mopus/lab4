package lab3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Child extends Neighbor{
    public Child(InetAddress address, int port, String name){
        super();
        this.address = address;
        this.port = port;
        this.name = name;
    }

    public void childHandshake(DatagramSocket socket, String nodeName) throws IOException {
        byte[] initMsg = (new Message(nodeName)).getInitMessage();
        socket.send(new DatagramPacket(initMsg, initMsg.length, address, port));
    }
}
