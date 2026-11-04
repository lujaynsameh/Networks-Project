package server;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scalability test (task 1.10).
 * For each N, opens N fake clients; every client sends MSGS_PER_CLIENT messages.
 * Latency = time until the sender receives its own message back from the
 * broadcast (round trip: client -> server -> client).
 *
 * Run (server must be running):
 *   mvn -q compile exec:java -Dexec.mainClass=server.LoadTest [-Dexec.args="localhost"]
 */
public class LoadTest {
    static final int[] SIZES = {1, 2, 5, 10, 50};
    static final int MSGS_PER_CLIENT = 20;
    static final int INTERVAL_MS = 50;

    static class Bot extends WebSocketClient {
        final int id;
        final ConcurrentHashMap<String, Long> sentAt = new ConcurrentHashMap<>();
        final AtomicLong totalLatencyNs = new AtomicLong();
        final AtomicLong received = new AtomicLong();
        final CountDownLatch opened = new CountDownLatch(1);

        Bot(URI uri, int id) { super(uri); this.id = id; }

        @Override public void onOpen(ServerHandshake h) { opened.countDown(); }

        @Override public void onMessage(String msg) {
            Long t0 = sentAt.remove(msg);
            if (t0 != null) { // only measure our own messages
                totalLatencyNs.addAndGet(System.nanoTime() - t0);
                received.incrementAndGet();
            }
        }
        @Override public void onClose(int c, String r, boolean remote) {}
        @Override public void onError(Exception e) {}
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        URI uri = new URI("ws://" + host + ":8081");

        StringBuilder table = new StringBuilder();
        table.append("| Clients (N) | Messages sent | Avg latency (ms) | Delivered own echoes |\n");
        table.append("|---|---|---|---|\n");

        for (int n : SIZES) {
            List<Bot> bots = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Bot b = new Bot(uri, i);
                b.connect();
                bots.add(b);
            }
            for (Bot b : bots) {
                if (!b.opened.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("client " + b.id + " failed to connect");
                }
            }

            List<Thread> senders = new ArrayList<>();
            for (Bot b : bots) {
                Thread t = new Thread(() -> {
                    try {
                        for (int m = 0; m < MSGS_PER_CLIENT; m++) {
                            String payload = "LT|" + n + "|" + b.id + "|" + m;
                            b.sentAt.put(payload, System.nanoTime());
                            b.send(payload);
                            Thread.sleep(INTERVAL_MS);
                        }
                    } catch (Exception ignored) {}
                });
                senders.add(t);
                t.start();
            }
            for (Thread t : senders) t.join();
            Thread.sleep(1000); // let in-flight echoes arrive

            long totalNs = 0, got = 0;
            for (Bot b : bots) {
                totalNs += b.totalLatencyNs.get();
                got += b.received.get();
                b.close();
            }
            long sent = (long) n * MSGS_PER_CLIENT;
            double avgMs = got == 0 ? Double.NaN : totalNs / (double) got / 1e6;
            table.append(String.format("| %d | %d | %.2f | %d |%n", n, sent, avgMs, got));
            System.out.printf("N=%d done: avg %.2f ms (%d/%d echoes)%n", n, avgMs, got, sent);
            Thread.sleep(500);
        }
        System.out.println();
        System.out.println(table);
    }
}
