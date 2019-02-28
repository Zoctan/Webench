package com.zoctan;

import java.time.Duration;

public class Main {
  public static void main(String[] args) {
    new Webench()
        .setIP("www.baidu.com")
        .setBenchTime(Duration.ofSeconds(30L))
        .setClients(10)
        .setMinThreads(10)
        .setMaxThreads(20)
        .start();
  }
}
