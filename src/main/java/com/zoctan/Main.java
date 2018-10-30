package com.zoctan;

public class Main {
    public static void main(String[] args) {
        new Webench()
                .setIP("www.baidu.com")
                .setBenchTime(10)
                .setClients(10)
                .setMinThreads(10)
                .setMaxThreads(20)
                .start();
    }
}
