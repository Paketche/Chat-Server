package com.company;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {

        Thread.currentThread().setName("Driver thread");

        try {
            SimpleMessage m = new SimpleMessage();
            ReaderFactory rf = new ReaderFactory("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/messages", "root", "SNTmgl134", m);
            WriterFactory wf = new WriterFactory();
            ChatServer cs = new ChatServer("localhost", 8080, rf, wf);
            cs.setCrashLogFile("/logs/ChatServer.txt", "yyyy.MM.dd G 'at' HH:mm:ss z");

            cs.start();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }
}
