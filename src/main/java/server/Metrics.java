package server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe server statistics (task 1.7).
 * All counters are atomics, so any connection thread can update them
 * without locking.
 */
public class Metrics {
    private final AtomicInteger activeConnections = new AtomicInteger();
    private final AtomicLong totalConnections = new AtomicLong();
    private final AtomicLong totalMessages = new AtomicLong();
    private final AtomicLong bytesReceived = new AtomicLong();
    private final AtomicLong bytesSent = new AtomicLong();

    private volatile double messagesPerSecond = 0;
    private long lastTotal = 0;
    private final long startTime = System.currentTimeMillis();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "metrics");
                t.setDaemon(true);
                return t;
            });

    /** Starts the 1 s rate sampler and the 5 s console printer. */
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = totalMessages.get();
            messagesPerSecond = now - lastTotal;
            lastTotal = now;
        }, 1, 1, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(
                () -> System.out.println("[metrics] " + toConsoleString()),
                5, 5, TimeUnit.SECONDS);
    }

    public void connectionOpened() {
        activeConnections.incrementAndGet();
        totalConnections.incrementAndGet();
    }

    public void connectionClosed() {
        activeConnections.decrementAndGet();
    }

    public void messageReceived(int bytes) {
        totalMessages.incrementAndGet();
        bytesReceived.addAndGet(bytes);
    }

    public void bytesSent(int bytes) {
        bytesSent.addAndGet(bytes);
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public String toConsoleString() {
        return String.format("active=%d total_msgs=%d msgs/s=%.0f bytes_in=%d bytes_out=%d",
                activeConnections.get(), totalMessages.get(), messagesPerSecond,
                bytesReceived.get(), bytesSent.get());
    }

    /** JSON served at /metrics (task 1.8). Hand-built to avoid a dependency. */
    public String toJson() {
        return "{"
                + "\"activeConnections\":" + activeConnections.get() + ","
                + "\"totalConnections\":" + totalConnections.get() + ","
                + "\"totalMessages\":" + totalMessages.get() + ","
                + "\"messagesPerSecond\":" + messagesPerSecond + ","
                + "\"bytesReceived\":" + bytesReceived.get() + ","
                + "\"bytesSent\":" + bytesSent.get() + ","
                + "\"uptimeSeconds\":" + (System.currentTimeMillis() - startTime) / 1000
                + "}";
    }
}
