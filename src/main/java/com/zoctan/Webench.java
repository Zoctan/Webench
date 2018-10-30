package com.zoctan;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.lang.System.out;

public class Webench {

    private int benchTime = 30;
    private int clients = 1;
    private int minThreads = 10;
    private int maxThreads = 200;
    private String ip = "localhost";
    private ExecutorService threadPool = new ThreadPoolExecutor(minThreads, maxThreads,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024), new ThreadPoolExecutor.AbortPolicy());

    public Webench setIP(String ip) {
        this.ip = ip;
        return this;
    }

    public Webench setBenchTime(int benchTime) {
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
            if (!threadPool.awaitTermination(benchTime, SECONDS)) {
                HttpClient.exitSemaphore = true;
            }
            threadPool.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out.printf("Speed: %d pages/sec, %d bytes/sec.\n", (HttpClient.success.get() + HttpClient.failure.get()) / benchTime, HttpClient.bytes.get() / benchTime);
        out.printf("Requests: %d succeed, %d failed.\n", HttpClient.success.get(), HttpClient.failure.get());
    }

}
