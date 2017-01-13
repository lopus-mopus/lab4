package lab3;

import lab3.UI;
import lab3.SendType;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class Node {
    private final Object mutex = new Object();
    private String nodeName = null;
    private int propLoss = 0;
    private Parent parent = null;
    private boolean hasParent = false;

    private boolean hadParent = false;
    private Parent deadParent = null;

    private Map<String, Child> deadChildren = new HashMap<>();
    private Map<String, Child> children = new HashMap<>();

    private DatagramSocket socket;

    private UI ui = null;

    private boolean ackDeadChildMsg = false;
    private Set<String> ackDeadParentMsg = new HashSet<>();

    private Random loss = new Random();
//конструктор
    public Node(String nodeName, int propLoss, int nodePort, Parent parent) throws IOException {
        this.nodeName = nodeName;
        this.propLoss = propLoss;


        socket = new DatagramSocket(nodePort);
        socket.setSoTimeout(Constants.TIMEOUT);

        if (null != parent) {
            this.parent = parent;
            hasParent = true;
            parent.parentHandshake(socket, nodeName);
        }
    }
//принять новый юзер интерфейс
    public void setUI(UI ui) {
        this.ui = ui;
    }
//отправка сообщения в буфер
    public void sendUserMessage(String message) {
        try {
            synchronized (mutex) {
                sendToEach(nodeName + ": " + message, null);
            }
        } catch (InterruptedException ex) {
            System.out.println(ex.getMessage());
        }
    }
//осуществляет сетевое взаимодействие
    public void communicate() throws IOException, InterruptedException {
        byte[] buf = new byte[Constants.MAX_SIZE];
        DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);

        sendQueuedMsgs(SendType.ALL);
        try {
            socket.receive(recvPacket);
        } catch (SocketTimeoutException ex) {
            return;
        }

        if (propLoss > loss.nextInt(100)){
            return;
        }

        parseMessage(recvPacket);
    }
//прекращение
    public void stopCommunicate() throws IOException {
        flush();
        int count = 0;

        boolean choseParent = false;
        String parName = null;
        String ip = null;
        int port = 0;

        ackDeadParentMsg.addAll(children.keySet());
        sendQueuedMsgs(SendType.ACK);
        
        while (!(ackDeadChildMsg && ackDeadParentMsg.isEmpty()) && count < 10) {
            count++;
            if (hasParent) {
                if (!ackDeadChildMsg) {
                    parent.sendDeadChildMsg(socket, nodeName);
                }
//отсылает сообщения всем и в первую очередь родителю
                for (Map.Entry<String, Child> child : children.entrySet()) {
                    if (ackDeadParentMsg.contains(child.getKey())) {
                        child.getValue().sendDeadParentMsg(socket, nodeName, parent.ip(), parent.port());
                    }
                }
//получаем подтверждение что сообщение всем доставлено
                recvAcks();
            } else {
                for (Map.Entry<String, Child> child : children.entrySet()) {
                    if (choseParent) {
                        if (ackDeadParentMsg.contains(child.getKey())) {
                            if (child.getKey().equals(parName)) {
                                child.getValue().sendDeadChildMsg(socket, nodeName);
                            } else {
                                child.getValue().sendDeadParentMsg(socket, nodeName, ip, port);
                            }
                        }
                    } else {
                        child.getValue().sendDeadChildMsg(socket, nodeName);
                        parName = child.getKey();
                        ip = child.getValue().ip();
                        port = child.getValue().port();
                        choseParent = true;
                    }
//если родителя нет то новым р становится 1 насл
                    recvAcks();
                }
            }
        }
    }
//отправка сообщения всем соседним узлам
    public void sendQueuedMsgs(SendType sendType) throws IOException {
        synchronized (mutex) {
            if (hasParent) {
                if (sendType == SendType.ACK)
                	parent.sendAckData(socket);
                else
                	parent.sendData(socket);
            }

            for (Map.Entry<String, Child> child : children.entrySet()) {
            	if (sendType == SendType.ACK)
                	child.getValue().sendAckData(socket);
                else
                	child.getValue().sendData(socket);
            }
        }
    }
//регестрирует акка
    private void recvAcks() throws IOException {
        try {
            for (; ; ) {
                Message msg = recv();
                if (MessageType.ACK != msg.messageType()) {
                    continue;
                }
                if (hasParent
                        && msg.nameFrom().equals(parent.name())
                        && msg.id() == parent.deadMessage.id()) {

                    ackDeadChildMsg = true;
                }
                if (ackDeadParentMsg.contains(msg.nameFrom())
                        && msg.id() == children.get(msg.nameFrom()).deadMessage.id()) {

                    ackDeadParentMsg.remove(msg.nameFrom());
                }
            }
        } catch (SocketTimeoutException ex) {
        }

    }

    private Message recv() throws SocketTimeoutException,
            IOException {
        byte[] buf = new byte[Constants.MAX_SIZE];
        DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);

        socket.receive(recvPacket);

        return new Message(buf);
    }
