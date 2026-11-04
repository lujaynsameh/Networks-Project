package server;

import java.net.InetAddress;
import java.nio.file.Path;

/** Entry point: starts metrics, WebSocket server (8081) and HTTP server (8080). */
public class Main {
    public static final int HTTP_PORT = 8080;
    public static final int WS_PORT = 8081;

    public static void main(String[] args) throws Exception {
        Metrics metrics = new Metrics();
        metrics.start();

        ChatServer chat = new ChatServer(WS_PORT, metrics);
        chat.start();

        WebFileServer web = new WebFileServer(HTTP_PORT, Path.of("client"), metrics);
        web.start();

        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Open on phones: http://" + ip + ":" + HTTP_PORT
                + "  (if wrong, use the Wi-Fi IPv4 from ipconfig)");
        System.out.println("Metrics page:   http://" + ip + ":" + HTTP_PORT + "/metrics.html");
    }
}
