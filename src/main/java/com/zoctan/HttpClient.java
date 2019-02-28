package com.zoctan;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static java.lang.System.*;

/**
 * Http客户端
 *
 * @author Zoctan
 */
public class HttpClient implements Runnable {
  // 响应不被缓存
  static boolean isReload = false;
  static boolean isRead = true;
  static AtomicLong bytes = new AtomicLong(0);
  static AtomicInteger success = new AtomicInteger(0);
  static AtomicInteger failure = new AtomicInteger(0);
  static boolean exitSemaphore = false;
  private static String ip;
  private static String method = "GET";
  private static int port = 80;
  private static int connectTimeout = 3000;
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
    // out.println("Creating " + i);
  }

  @Override
  public void run() {
    while (!exitSemaphore) {
      if (send()) {
        success.incrementAndGet();
        out.println("Running:" + name + " success:" + success.get());
      } else {
        failure.incrementAndGet();
        // out.println("Running:" + name + " failure:" + failure.get());
      }
    }
  }

  private Pattern patternIPv4 =
      Pattern.compile(
          "^(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$");
  private Pattern patternIPv6 =
      Pattern.compile(
          "^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:)|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}(:[0-9A-Fa-f]{1,4}){1,2})|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4}){1,3})|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){1,4})|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){1,5})|([0-9A-Fa-f]{1,4}:(:[0-9A-Fa-f]{1,4}){1,6})|(:(:[0-9A-Fa-f]{1,4}){1,7})|(([0-9A-Fa-f]{1,4}:){6}(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){5}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4})?:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){0,2}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|([0-9A-Fa-f]{1,4}:(:[0-9A-Fa-f]{1,4}){0,4}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(:(:[0-9A-Fa-f]{1,4}){0,5}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}))$");

  private boolean isIP(String ip) {
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
        // out.println(HttpClient.ip);
      } catch (UnknownHostException e) {
        err.println("Unknown Host");
        e.printStackTrace();
        exit(1);
      }
    }
    return this;
  }

  HttpClient setConnectTimeout(int connectTimeout) {
    HttpClient.connectTimeout = connectTimeout;
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
    if ("GET".equals(method)) {
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
      if ("GET".equals(method)) {
        path += data;
      }
      stringBuilder
          .append(method)
          .append(" ")
          .append(protocol)
          .append(ip)
          .append(":")
          .append(port)
          .append(path)
          .append(" ")
          .append(httpVersion)
          .append("\r\n");
      stringBuilder.append("User-Agent: ").append(userAgent).append("\r\n");
      stringBuilder.append("Host: ").append(ip).append("\r\n");
      if (isReload) {
        stringBuilder.append("Pragma: no-cache\r\n");
      }
      stringBuilder.append("Connection: close\r\n");

      if ("POST".equals(method)) {
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
        bytes.addAndGet(len);
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
      socket.connect(new InetSocketAddress(ip, port), connectTimeout);
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