//отправляет все ообщения из буфера всем
    private void flush() throws IOException {
        boolean isQueuedMsg = true;
        int eff = 0;

        while (isQueuedMsg && eff < 20) {
            int counter = 0;
            boolean isParentMsg = false;
            boolean isChildMsg = false;

            eff++;

            if (hasParent) {
                parent.sendData(socket);
                isParentMsg = parent.hasMessages();
            }

            for (Map.Entry<String, Child> child : children.entrySet()) {
                child.getValue().sendData(socket);
                isChildMsg = isChildMsg || child.getValue().hasMessages();
            }

            while (counter < children.size()) {
                byte[] buf = new byte[Constants.MAX_SIZE];
                DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(recvPacket);
                } catch (SocketTimeoutException ex) {
                    return;
                }
                Message msg = new Message(recvPacket.getData());
                if (msg.messageType() != MessageType.ACK) {
                    continue;
                } else {
                    handleAckMsg(msg);
                }
            }

            isQueuedMsg = isChildMsg || isParentMsg;
        }
    }

    private void sendToEach(String message, String node) throws InterruptedException {
        for (Map.Entry<String, Child> child : children.entrySet()) {
            if (child.getKey().equals(node)) {
                continue;
            }

            child.getValue().send(message, nodeName);
        }

        if (hasParent && !parent.name().equals(node)) {
            parent.send(message, nodeName);
        }
    }
//парсинг сообщения
    private void parseMessage(DatagramPacket recvPacket) throws IOException, InterruptedException {
        Message msg = new Message(recvPacket.getData());

        switch (msg.messageType()) {
            case INIT_MSG:
                handleNewChildrenMsg(msg.nameFrom(), recvPacket.getAddress(), recvPacket.getPort());
                break;
            case TEXT_MSG:
                handleTextMsg(msg);
                break;
            case ACK:
                handleAckMsg(msg);
                break;
            case DEAD_CHILD:
                handleDeadChildMsg(msg);
                break;
            case DEAD_PARENT:
                handleDeadParentMsg(msg);
                break;
        }
    }
//обработка нового соеденения
    private void handleNewChildrenMsg(String name, InetAddress address, int port) throws IOException {
        synchronized (mutex) {
            if (!children.containsKey(name)) {
                children.put(name, new Child(address, port, name));
            }

            children.get(name).childHandshake(socket, nodeName);
        }
    }
//обработка собщеня
    private void handleTextMsg(Message msg) throws IOException,
            InterruptedException {
        boolean wasAccepted = false;

        if (hasParent && parent.name().equals(msg.nameFrom())) {
            wasAccepted = parent.wasAccepted(msg.id());
            parent.sendAck(msg.id(), socket, nodeName);
        } else if (hadParent && deadParent.name().equals(msg.nameFrom())) {
            wasAccepted = deadParent.wasAccepted(msg.id());
            deadParent.sendAck(msg.id(), socket, nodeName);
        }

        if (children.containsKey(msg.nameFrom())) {
            wasAccepted = children.get(msg.nameFrom()).wasAccepted(msg.id());
            children.get(msg.nameFrom()).sendAck(msg.id(), socket, nodeName);
        } else if (deadChildren.containsKey(msg.nameFrom())) {
            wasAccepted = deadChildren.get(msg.nameFrom()).wasAccepted(msg.id());
            deadChildren.get(msg.nameFrom()).sendAck(msg.id(), socket, nodeName);
        }

        if (wasAccepted) return;

        sendToEach(msg.text(), msg.nameFrom());
        ui.print(msg.text());
    }
    
//обработка сообщния если пришли одни акка
    private void handleAckMsg(Message msg) {
        if (hasParent && parent.name().equals(msg.nameFrom())) {
            parent.ack(msg.id());
        }

        if (children.containsKey(msg.nameFrom())) {
            children.get(msg.nameFrom()).ack(msg.id());
        }
    }
//смерть ребенка
    private void handleDeadChildMsg(Message msg) throws IOException {
        System.out.println("dead child " + msg.nameFrom());
        
        boolean wasAccepted = false;
            
        if (hasParent && msg.nameFrom().equals(parent.name())) {
            deadParent = parent;
            parent = null;
            hasParent = false;
            hadParent = true;
            
            if (!parent.wasAccepted(msg.id()))
            	deadParent.sendAck(msg.id(), socket, nodeName);
        } else
        if (hadParent && deadParent.name().equals(msg.nameFrom())){
        	if (!deadParent.wasAccepted(msg.id()))
        		deadParent.sendAck(msg.id(), socket, nodeName);
        } else
        if (!deadChildren.containsKey(msg.nameFrom())) {
            deadChildren.put(msg.nameFrom(), children.get(msg.nameFrom()));
            children.remove(msg.nameFrom());
            
            if (!deadChildren.get(msg.nameFrom()).wasAccepted(msg.id()))
            	deadChildren.get(msg.nameFrom()).sendAck(msg.id(), socket, nodeName);
        }
    }
    
//смерть родителя
    private void handleDeadParentMsg(Message msg) throws IOException {
        System.out.println("dead parent " + msg.nameFrom());
        if (!hasParent) {
            return;
        }

        deadParent = parent;
        
        if (!deadParent.wasAccepted(msg.id()))
        	deadParent.sendAck(msg.id(), socket, nodeName);

        hadParent = true;

        parent = new Parent(msg.newParentIp(), msg.newParentPort());
        parent.parentHandshake(socket, nodeName);
    }
}
