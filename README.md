# Networks-Project 

- Person 1
#### Lujayn Sameh :

# Person 1: Server Core (Network, Transport, Metrics)

## How to run (Linux)
1. `hostname -I` -> note the Wi-Fi **IPv4 address** (e.g. `192.168.1.23`).
2. If the firewall is active (`sudo ufw status`): `sudo ufw allow 8080/tcp` and `sudo ufw allow 8081/tcp`.
3. From the project root (so `client/` is found):
   `mvn org.codehaus.mojo:exec-maven-plugin:3.3.0:java -Dexec.mainClass=server.Main`
4. Phones on the same Wi-Fi open `http://<laptop-ip>:8080`; metrics at `http://<laptop-ip>:8080/metrics.html`.
5. Scalability test (server running, second terminal):
   `mvn org.codehaus.mojo:exec-maven-plugin:3.3.0:java -Dexec.mainClass=server.LoadTest`

## Network / Link Layer
- Server = one laptop; clients = phones on the same Wi-Fi (same subnet, IEEE 802.11 link).
- Clients reach the laptop by its **IPv4 address** (found with `hostname -I`); no DNS needed.
- Server binds to `0.0.0.0`, so it listens on every interface, including Wi-Fi.
- Ports: **8080** (HTTP: client files + `/metrics`), **8081** (WebSocket chat).
- Firewall (`ufw`) must allow inbound TCP 8080 and 8081, otherwise phones time out.
- Verification: a phone browser loading `http://<ip>:8080` proves L2/L3 reachability.

## Transport Layer
- Both services use **TCP** (reliable, ordered byte stream). WebSocket runs over one long-lived TCP connection per client.
- **Thread model:**
  - Java-WebSocket accepts connections on a selector thread and handles each client's frames on a worker pool, so clients are served simultaneously.
  - `com.sun.net.httpserver` uses a fixed pool of 8 threads for HTTP.
  - A daemon `metrics` thread samples messages/sec each second and prints stats every 5 s.
- **Shared state:** `ConcurrentHashMap<WebSocket, String>` of connected clients; all counters are `AtomicInteger`/`AtomicLong`. No explicit locks.
- **Broadcast:** iterate over the map and send to every open connection, each send wrapped in try/catch.
- **Disconnect handling:**
  - `onClose` and `onError` both call an idempotent `removeClient()` (a `remove()` that only decrements the active counter once).
  - A failed send removes that client only; the loop continues for the others.
  - Result: closing one client's browser does not affect the rest.

## Metrics (server monitoring)
- Tracked: active connections, total connections, total messages, messages/second, bytes received/sent, uptime.
- Console line every 5 s; JSON at `/api/metrics`; `metrics.html` polls it every second.

## Performance: Scalability
Latency = round trip from a client sending a message to receiving the server's broadcast of that same message (20 messages per client, 50 ms apart). Clients and server ran on the same laptop (localhost), so there is no Wi-Fi delay in these numbers.

| Clients (N) | Messages sent | Avg latency (ms) | Delivered own echoes |
|---|---|---|---|
| 1 | 20 | 3.37 | 20 |
| 2 | 40 | 22.58 | 40 |
| 5 | 100 | 25.84 | 100 |
| 10 | 200 | 33.04 | 200 |
| 50 | 1000 | 41.66 | 1000 |

- No messages were lost at any N (echoes delivered = messages sent).
- Latency rises with N because each message is broadcast to all N clients (N sends per message).
- Growth is gradual: 10 to 50 clients added about 9 ms.
- Localhost only; real Wi-Fi latency will be higher.

## Integration points (for P2, P3, P4)
- `ChatServer.handleMessage(WebSocket sender, String message)` is the single hook. Replace the default `broadcastToAll(message)` with your protocol parsing, storage and room logic.
- `ChatServer.safeSend(conn, text)` sends to one client safely; `getClients()` exposes the connection map.
- `client/index.html` is a placeholder test client; P4 replaces it.


- Person 2
#### Jana Khaled :

- Person 3
#### Jassen Malak: 

- Person 4 
#### Hala Maged: 

