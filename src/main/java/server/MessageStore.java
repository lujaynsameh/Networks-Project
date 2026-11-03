package server;

import java.util.List;

public interface MessageStore {
    Message add(Message m);
    List<Message> getAfter(long lastId);
}