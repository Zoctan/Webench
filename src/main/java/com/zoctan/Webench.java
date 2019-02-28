package com.zoctan;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 压测
 *
 * @author Zoctan
 */
public class Webench {

  private Duration benchTime = Duration.ofSeconds(30L);
  private int clients = 1;
  private int minThreads = 10;
  private int maxThreads = 200;
  private String ip = "localhost";
  private ExecutorService threadPool =
      new ThreadPoolExecutor(
          minThreads,
          maxThreads,
          0L,
          TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(1024),
          new ThreadPoolExecutor.AbortPolicy());

  public void start() {

    out.printf("%d client, running %d sec.\n", clients, benchTime);
    Runnable[] clientRuns = new HttpClient[clients];
    for (int i = 0; i < clients; i++) {
      clientRuns[i] = new HttpClient(i + "").setIP(ip);
    }
    for (int i = 0; i < clients; i++) {
      threadPool.execute(clientRuns[i]);
    }
    try {
      if (!threadPool.awaitTermination(benchTime.getSeconds(), SECONDS)) {
        HttpClient.exitSemaphore = true;
      }
      threadPool.shutdown();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    out.printf(
        "Speed: %d pages/sec, %d bytes/sec.\n",
        (HttpClient.success.get() + HttpClient.failure.get()) / benchTime.getSeconds(),
        HttpClient.bytes.get() / benchTime.getSeconds());
    out.printf(
        "Requests: %d succeed, %d failed.\n", HttpClient.success.get(), HttpClient.failure.get());
  }

  public Webench setIP(String ip) {
    this.ip = ip;
    return this;
  }

  public Webench setBenchTime(Duration benchTime) {
    this.benchTime = benchTime;
    return this;
  }

  public Webench setClients(int clients) {
    this.clients = clients;
    return this;
  }

  public Webench setMinThreads(int minThreads) {
    this.minThreads = minThreads;
    return this;
  }

  public Webench setMaxThreads(int maxThreads) {
    this.maxThreads = maxThreads;
    return this;
  }
}
