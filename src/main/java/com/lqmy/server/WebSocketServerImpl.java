package com.lqmy.server;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lqmy.server.MainApp.UIUpdater;

public class WebSocketServerImpl extends WebSocketServer {
    private final Gson gson = new Gson();
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    // 心跳超时时间（毫秒）
    private static final long HEARTBEAT_TIMEOUT = 60_000;
    // 检查间隔（毫秒）
    private static final long CHECK_INTERVAL = 30_000;

    public WebSocketServerImpl(int port) throws Exception {
        super(new InetSocketAddress("0.0.0.0", port));
        // // --- SSL/TLS Setup ---
        // // 1. 加载 keystore（PKCS12）
        // String keystorePassword = "changeit";
        // KeyStore ks = KeyStore.getInstance("PKCS12");
        // try (InputStream is = getClass().getResourceAsStream("/signaling-server.p12")) {
        //     if (is == null) {
        //         throw new FileNotFoundException("Certificate not found in classpath!");
        //     }
        //     ks.load(is, keystorePassword.toCharArray());
        // }

        // // 2. 初始化 KeyManager
        // KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        // kmf.init(ks, keystorePassword.toCharArray());
        // // 3. 初始化 SSLContext
        // SSLContext sslContext = SSLContext.getInstance("TLS");
        // sslContext.init(kmf.getKeyManagers(), null, null);
        // // 4. 设置 WebSocketFactory 为 SSL
        // setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("客户端连接：" + conn.getRemoteSocketAddress());
        // 等待客户端发送 register 消息
    }

    @Override
    public void onMessage(WebSocket conn, String msg) {
        JsonObject o = gson.fromJson(msg, JsonObject.class);
        System.out.println(o);
        String type = o.get("type").getAsString();
        switch (type) {
            case "register" -> {
                /*
                 * R{"type":"register","client_type":"desktop"/"moblie"}
                 * A1{"type":"register_ack","uuid":"uuid"}
                 * A2{"type":"register_reject","reason":"connection limit exceeded"}
                 */
                // 根据json信息建立ClinetConnection结构体
                try {
                    String clientType = o.get("client_type").getAsString();
                    String uuid = generateShortUUID(10);
                    ClientConnection.Type t = "mobile".equalsIgnoreCase(clientType)
                            ? ClientConnection.Type.MOBILE
                            : ClientConnection.Type.DESKTOP;
                    ClientConnection cc = new ClientConnection(conn, uuid, t);
                    // 使用同步函数检测是否可以连接，连接成功，返回true，反之。
                    boolean accepted = ConnectionManager.get().add(cc);
                    // 根据连接成功与否设置json
                    JsonObject ack = new JsonObject();
                    ack.addProperty("type", accepted ? "register_ack" : "register_reject");
                    if (accepted) {
                        ack.addProperty("uuid", uuid);
                        conn.send(gson.toJson(ack));
                        UIUpdater.addClient(cc);// 在UI更新
                        System.out.println("连接成功：" + clientType + conn.getRemoteSocketAddress());

                    } else {
                        ack.addProperty("reason", "connection limit exceeded");
                        conn.send(gson.toJson(ack));
                        conn.close(); // 主动断开连接
                        System.out.println("连接失败：" + clientType + conn.getRemoteSocketAddress());
                    }
                } catch (Exception e) {
                    // 注册中出现错误就返回错误，断开连接
                    System.out.println(e);
                    JsonObject ack = new JsonObject();
                    ack.addProperty("type", "register_fail");
                    ack.addProperty("reason", "Server Internal Error");
                    conn.send(gson.toJson(ack));
                    conn.close(); // 主动断开连接
                }
            }
            // case "pair_request" -> {
            // String from = o.get("from").getAsString();
            // String to = o.get("target_uuid").getAsString();
            // ConnectionManager.get().pair(from, to);
            // }
            case "message" -> {
                /*
                 * R{"type":"message","target_uuid":"...","from":"...","payload":"..."}
                 * A{"type":"message","from":"...","payload":"..."}
                 */
                try {
                    String target = o.get("target_uuid").getAsString();
                    // 根据消息发往地设置

                    String payload = o.get("payload").getAsString();
                    ConnectionManager.get().get(target)
                            .ifPresent(cc -> {
                                JsonObject fwd = new JsonObject();
                                fwd.addProperty("type", "message");
                                fwd.addProperty("from", o.get("from").getAsString());
                                fwd.addProperty("payload", payload);
                                cc.getSocket().send(gson.toJson(fwd));
                            });
                } catch (Exception e) {
                    // 消息发送失败，告知发送方
                    System.out.println(e);
                    JsonObject ack = new JsonObject();
                    ack.addProperty("type", "message_fail");
                    ack.addProperty("reason", "Server Internal Error");
                    conn.send(gson.toJson(ack));
                }
            }
            case "ping" -> {
                /*
                 * R{"type":"ping","from":"..."}
                 * A{"type":"pong"}
                 */
                // 找到对应 ClientConnection 并刷新心跳
                try {
                    String from = o.get("from").getAsString();
                    ConnectionManager.get().get(from)
                            .ifPresent(ClientConnection::refreshHeartbeat);
                    // 回复 pong
                    JsonObject pong = new JsonObject();
                    pong.addProperty("type", "pong");
                    conn.send(gson.toJson(pong));
                    System.out.println("ping:来自"+from);
                } catch (Exception e) {
                    // 消息发送失败，告知发送方
                    System.out.println(e);
                    JsonObject ack = new JsonObject();
                    ack.addProperty("type", "ping_fail");
                    ack.addProperty("reason", "Server Internal Error");
                    conn.send(gson.toJson(ack));
                }
            }
            case "close" -> {
                /*
                 * {"type":"close"}
                 */
                try {
                    //String from = o.get("from").getAsString();
                    conn.close();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
            default -> {
               
            }
        }
        // ignore
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("客户端断开：" + conn.getRemoteSocketAddress() +
                " (remote=" + remote + ", code=" + code + ", reason=" + reason + ")");
        ConnectionManager.get().allClients().stream()
                .filter(c -> c.getSocket() == conn)
                .findFirst()
                .ifPresent(c -> {
                    ConnectionManager.get().remove(c.getUuid());
                    UIUpdater.removeClient(c.getUuid());
                });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Secure WebSocket server started on port " + getPort());
        // 启动心跳检查线程
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            ConnectionManager.get().allClients().forEach(c -> {
                if (now - c.getLastHeartbeat() == HEARTBEAT_TIMEOUT) {
                    System.out.println("心跳超时，关闭连接：" + c.getUuid());
                    c.getSocket().close(); // 会触发 onClose，自动移除

                }
            });
        }, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public static String generateShortUUID(int length) {
        UUID uuid = UUID.randomUUID();
        BigInteger bigInt = new BigInteger(uuid.toString().replace("-", ""), 16);
        StringBuilder sb = new StringBuilder();

        while (bigInt.compareTo(BigInteger.ZERO) > 0 && sb.length() < length) {
            int rem = bigInt.mod(BigInteger.valueOf(62)).intValue();
            sb.append(BASE62.charAt(rem));
            bigInt = bigInt.divide(BigInteger.valueOf(62));
        }

        return sb.toString();
    }
}
