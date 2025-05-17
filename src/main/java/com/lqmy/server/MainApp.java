package com.lqmy.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    private final ObservableList<ClientConnection> mobiles = FXCollections.observableArrayList();
    private final ObservableList<ClientConnection> desktops = FXCollections.observableArrayList();
    private final Pane linePane = new Pane();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        UIUpdater.initialize(this);

        VBox leftBox = new VBox(5, new Label("📱 Mobiles"), listViewOf(mobiles));
        VBox rightBox = new VBox(5, new Label("💻 Desktops"), listViewOf(desktops));
        leftBox.setPrefWidth(200);
        rightBox.setPrefWidth(200);

        HBox root = new HBox(10, leftBox, linePane, rightBox);
        root.setAlignment(Pos.CENTER);

        primaryStage.setTitle("WebSocket Signaling Server");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();

        // 全局变量持有 server
        WebSocketServerImpl[] serverHolder = new WebSocketServerImpl[1];

        // 窗口关闭时彻底退出程序
        primaryStage.setOnCloseRequest(event -> {
            try {
                if (serverHolder[0] != null) {
                    serverHolder[0].stop();
                    System.out.println("WebSocketServer 已关闭");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Platform.exit();
                System.exit(0); // 强制 JVM 退出
            }
        });

        // 启动 WebSocket Server，保存引用
        new Thread(() -> {
            try {
                WebSocketServerImpl server = new WebSocketServerImpl(9876);
                serverHolder[0] = server;
                server.start(); // 阻塞运行
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // @Override
    // public void start(Stage primaryStage) {
    // UIUpdater.initialize(this);

    // VBox leftBox = new VBox(5, new Label("📱 Mobiles"), listViewOf(mobiles));
    // VBox rightBox = new VBox(5, new Label("💻 Desktops"), listViewOf(desktops));
    // leftBox.setPrefWidth(200);
    // rightBox.setPrefWidth(200);

    // HBox root = new HBox(10, leftBox, linePane, rightBox);
    // root.setAlignment(Pos.CENTER);

    // primaryStage.setTitle("WebSocket Signaling Server");
    // primaryStage.setScene(new Scene(root, 600, 400));
    // primaryStage.show();

    // new Thread(() -> {
    // try {
    // WebSocketServerImpl server = new WebSocketServerImpl(9876);
    // Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // try {
    // server.stop();
    // System.out.println("WebSocketServer 已关闭");
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }));
    // server.start(); // 默认就是 WSS 了
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }).start();
    // }

    private ListView<ClientConnection> listViewOf(ObservableList<ClientConnection> list) {
        ListView<ClientConnection> lv = new ListView<>(list);
        lv.setCellFactory(tv -> new ListCell<>() {
            @Override
            protected void updateItem(ClientConnection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUuid());
            }
        });
        return lv;
    }

    /** UIUpdater 用于在非 JavaFX 线程触发时更新界面 */
    static class UIUpdater {
        private static MainApp app;

        static void initialize(MainApp a) {
            app = a;
        }

        // static MainApp getApp() { return app; }

        static void addClient(ClientConnection c) {
            Platform.runLater(() -> {
                if (c.getType() == ClientConnection.Type.MOBILE) {
                    app.mobiles.add(c);
                } else {
                    app.desktops.add(c);
                }
            });
        }

        static void removeClient(String uuid) {
            Platform.runLater(() -> {
                app.mobiles.removeIf(c -> c.getUuid().equals(uuid));
                app.desktops.removeIf(c -> c.getUuid().equals(uuid));
                // 同时也移除线
                // app.linePane.getChildren().removeIf(n -> n.getUserData() != null
                // && uuid.equals(n.getUserData()));
            });
        }

        // static void updatePairing(String u1, String u2) {
        // Platform.runLater(() -> {
        // // 找到两个节点在界面的位置
        // Node n1 = findNode(u1, app.mobiles, app.linePane, true);
        // Node n2 = findNode(u2, app.desktops, app.linePane, true);
        // if (n1 != null && n2 != null) {
        // // 画条线并存储 userData 以便删除
        // Line line = new Line(
        // n1.getLayoutX() + 100, n1.getLayoutY() + 10,
        // n2.getLayoutX() + 10, n2.getLayoutY() + 10
        // );
        // line.setStroke(Color.BLUE);
        // line.setUserData(u1 + "|" + u2);
        // app.linePane.getChildren().add(line);
        // }
        // });
        // }

        // // 辅助：按 uuid 在列表中找到对应 ListCell 的坐标
        // private static Node findNode(String uuid, ObservableList<ClientConnection>
        // list,
        // Pane linePane, boolean isMobile) {
        // // 简化：这里假设列表是固定高度单元格，逐一计算
        // int idx = 0;
        // for (ClientConnection c : list) {
        // if (c.getUuid().equals(uuid)) break;
        // idx++;
        // }
        // double y = 30 + idx * 24; // 30px 顶部偏移，24px 行高
        // double x = isMobile ? 200 : 0; // 左侧/右侧
        // Label dummy = new Label(); dummy.setLayoutX(x); dummy.setLayoutY(y);
        // return dummy;
        // }
    }
}
