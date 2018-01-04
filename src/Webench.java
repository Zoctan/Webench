import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import static java.lang.System.exit;
import static java.lang.System.out;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Webench {
    private int benchTime = 30;
    private int clients = 1;
    private ExecutorService threadPool = Executors.newFixedThreadPool(35);

    public static void main(String[] args) {
        Webench webench = new Webench();
        webench.benchTime = 3;
        webench.clients = 1;
        webench.start();
    }

    private void start() {
        out.printf("%d client, running %d sec.\n", clients, benchTime);
        Runnable[] clientRuns = new HttpClient[clients];
        for (int i = 0; i < clients; i++) {
            clientRuns[i] = new HttpClient(i + "").setIP("localhost");
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
        out.printf("Speed: %d pages/sec, %d bytes/sec.\n", (HttpClient.success + HttpClient.failure) / benchTime, HttpClient.bytes / benchTime);
        out.printf("Requests: %d succeed, %d failed.\n", HttpClient.success, HttpClient.failure);
    }
}

class HttpClient implements Runnable {
    static boolean isReload = false;
    static boolean isRead = true;
    static int bytes = 0;
    static int success = 0;
    static int failure = 0;
    static boolean exitSemaphore = false;
    private static Lock lock = new ReentrantLock();
    private static String ip;
    private static String method = "GET";
    private static int port = 80;
    private static int connectTimeOut = 3000;
    private static String protocol = "http://";
    private static String path = "/";
    private static String httpVersion = "HTTP/1.1";
    private static String userAgent = "Webench";
    private static String data = "";
    private static String requestHeader = null;
    private static boolean isHTTPS = false;
    private static boolean isShowResponse = false;
    private String name;

    HttpClient(String i) {
        this.name = i;
        //out.println("Creating " + i);
    }

    public void run() {
        //out.println("Running " + name);
        while (!exitSemaphore) {
            if (send()) {
                lock.lock();
                success += 1;
                lock.unlock();
                /* same as above
                synchronized (Client.class) {
                    success += 1;
                }
                */
            } else {
                lock.lock();
                failure += 1;
                lock.unlock();
            }
        }
    }

    private boolean isIP(String ip) {
        Pattern patternIPv4 = Pattern.compile("^(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$");
        Pattern patternIPv6 = Pattern.compile("^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:)|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}(:[0-9A-Fa-f]{1,4}){1,2})|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4}){1,3})|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){1,4})|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){1,5})|([0-9A-Fa-f]{1,4}:(:[0-9A-Fa-f]{1,4}){1,6})|(:(:[0-9A-Fa-f]{1,4}){1,7})|(([0-9A-Fa-f]{1,4}:){6}(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){5}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4})?:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){0,2}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|([0-9A-Fa-f]{1,4}:(:[0-9A-Fa-f]{1,4}){0,4}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(:(:[0-9A-Fa-f]{1,4}){0,5}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}))$");
        return patternIPv4.matcher(ip).matches() || patternIPv6.matcher(ip).matches();
    }

    HttpClient setHTTPS(boolean isHTTPS) {
        HttpClient.isHTTPS = isHTTPS;
        return this;
    }

    HttpClient setShowResponse(boolean isShowResponse) {
        HttpClient.isShowResponse = isShowResponse;
        return this;
    }

    HttpClient setIP(String ip) {
        HttpClient.ip = ip;
        if (!isIP(ip)) {
            try {
                InetAddress i = InetAddress.getByName(ip);
                HttpClient.ip = i.getHostAddress();
                out.println(HttpClient.ip);
            } catch (UnknownHostException e) {
                out.println("Unknown Host");
                e.printStackTrace();
                exit(1);
            }
        }
        return this;
    }

    HttpClient setConnectTimeOut(int connectTimeOut) {
        HttpClient.connectTimeOut = connectTimeOut;
        return this;
    }

    HttpClient setPort(int port) {
        HttpClient.port = port;
        return this;
    }

    HttpClient setMethod(String method) {
        HttpClient.method = method;
        return this;
    }

    HttpClient setProtocol() {
        if (isHTTPS) {
            protocol = "https://";
        }
        return this;
    }

    HttpClient setPath(String path) {
        HttpClient.path = path;
        return this;
    }

    HttpClient setHttpVersion(String httpVersion) {
        HttpClient.httpVersion = httpVersion;
        return this;
    }

    HttpClient setUserAgent(String userAgent) {
        HttpClient.userAgent = userAgent;
        return this;
    }

    HttpClient setData(String data) {
        // a=1&b=2
        // a=1, b=2
        String[] dataArray = data.split("&");
        StringBuilder stringBuilder = new StringBuilder();
        if (method.equals("GET")) {
            stringBuilder.append("?");
        }
        for (int i = 0; i < dataArray.length; i++) {
            // a=1
            String[] tmp = dataArray[i].split("=");
            try {
                stringBuilder
                        .append(URLEncoder.encode(tmp[0], "utf-8"))
                        .append("=")
                        .append(URLEncoder.encode(tmp[1], "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (i != dataArray.length - 1) {
                stringBuilder.append("&");
            }
        }
        HttpClient.data = stringBuilder.toString();
        return this;
    }

    private String getRequestHeader() {
        if (requestHeader == null) {
            StringBuilder stringBuilder = new StringBuilder();
            if (method.equals("GET")) {
                path += data;
            }
            stringBuilder.append(method).append(" ").append(protocol).append(ip).append(":").append(port).append(path).append(" ").append(httpVersion).append("\r\n");
            stringBuilder.append("User-Agent: ").append(userAgent).append("\r\n");
            stringBuilder.append("Host: ").append(ip).append("\r\n");
            if (isReload) {
                stringBuilder.append("Pragma: no-cache\r\n");
            }
            stringBuilder.append("Connection: close\r\n");

            if (method.equals("POST")) {
                stringBuilder.append("Content-Length: ").append(data.length()).append("\r\n");
                stringBuilder.append("Content-Type: application/x-www-form-urlencoded\r\n");
                stringBuilder.append("\r\n");
                stringBuilder.append(data);
            }
            stringBuilder.append("\r\n");
            requestHeader = stringBuilder.toString();
        }
        return requestHeader;
    }

    private void readBytes(Socket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buf = new byte[1500];
            for (int len; -1 != (len = inputStream.read(buf)); ) {
                byteArrayOutputStream.write(buf, 0, len);
                lock.lock();
                bytes += len;
                lock.unlock();
            }
            if (isShowResponse) {
                out.println(byteArrayOutputStream.toString("utf-8"));
            }
            inputStream.close();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean send() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), connectTimeOut);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(getRequestHeader().getBytes());
            outputStream.flush();
            if (isRead) {
                readBytes(socket);
            }
            outputStream.close();
            socket.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}