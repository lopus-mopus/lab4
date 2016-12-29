package lab3;

import lab3.Node;
import lab3.Parent;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("You didn't enter name or port of the node or probability of packet loss\n");
            return;
        }

        Parent parent = null;

        if (args.length >= 5) {
            try {
                parent = new Parent(args[3], Integer.parseInt(args[4]));
            } catch (UnknownHostException ex) {
                System.out.println(ex.getMessage());
                return;
            }
        }
        Node node;

        try {
            node = new Node(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), parent);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return;
        }
        
        UI ui = new UI(node);
        node.setUI(ui);
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	ui.stop();
                System.out.println("close win!");
            }
        });
        
        
        ui.start();
    }
}