package lab3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
//import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
//сосед
public class Neighbor {
    protected LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>(Constants.QUEUE_MAX_CAPACITY);

    public Message deadMessage = null;

    private Set<Integer> accepted = new HashSet<>();

    protected InetAddress address = null;
    protected int port = 0;

    protected int sendIdx = 1;

    protected String name = null;

    public void send(String message, String nameFrom) throws InterruptedException {
    	if (messages.remainingCapacity() == 0)
    		messages.remove();
        messages.offer(new Message(sendIdx++, message, nameFrom), Constants.TIMEOUT * 2, TimeUnit.MILLISECONDS);
    }
//отправка акка
    public void sendAck(int id, DatagramSocket socket, String nodeName) throws IOException {
        accepted.add(id);

        if (Constants.ACCEPTED_COUNT < accepted.size()){
            accepted.remove(accepted.stream().min(Comparator.naturalOrder()).get());
        }

        byte[] data = (new Message(id, "", nodeName)).getAck();
        socket.send(new DatagramPacket(data, data.length, address, port));
    }
//тправка данных
    public void sendData(DatagramSocket socket) throws IOException{
        for (Message msg : messages){
            if (msg.isOld(Constants.TIMEOUT)){
                byte[] data = msg.getTextMessage();
                socket.send(new DatagramPacket(data, data.length, address, port));
                msg.resetSendTime();
            }
        }
    }
    public void sendAckData(DatagramSocket socket) throws IOException{
        for (Message msg : messages){
            if (msg.isOld(Constants.TIMEOUT) && (msg.messageType() == MessageType.ACK)) {
                byte[] data = msg.getTextMessage();
                socket.send(new DatagramPacket(data, data.length, address, port));
                msg.resetSendTime();
            }
        }
    } 
//смерть ребенка
    public void sendDeadChildMsg(DatagramSocket socket, String nodeName) throws IOException{
        deadMessage = new Message(sendIdx++, "", nodeName);
        byte[] data = deadMessage.getDeadChildMsg();
        socket.send(new DatagramPacket(data, data.length, address, port));
    }
//смерть родителя
    public void sendDeadParentMsg(DatagramSocket socket, String nodeName, String ip, int port) throws IOException{
        deadMessage = new Message(sendIdx, "", nodeName);
        byte[] data = deadMessage.getDeadParentMsg(ip, port);
        socket.send(new DatagramPacket(data, data.length, address, this.port));
    }

    public boolean wasAccepted(int id){
        return accepted.contains(id);
    }

    public void ack(int idx){
        messages.remove(new Message(idx, "", ""));
    }

    public boolean hasMessages(){
        return !messages.isEmpty();
    }

    public String ip(){
        return address.getHostAddress();
    }

    public int port(){
        return port;
    }

    public Message getDeadMessage(){
        return deadMessage;
    }
}
