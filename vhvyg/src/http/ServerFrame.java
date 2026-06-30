package http;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ServerFrame extends JFrame {
    private JTextArea logArea;
    private JButton startBtn;
    private JButton stopBtn;
    private HttpServerCore server;

    public ServerFrame() {
        // 窗口基础配置
        setTitle("简易HTTP文件服务器控制台");
        setSize(650, 450);
        setMinimumSize(new Dimension(600, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 窗口居中
        setResizable(true);

        // 全局美化：统一字体
        UIManager.put("Button.font", new Font("微软雅黑", Font.PLAIN, 15));
        UIManager.put("TextArea.font", new Font("Consolas", Font.PLAIN, 14));

        initView();

        // 绑定核心服务与日志回调
        server = new HttpServerCore();
        server.setLogCallback(msg -> SwingUtilities.invokeLater(() -> appendLog(msg)));
        System.out.println("当前工作目录: " + System.getProperty("user.dir"));
    }

    private void initView() {
        // 主面板，增加边距
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);
        getContentPane().add(mainPanel);

        // ---------------------- 顶部按钮面板 ----------------------
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 5));
        topPanel.setBackground(Color.WHITE);

        // 美化【启动按钮】
        startBtn = new JButton("▶ 启动服务器");
        startBtn.setPreferredSize(new Dimension(160, 42));
        startBtn.setBackground(new Color(40, 160, 60));
        startBtn.setForeground(Color.WHITE);
        startBtn.setFocusPainted(false);
        startBtn.setBorderPainted(false);
        startBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 美化【停止按钮】
        stopBtn = new JButton("■ 停止服务器");
        stopBtn.setPreferredSize(new Dimension(160, 42));
        stopBtn.setBackground(new Color(200, 60, 60));
        stopBtn.setForeground(Color.WHITE);
        stopBtn.setFocusPainted(false);
        stopBtn.setBorderPainted(false);
        stopBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        stopBtn.setEnabled(false);

        topPanel.add(startBtn);
        topPanel.add(stopBtn);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // ---------------------- 中间日志区域 ----------------------
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(248, 248, 248));
        logArea.setForeground(new Color(20, 20, 20));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // ---------------------- 按钮事件 ----------------------
        startBtn.addActionListener(e -> {
            server.start();
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            startBtn.setBackground(new Color(100, 180, 110));
        });

        stopBtn.addActionListener(e -> {
            server.stop();
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            startBtn.setBackground(new Color(40, 160, 60));
        });
    }

    // 日志自动滚动
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