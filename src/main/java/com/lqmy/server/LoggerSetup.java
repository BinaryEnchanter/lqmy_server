package com.lqmy.server;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class LoggerSetup {
    public static void initialize() {
        Logger rootLogger = Logger.getLogger("");

        // 删除默认控制台处理器
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler h : handlers) {
            rootLogger.removeHandler(h);
        }
         // 生成带时间戳的文件名
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String logFileName = "logs/app-" + timestamp + ".log";

        try {
            // 日志文件路径（与程序运行目录相关）
            FileHandler fileHandler = new FileHandler(logFileName, true); // true = 追加写入
            fileHandler.setFormatter(new SimpleFormatter());

            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.INFO);

        } catch (IOException e) {
            System.err.println("无法创建日志文件：" + e.getMessage());
        }
    }
}
