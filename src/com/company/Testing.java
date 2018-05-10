package com.company;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Testing {


    public static void main(String[] args) {
        ExecutorService es = Executors.newFixedThreadPool(3);
        es.execute(()-> System.out.println("new execution"));
    }
}

