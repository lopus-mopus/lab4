package lab3;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Message {
    private int idx = 0;
    private String text = null;
    private long sendTime = 0;
    private String nameFrom = null;

    private String newParentIP = null;
    private int newParentPort = 0;

    private MessageType type;

    public Message(byte[] data){
        ByteBuffer msg = ByteBuffer.wrap(data);

        byte messageType = msg.get();

        switch(messageType){
            case 1:
                type = MessageType.INIT_MSG;
                parseInitMsg(msg);
                break;
            case 2:
                type = MessageType.TEXT_MSG;
                parseTextMsg(msg);
                break;
            case 3:
                type = MessageType.ACK;
                parseAckMsg(msg);
                break;
            case 4:
                type = MessageType.DEAD_CHILD;
                parseDeadChild(msg);
                break;
            case 5:
                type = MessageType.DEAD_PARENT;
                parseDeadParent(msg);
                break;
            default:
                type = MessageType.UNKNOWN;
        }
    }

    public Message(String nameFrom){
        this.nameFrom = nameFrom;
    }

    public Message(int idx, String text, String nameFrom){
        this.idx = idx;
        this.text = text;
        this.nameFrom = nameFrom;
        sendTime = 0;
    }

    public int id(){
        return idx;
    }

    public MessageType messageType(){
        return type;
    }

    public String nameFrom(){
        return nameFrom;
    }

    public String text(){
        return text;
    }

    public String newParentIp(){
        return newParentIP;
    }

    public int newParentPort(){
        return newParentPort;
    }

    public void resetSendTime(){
        sendTime = System.currentTimeMillis();
    }

    public boolean isOld(long timeOut){
        return timeOut < System.currentTimeMillis() - sendTime;
    }

    public byte[] getInitMessage(){
        ByteBuffer msg = ByteBuffer.allocate(1 + 4 + nameFrom.length());

        msg.put((byte)1);
        msg.putInt(nameFrom.length());
        msg.put(nameFrom.getBytes(StandardCharsets.UTF_8));

        msg.flip();

        return msg.array();
    }

    public byte[] getTextMessage(){
        ByteBuffer msg = ByteBuffer.allocate(1 + 4 + 4 + nameFrom.length() + 4 + text.length());

        msg.put((byte)2);
        msg.putInt(idx);
        msg.putInt(nameFrom.length());
        msg.put(nameFrom.getBytes(StandardCharsets.UTF_8));
        msg.putInt(text.length());
        msg.put(text.getBytes(StandardCharsets.UTF_8));

        msg.flip();

        return msg.array();
    }

    public byte[] getAck(){
        ByteBuffer msg = ByteBuffer.allocate(1 + 4 + 4 + nameFrom.length());

        msg.put((byte)3);
        msg.putInt(idx);
        msg.putInt(nameFrom.length());
        msg.put(nameFrom.getBytes(StandardCharsets.UTF_8));

        msg.flip();

        return msg.array();
    }

    public byte[] getDeadChildMsg(){
        ByteBuffer msg = ByteBuffer.allocate(1 + 4 + 4 + nameFrom.length());

        msg.put((byte) 4);
        msg.putInt(idx);
        msg.putInt(nameFrom.length());
        msg.put(nameFrom.getBytes(StandardCharsets.UTF_8));

        msg.flip();

        return msg.array();
    }

    public byte[] getDeadParentMsg(String ip, int port) {
        ByteBuffer msg = ByteBuffer.allocate(1 + 4 + 4 + nameFrom.length() + 4 + ip.length() + 4);

        msg.put((byte) 5);
        msg.putInt(idx);
        msg.putInt(nameFrom.length());
        msg.put(nameFrom.getBytes(StandardCharsets.UTF_8));
        msg.putInt(ip.length());
        msg.put(ip.getBytes(StandardCharsets.UTF_8));
        msg.putInt(port);

        msg.flip();

        return msg.array();
    }

    private void parseInitMsg(ByteBuffer msg){
        parseNameFrom(msg);
    }

    private void parseTextMsg(ByteBuffer msg){
        idx = msg.getInt();

        parseNameFrom(msg);

        int msgLength = msg.getInt();
        byte[] data = new byte[msgLength];
        msg.get(data, 0, msgLength);
        text = new String(data, StandardCharsets.UTF_8);
    }

    private void parseAckMsg(ByteBuffer msg){
        idx = msg.getInt();
        parseNameFrom(msg);
    }

    private void parseDeadChild(ByteBuffer msg){
        idx = msg.getInt();
        parseNameFrom(msg);
    }

    private void parseDeadParent(ByteBuffer msg){
        idx = msg.getInt();
        parseNameFrom(msg);

        int ipLength = msg.getInt();
        byte[] ip = new byte[ipLength];
        msg.get(ip, 0, ipLength);
        newParentIP = new String(ip, StandardCharsets.UTF_8);
        newParentPort = msg.getInt();
    }

    private void parseNameFrom(ByteBuffer msg){
        int nameLength = msg.getInt();
        byte[] name = new byte[nameLength];

        msg.get(name, 0, nameLength);
        nameFrom = new String(name, StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj) {
            return true;
        }
        if (null == obj) {
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }

        Message msg = (Message) obj;
        return idx == msg.idx;
    }
}