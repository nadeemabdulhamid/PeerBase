package peerbase.sample.pingpong;

import peerbase.LoggerUtil;
import peerbase.Node;
import peerbase.PeerInfo;
import peerbase.PeerMessage;
import peerbase.util.SimpleRouter;

import java.util.List;

public class PingPongNode extends Node {
    public PingPongNode(PeerInfo peerInfo) {
        super(5, peerInfo);

        // handlers
        addHandler(Message.PING.msg(), new PingHandler());

        // router
        addRouter(new SimpleRouter(this));
    }

    public void ping(String host, int port) {
        PeerInfo info = new PeerInfo(host, port);
        List<PeerMessage> replies = connectAndSend(info, Message.PING.name(), getHost() + " " + getPort(), true);
        System.out.println("Sent a ping to " + info);

        if (replies.isEmpty()) {
            System.out.println(info + " didn't reply to ping you sent");
            return;
        }

        PeerMessage reply = replies.get(0);
        if (reply.getMsgType().equals(Message.PONG.msg())) {
            System.out.println("Pong is arrived from " + info);
        }
    }
}



