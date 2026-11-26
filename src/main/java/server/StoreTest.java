package server;

import java.util.List;

/**
 * Small test for InMemoryMessageStore.
 * Run it, and every line should say PASS.
 */
public class StoreTest {

    // Prints PASS or FAIL for one check
    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS: " : "FAIL: ") + name);
    }

    // Makes a chat message with a given clientMsgId
static Message make(String clientId, String text) {
    Message m = new Message();
    m.type = Message.Type.CHAT;
    m.sender = "jana";
    m.target = "halas";
    m.text = text;
    m.clientMsgId = clientId;
    m.timestamp = System.currentTimeMillis();
    return m;
}

    public static void main(String[] args) {
        MessageStore store = new InMemoryMessageStore();

        // Test 1: ids start at 1 and go up by one
        Message a = store.add(make("a", "hello"));
        Message b = store.add(make("b", "how are you"));
        Message c = store.add(make("c", "bye"));
        check("first message gets id 1", a.id == 1);
        check("second message gets id 2", b.id == 2);
        check("third message gets id 3", c.id == 3);

        // Test 2: sending clientMsgId "a" again must NOT create a new message
        Message again = store.add(make("a", "hello"));
        check("duplicate returns the original id (1)", again.id == 1);
        check("duplicate is not stored (still 3 messages)", store.getAfter(0).size() == 3);

        // Test 3: getAfter returns only newer messages
        List<Message> after1 = store.getAfter(1);
        check("getAfter(1) returns 2 messages", after1.size() == 2);
        check("getAfter(1) starts with id 2", after1.get(0).id == 2);
        check("getAfter(3) returns nothing", store.getAfter(3).isEmpty());
    }
}