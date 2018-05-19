package com.company;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {


        try {
            SimpleMessage m = new SimpleMessage();
            ReaderFactory rf = new ReaderFactory("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/messages", "root", "SNTmgl134", m);
            WriterFactory wf = new WriterFactory();
            ChatServer cs = new ChatServer("localhost", 8085, rf, wf);

            cs.start();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }
}
