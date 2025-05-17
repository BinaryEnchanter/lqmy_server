package com.lqmy.server;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.lqmy.server.MainApp.UIUpdater;

public class ConnectionManager {
    //全局使用一个共享实例
    private static final ConnectionManager INSTANCE = new ConnectionManager();

    private ConnectionManager() {
    }

    public static ConnectionManager get() {
        return INSTANCE;
    }

    //这个记录移动端/桌面端所分配的uuid
    private final Map<String, ClientConnection> uuidToConn = new ConcurrentHashMap<>();

    //最大桌面端连接限制
    private static int MAX_DESKTOPS = 100;

    //最大移动端连接限制=5*最大桌面连接限制
    private static int MAX_MOBILES = 5*MAX_DESKTOPS;
    
    //获取最大桌面端连接数
    public static int getMaxDesktop() {
        return MAX_DESKTOPS;
    }
    
    //获取最大移动端连接数
    public static int getMaxMobile() {
        return MAX_MOBILES;
    }
    public static void setMaxDesktop(int value) {
        MAX_DESKTOPS = value;
    }

    public static void setMaxMobile(int value) {
        MAX_MOBILES = value;
    }
    public synchronized boolean add(ClientConnection conn) {
        int desktops = getDesktopCount();
        int mobiles = getMobileCount();

        if (conn.getType() == ClientConnection.Type.DESKTOP && desktops >= MAX_DESKTOPS) {
            return false;
        }
        if (conn.getType() == ClientConnection.Type.MOBILE && mobiles >= MAX_MOBILES) {
            return false;
        } 

        uuidToConn.put(conn.getUuid(), conn);
        return true;
    }

    public synchronized void remove(String uuid) {
        // ClientConnection c = uuidToConn.remove(uuid);
        // if (c != null && c.getPairedUuid() != null) {
        //     // 解除配对
        //     ClientConnection other = uuidToConn.get(c.getPairedUuid());
        //     if (other != null)
        //         other.setPairedUuid(null);
        // }
        uuidToConn.remove(uuid);
        UIUpdater.removeClient(uuid);
    }

    public Optional<ClientConnection> get(String uuid) {
        return Optional.ofNullable(uuidToConn.get(uuid));
    }

    // public void pair(String uuid1, String uuid2) {
    //     ClientConnection c1 = uuidToConn.get(uuid1);
    //     ClientConnection c2 = uuidToConn.get(uuid2);
    //     if (c1 != null && c2 != null) {
    //         c1.setPairedUuid(uuid2);
    //         c2.setPairedUuid(uuid1);
    //         //UIUpdater.updatePairing(uuid1, uuid2);
    //     }
    // }

    public Collection<ClientConnection> allClients() {
        return uuidToConn.values();
    }
    //同步计数当前Desk连接数
    public synchronized int getDesktopCount() {
        return (int) uuidToConn.values().stream()
                .filter(c -> c.getType() == ClientConnection.Type.DESKTOP)
                .count();
    }   
    //同步计数当前Moblie连接数
    public synchronized int getMobileCount() {
        return (int) uuidToConn.values().stream()
                .filter(c -> c.getType() == ClientConnection.Type.MOBILE)
                .count();
    }
}

