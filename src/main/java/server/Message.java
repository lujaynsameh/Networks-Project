package server;

public class Message {
    public enum Type { CHAT, HELLO, ACK, REGISTER, LOGIN, JOIN, ERROR }

    public long id;              // assigned by the server
    public String clientMsgId;   // made by the client
    public Type type;
    public String sender;
    public String target;        // a username, or #roomname
    public String text;
    public long timestamp;

    public Message() {}
}
