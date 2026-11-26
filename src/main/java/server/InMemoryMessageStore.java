package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps all messages in memory (a simple list).
 * Messages are lost when the server stops. Person 3's database store
 * will replace this later, which is why we use the MessageStore interface.
 */
public class InMemoryMessageStore implements MessageStore {

    // All stored messages, in the order they arrived
    private final List<Message> messages = new ArrayList<>();

    // Remembers which clientMsgId we already stored, and which message it became.
    private final Map<String, Message> seenClientIds = new HashMap<>();

    // The id the next message will get (ids start at 1)
    private long nextId = 1;

    /**
     * Stores the message and gives it the next id.
     * If a message with the same clientMsgId was already stored, nothing new
     * is added and the ORIGINAL message is returned (this prevents duplicates).
     * "synchronized" means only one thread runs this at a time, so two
     * clients sending at the same moment can never get the same id.
     */
    @Override
    public synchronized Message add(Message m) {
        // Duplicate check (only if the client gave us an id)
        if (m.clientMsgId != null) {
            Message existing = seenClientIds.get(m.clientMsgId);
            if (existing != null) {
                return existing;
            }
        }

        m.id = nextId;
        nextId++;
        messages.add(m);

        if (m.clientMsgId != null) {
            seenClientIds.put(m.clientMsgId, m);
        }
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