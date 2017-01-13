package lab3;

import lab3.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UI {
    private Thread netThread;
    private Node node;

    public UI(Node node){
        this.node = node;


        netThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    this.node.communicate();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                } catch (InterruptedException ex){
                    break;
                }
            }

            try {
                this.node.stopCommunicate();
                System.out.println("111");
            } catch (IOException ex){
                System.out.println(ex.getMessage());
            }
        });
    }

    public void print(String msg){
        System.out.println(msg);
    }

    public void stop() {
    	netThread.interrupt();
    }
    
    public void start(){
        netThread.start();
        String userMessage = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        for (;;){
            try {
                userMessage = reader.readLine();
            } catch (IOException ex){
                System.out.println(ex.getMessage());
            }

            if ("".equals(userMessage)) {
                break;
            }

            System.out.println("You: " + userMessage);
            node.sendUserMessage(userMessage);
        }

        netThread.interrupt();
    }
}