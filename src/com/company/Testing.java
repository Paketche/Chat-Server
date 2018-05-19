package com.company;

import java.nio.ByteBuffer;

public class Testing {


    public static void main(String[] args) {

        String someString = new String("Password. the content");
        byte[] somestuff = new byte[10];


        ByteBuffer buffer = ByteBuffer.allocate(1000);
        ByteBuffer longy = ByteBuffer.allocate(Long.BYTES);


        buffer.put(someString.getBytes());
        buffer.position(9);

        System.out.println(new String(buffer.array()));

        System.out.println(new String(longy.putLong(buffer.getLong(5)).array()));
        longy.clear();
        System.out.println(new String(longy.putLong(buffer.getInt(1)).array()));
        longy.clear();

        buffer.reset();
        buffer.get(somestuff);
        System.out.println(new String(somestuff));


        System.out.println(new String(longy.putLong(buffer.getLong(5)).array()));
        longy.clear();
        System.out.println(new String(longy.putLong(buffer.getInt(1)).array()));
        longy.clear();

        buffer.reset();
        buffer.get(somestuff);
        System.out.println(new String(somestuff));
    }
}

