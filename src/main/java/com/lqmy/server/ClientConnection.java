package com.lqmy.server;

import org.java_websocket.WebSocket;

public class ClientConnection {
    public enum Type { MOBILE, DESKTOP }

    private final WebSocket socket;
    private final String uuid;
    private final Type type;
    // //对于移动端这是与其配对的桌面端id，对于桌面端，这是与之对应的可以控制桌面端的移动端id
    // private String pairedUuid;
    // //对于桌面端，这是可以看到桌面端画面的移动端id
    // private String[] pairedUuidVec;

    // 最后一次心跳时间（毫秒）
    private volatile long lastHeartbeat;

    public ClientConnection(WebSocket socket, String uuid, Type type) {
        this.socket = socket;
        this.uuid = uuid;
        this.type = type;
    }

    public WebSocket getSocket() { return socket; }
    public String getUuid() { return uuid; }

    public Type getType() {
        return type;
    }

    public long getLastHeartbeat() { return lastHeartbeat; }
    public void refreshHeartbeat() { this.lastHeartbeat = System.currentTimeMillis(); }
    
    // public String getPairedUuid() { return pairedUuid; }
    // public void setPairedUuid(String pairedUuid) { this.pairedUuid = pairedUuid; }
}
