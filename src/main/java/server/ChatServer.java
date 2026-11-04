package server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket chat server (tasks 1.2 - 1.5).
 * Java-WebSocket gives each connection a worker thread from its decoder pool,
 * so many clients are served concurrently. Shared state lives in a
 * ConcurrentHashMap, so no manual locking is needed.
 *
 * Integration points for teammates (task 1.9): see handleMessage().
 */
public class ChatServer extends WebSocketServer {

    /** connection -> username (thread-safe, task 1.3). */
    private final Map<WebSocket, String> clients = new ConcurrentHashMap<>();
    private final AtomicInteger guestCounter = new AtomicInteger();
    private final Metrics metrics;

    public ChatServer(int port, Metrics metrics) {
        super(new InetSocketAddress(port)); // binds to 0.0.0.0, reachable from phones
        this.metrics = metrics;
        setReuseAddr(true);
    }

    @Override
    public void onStart() {
        System.out.println("[ws] ChatServer listening on port " + getPort());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String name = "guest-" + guestCounter.incrementAndGet();
        clients.put(conn, name);
        metrics.connectionOpened();
        System.out.println("[ws] opened: " + name + " from " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        metrics.messageReceived(message.getBytes(StandardCharsets.UTF_8).length);
        System.out.println("[ws] message from " + clients.get(conn) + ": " + message);
        handleMessage(conn, message);
    }

    /**
     * Single hook for protocol/storage/room logic.
     * P2 (store, rooms), P3 and P4 (protocols) plug in here (task 1.9).
     * For now: broadcast the raw text to everyone, including the sender.
     */
    protected void handleMessage(WebSocket sender, String message) {
        broadcastToAll(message);
    }

    /** Sends to every connected client; a failing client never blocks the others (task 1.4/1.5). */
    public void broadcastToAll(String message) {
        int bytes = message.getBytes(StandardCharsets.UTF_8).length;
        for (WebSocket c : clients.keySet()) {
            safeSend(c, message, bytes);
        }
    }

    /** Sends to one client, wrapped in try/catch so errors are contained. */
    public void safeSend(WebSocket c, String message) {
        safeSend(c, message, message.getBytes(StandardCharsets.UTF_8).length);
    }

    private void safeSend(WebSocket c, String message, int bytes) {
        try {
            if (c.isOpen()) {
                c.send(message);
                metrics.bytesSent(bytes);
            }
        } catch (Exception e) {
            System.out.println("[ws] send failed to " + clients.get(c) + ": " + e.getMessage());
            removeClient(c);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[ws] closed: " + clients.get(conn) + " (code " + code + ")");
        removeClient(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("[ws] error: " + ex);
        if (conn != null) {
            removeClient(conn); // task 1.5: drop the broken client, others keep chatting
        }
    }

    /** Idempotent: close and onError can both fire for the same socket. */
    private void removeClient(WebSocket conn) {
        if (clients.remove(conn) != null) {
            metrics.connectionClosed();
        }
    }

    public Map<WebSocket, String> getClients() {
        return clients;
    }
}
