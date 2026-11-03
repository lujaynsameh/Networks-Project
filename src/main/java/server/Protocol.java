package server;

public interface Protocol {
    String encode(Message m);
    Message decode(String raw);
}