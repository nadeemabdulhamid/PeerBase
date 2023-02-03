package peerbase.sample.pingpong;

public enum Message {
    PING("PING"), PONG("PONG");

    private String msg;

    Message(String msg) {
        this.msg = msg;
    }

    public String msg() {
        return msg;
    }
}
