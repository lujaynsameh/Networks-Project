package server;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps all messages in memory (a simple list).
 * Messages are lost when the server stops. Person 3's database store
 * will replace this later, which is why we use the MessageStore interface.
 */
public class InMemoryMessageStore implements MessageStore {

    // All stored messages, in the order they arrived
    private final List<Message> messages = new ArrayList<>();

    // The id the next message will get (ids start at 1)
    private long nextId = 1;

    /**
     * Gives the message the next id, stores it, and returns it.
     * "synchronized" means only one thread can run this at a time, so two
     * clients sending at the same moment can never get the same id.
     */
    @Override
    public synchronized Message add(Message m) {
        m.id = nextId;
        nextId++;
        messages.add(m);
        return m;
    }

    /**
     * Returns every message whose id is bigger than lastId, in order.
     * A client that was offline says "my last message was id 42" and
     * gets everything after 42.
     */
    @Override
    public synchronized List<Message> getAfter(long lastId) {
        List<Message> result = new ArrayList<>();
        for (Message m : messages) {
            if (m.id > lastId) {
                result.add(m);
            }
        }
        return result;
    }
}