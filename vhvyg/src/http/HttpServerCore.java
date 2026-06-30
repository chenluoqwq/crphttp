package http;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HttpServerCore {
    private static final int PORT = 8080;//服务器监听端口
    private static final String WEB_ROOT = "./web";//网站文件的存放目录，，服务器从这个目录处读取文件
    private final Path webRootPath;   // 规范化后的绝对路径，用于安全检查

    private ServerSocket serverSocket;//设置本地服务器
    private ExecutorService threadPool;//声明线程池
    private volatile boolean isRunning = false;//插入内存屏障，直接跨过缓存直接修改主存，同时防止编译器对擅自修改序列
    //此时还未启动该变量；
    // 初始化时解析 WEB_ROOT 为绝对路径，避免每次请求都做 IO
    public HttpServerCore() {

        try {
            webRootPath = Paths.get(WEB_ROOT).toRealPath();//Paths.get进行数据类型的转化，toRealPath获得真正的路径 抛出Exception
        } catch (IOException e) {
            throw new RuntimeException("无法解析 WEB_ROOT 路径: " + WEB_ROOT, e);
        }
    }

    // 启动服务器
    public void start() {
        if (isRunning) return;
        isRunning = true;//插入内存屏障了

        // 使用有界线程池，防止资源耗尽
        threadPool = Executors.newFixedThreadPool(20);//创建固定线程，防止线程太多

        // 监听线程设置为守护线程，使 JVM 可正常退出
        Thread acceptThread = new Thread(() -> {    //设置独立线程
            try {
                serverSocket = new ServerSocket(PORT);//本地服务器连接已设置的端口，“线人的联络点”
                log("服务器启动成功，监听端口: " + PORT);
                while (isRunning) {//服务器运行中
                    try {
                        Socket socket = serverSocket.accept();//接收器socket 等待接受本地服务器accept（）收到的信号，看成线人
                        threadPool.execute(() -> handleClient(socket));//将括号中的任务放入任务列中  () -> handleClient(socket)对runnable的匿名类
                        //handleClient处理客户端请求的函数（客户的信号）


                    } catch (IOException e) {//检查输入输出异常
                        if (!isRunning) break;//用户主动断开
                        log("服务器连接异常: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                log("服务器启动失败: " + e.getMessage());
            }
        });
        acceptThread.setDaemon(true);//把正在handleClient的线程设置为守护线程，只保留主线程
        acceptThread.start();
    }

    // 停止服务器
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {//检查联络点的状态是否已关闭
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdown();//线程完成任务后关闭
                // 等待已提交任务完成（最多等待3秒）
                if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {//最多只等3秒
                    threadPool.shutdownNow();//立即结束
                }
            }
            log("服务器已安全关闭。");
        } catch (IOException | InterruptedException e) {
            log("关闭服务器异常: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    // ============ 核心业务逻辑 ============

    private void handleClient(Socket socket) {
        try (InputStream inputStream = socket.getInputStream();  //把输入流接到信号上
             OutputStream outputStream = socket.getOutputStream();//把输出流接到信号上
             BufferedReader reader = new BufferedReader(//安排我的翻译官
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {//把UTF_8的字节流翻译成字符流

            // 读取请求行
            String requestLine = reader.readLine();//等待发来的文字，等对方说话
            if (requestLine == null || requestLine.isEmpty()) return;

            log("收到请求: " + requestLine);
//对方说话了之后
            //翻译官来看看怎么个事

            String[] parts = requestLine.split(" ");//http协议，会有三部分，动作，地址，http版本，这一步来把对方的话切成三份
            if (parts.length < 3) {
                sendError(outputStream, 400, "Bad Request");
                return;
            }//暗号连字数都不对

            String method = parts[0];
            String uri = parts[1];
            String httpVersion = parts[2];   // 记录客户端 HTTP 版本
//翻译官把对方说的信息记下来
// 暗号对接完毕，接收信息
            // 读取并消费所有请求头0
            int contentLength = 0;//先假设长度为0
            String line;
            //先来看看有多长
            while ((line = reader.readLine()) != null && !line.isEmpty()) {//阅读，知道空行或结束
                if (line.toLowerCase().startsWith("content-length:")) {//先把那内容全部换成小写，不然我看不习惯，如果是内容长度打头
                    try {
                        contentLength = Integer.parseInt(//类型转化，换成整数类型
                                line.substring("content-length:".length()).trim());//把字段处截断，再把空格删了
                    } catch (NumberFormatException ignored) {}//抓住字符串翻译的异常
                }
            }

            // 如果有请求体（POST / PUT），将其读走，防止影响后续连接
            if (contentLength > 0) {//翻译：好吧，真有活了
                char[] body = new char[contentLength];//用点脑子把这个记住
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int read = reader.read(body, totalRead, contentLength - totalRead);//开始阅读：我脑子存放的位置，读了多少了，还有多少要读
                    if (read == -1) break;//不让读了
                    totalRead += read;
                }
                // 此处可获取请求体字符串: String requestBody = new String(body);
                // 目前仅消费掉，不做处理
            }

            // 路由分发
            //前面把暗号拆解成了三段，这是处理第一段，动作。忽略大小写
            if ("GET".equalsIgnoreCase(method)) {//线人：我来拿情报
                handleGet(uri, outputStream, httpVersion);//从这个流把这个版本的这个uri给它
            } else if ("POST".equalsIgnoreCase(method)) {//线人：我来给情报
                handlePost(outputStream, httpVersion);//      在这准备好接收了      String uri = parts[1];
            } else {
                sendError(outputStream, 405, "Method Not Allowed", httpVersion);//线人：什么都不干。你一边玩去吧
            }

        } catch (IOException e) {
            log("客户端连接异常: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}//线人离开
        }
    }

    // 处理 GET 请求（增加路径遍历安全检查和 HTTP 版本）
    private void handleGet(String uri, OutputStream outputStream, String httpVersion) throws IOException {
        // 默认首页
        if (uri.equals("/")) {//如果延保线人给的地址：总部，但没细琐是哪
            uri = "/index.html";//别拉到了，给你领到这得了
        }

        // 安全解析文件路径，防止路径遍历攻击
        // 先去掉首字符 '/'，然后拼接到根路径，再规范化
        Path requestedPath;
        try {
            // URI 解码（简化处理，只处理最常见情况）
            String decodedUri = java.net.URLDecoder.decode(uri, StandardCharsets.UTF_8);//把地址用UTF_8翻译一下
            // 去掉开头的 '/'
            String relativePath = decodedUri.startsWith("/") ? decodedUri.substring(1) : decodedUri;//记录真正的地址，检查字符串是否以 "/" 开头
            // 如果以 "/" 开头，就从索引 1 开始截取，相当于去掉开头的斜杠    如果不以 "/" 开头，直接使用原字符串
            requestedPath = webRootPath.resolve(relativePath).normalize();//将地址拼接到webRootPath 后面后后规范化
        } catch (Exception e) {
            sendError(outputStream, 400, "Bad Request", httpVersion);//说的话有问题啊翻译不出来
            return;
        }

        // 验证路径是否仍位于 WEB_ROOT 下
        if (!requestedPath.startsWith(webRootPath)) {//看看我拼接成功了吗
            sendError(outputStream, 403, "Forbidden", httpVersion);
            return;
        }

        File file = requestedPath.toFile();//转化成文件类型//把他盖个章
        if (file.exists() && file.isFile() && file.canRead()) {//文件没问题吧
            // 检查文件大小，防止内存溢出（这里限制最大 10MB 直接读取，更大应使用流）
            long fileSize = file.length();//看看文件多大
            if (fileSize > 10 * 1024 * 1024) {
                // 对于大文件，可选择返回 413 或改为流式传输，这里简单返回 413
                sendError(outputStream, 413, "Payload Too Large", httpVersion);
                return;//太大了，不考虑了，防止我的JVM炸了
            }
            byte[] fileContent = Files.readAllBytes(requestedPath);//调用类Files 把这个路径的文件放在这里
            String mimeType = getMimeType(uri);//得到这个文件的类型，照片啊还是视频呀

            String dateHeader = "Date: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            //http协议规定的Date头     固定输出形如 Tue, 29 Jun 2026 10:15:30 GMT 的格式。          取当前 UTC 时间
            String responseHeader = httpVersion + " 200 OK\r\n" +
                    dateHeader +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            outputStream.write(responseHeader.getBytes(StandardCharsets.UTF_8));
            outputStream.write(fileContent);
            //输出内容·
        } else {
            sendError(outputStream, 404, "Not Found", httpVersion);
        }
        outputStream.flush();
    }

    // 处理 POST 请求（实际应用中应接收 body 数据）
    private void handlePost(OutputStream outputStream, String httpVersion) throws IOException {
        String responseBody = "<html><body><h1>POST 请求已收到</h1>" +
                "<p>服务器成功处理了您的 POST 请求。</p></body></html>";
        String dateHeader = "Date: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()) + "\r\n";
        String responseHeader = httpVersion + " 200 OK\r\n" +
                dateHeader +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + responseBody.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        outputStream.write(responseHeader.getBytes(StandardCharsets.UTF_8));
        outputStream.write(responseBody.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();//强制清空缓冲区，把所有积压的字节立刻写出到客户端
    }

    // 发送错误响应（增加 HTTP 版本参数）
    private void sendError(OutputStream outputStream, int statusCode, String statusMessage, String httpVersion) throws IOException {
        String body = "<html><body><h1>" + statusCode + " " + statusMessage + "</h1></body></html>";
        String dateHeader = "Date: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()) + "\r\n";
        String header = httpVersion + " " + statusCode + " " + statusMessage + "\r\n" +
                dateHeader +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        outputStream.write(header.getBytes(StandardCharsets.UTF_8));
        outputStream.write(body.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    // 保留原有的两参数重载（兼容性），内部调用新版本并默认 HTTP/1.1
    private void sendError(OutputStream outputStream, int statusCode, String statusMessage) throws IOException {
        sendError(outputStream, statusCode, statusMessage, "HTTP/1.1");
    }

    // MIME 类型识别（可扩展为从文件扩展名映射表读取）
    private String getMimeType(String uri) {
        if (uri.endsWith(".html") || uri.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (uri.endsWith(".css")) return "text/css";
        if (uri.endsWith(".js")) return "application/javascript";
        if (uri.endsWith(".png")) return "image/png";
        if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
        if (uri.endsWith(".svg")) return "image/svg+xml";
        return "text/plain; charset=UTF-8";
    }

    // ============ 日志回调接口 ============

    public interface LogCallback {
        void onLog(String message);
    }//定义接收窗口

    private LogCallback logCallback;

    public void setLogCallback(LogCallback callback) {
        this.logCallback = callback;
    }

    private void log(String msg) {
        System.out.println(msg);
        if (logCallback != null) {
            logCallback.onLog(msg);
        }
    }
}