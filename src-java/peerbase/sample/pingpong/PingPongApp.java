package peerbase.sample.pingpong;

import peerbase.LoggerUtil;
import peerbase.PeerInfo;

import java.util.Scanner;
import java.util.logging.Level;

public class PingPongApp {
    private PingPongNode node;

    public static void main(String[] args) {
        new PingPongApp();
    }

    public PingPongApp() {
        System.out.println("[ PingPongApp ]");
        Scanner sc = new Scanner(System.in);

        String cmd = "";
        while (true) {
            System.out.print("> ");
            cmd = sc.nextLine();

            if (cmd.startsWith("init")) {
                init(cmd);
            } else if (cmd.startsWith("ping")) {
                ping(cmd);
            } else if (cmd.equalsIgnoreCase("exit")) {
                break;
            } else {
                System.out.println("Wrong command. Try ");
                System.out.println("init <id> <host> <port>");
                System.out.println("ping <host> <port>");
                System.out.println("exit");
            }
        }

        System.out.println("Bye");
    }

    private void init(String cmd) {
        // remove cmd head
        cmd = cmd.substring("init ".length());

        // init <id> <host> <port>
        String[] args = cmd.split(" ");
        String id = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        this.node = new PingPongNode(new PeerInfo(id, host, port));
        new Thread(() -> node.mainLoop()).start();

        System.out.println("Initialized successfully");
    }

    private void ping(String cmd) {
        // remove cmd head
        cmd = cmd.substring("ping ".length());

        // ping <host> <port>
        String[] args = cmd.split(" ");
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        this.node.ping(host, port);
    }
}
