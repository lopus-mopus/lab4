package lab3;

import java.io.IOException;
import java.net.*;

public class Parent extends Neighbor{
    public Parent(String ip, int port) throws UnknownHostException {
        super();
        address = InetAddress.getByName(ip);
        this.port = port;
    }

    public void parentHandshake(DatagramSocket socket, String nodeName) throws IOException {
        int count = 0;
        byte[] buf = new byte[1024];
        DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);

        byte[] initMsg = (new Message(nodeName)).getInitMessage();

        DatagramPacket sendPacket = new DatagramPacket(initMsg, initMsg.length, address, port);

        Message msg;

        do {
            socket.send(sendPacket);
            try {
                socket.receive(recvPacket);
            } catch (SocketTimeoutException ex){}

            msg = new Message(buf);
            count++;

            if (20 < count){
                throw new WrongNodeException("Can't reach the parent's node");
            }
        } while (!address.equals(recvPacket.getAddress()) &&
                port != recvPacket.getPort() &&
                MessageType.INIT_MSG != msg.messageType());

        name = msg.nameFrom();
    }

    public String name(){
        return name;
    }
}