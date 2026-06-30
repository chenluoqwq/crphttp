package http;
import javax.swing.*;
import java.awt.*;

public class ServerFrame extends JFrame {
    private JTextArea logArea;   // 日志文本框
    private JButton startBtn;    // 启动按钮
    private JButton stopBtn;     // 停止按钮
    private HttpServerCore server; // 核心服务器对象

    public ServerFrame() {
        setTitle("简易 HTTP 服务器控制台");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initView(); // 初始化界面布局

        // 启动核心服务器对象
        server = new HttpServerCore();
        // 将 GUI 的日志打印方法传给服务器，当服务器有日志时，会自动刷新到界面上
        server.setLogCallback(msg -> SwingUtilities.invokeLater(() -> appendLog(msg)));

        System.out.println("当前工作目录: " + System.getProperty("user.dir"));
    }

    private void initView() {
        // 顶部面板：启动和停止按钮
        JPanel topPanel = new JPanel();
        startBtn = new JButton("启动服务器");
        stopBtn = new JButton("停止服务器");
        stopBtn.setEnabled(false); // 初始状态，停止按钮不可用

        topPanel.add(startBtn);
        topPanel.add(stopBtn);
        add(topPanel, BorderLayout.NORTH);

        // 中部面板：日志显示区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // 绑定按钮点击事件
        startBtn.addActionListener(e -> {
            server.start();
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
        });

        stopBtn.addActionListener(e -> {
            server.stop();
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        });
    }

    // 线程安全地在 JTextArea 追加文本并自动滚动到底部
    private void appendLog(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServerFrame().setVisible(true);
        });
    }
}