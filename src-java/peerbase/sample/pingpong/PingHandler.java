package peerbase.sample.pingpong;

import peerbase.HandlerInterface;
import peerbase.LoggerUtil;
import peerbase.PeerConnection;
import peerbase.PeerMessage;

/**
 * Sends a pong message to where sent a ping message
 */
public class PingHandler implements HandlerInterface {

    @Override
    public void handleMessage(PeerConnection conn, PeerMessage msg) {
        String[] args = msg.getMsgData().split(" ");
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        System.out.println("Ping from " + host + ":" + port);

        conn.sendData(new PeerMessage(Message.PONG.msg(), ""));
        System.out.println("Sent a Pong to " + host + ":" + port);
    }
}
