package http;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class ServerFrame extends JFrame {
    private JTextPane logPane;
    private ModernButton startBtn;
    private ModernButton stopBtn;
    private HttpServerCore server;
    private JLabel statusLabel;
    private JLabel requestCountLabel;
    private JLabel upTimeLabel;
    private StatusIndicator statusIndicator;
    private long startTime = 0;

    public ServerFrame() {
        setTitle("高性能HTTP文件服务器");
        setSize(900, 650);
        setMinimumSize(new Dimension(800, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setUndecorated(false);

        // 设置现代字体
        setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));

        initView();

        // 绑定核心服务与日志回调
        server = new HttpServerCore();
        server.setLogCallback(msg -> SwingUtilities.invokeLater(() -> appendLog(msg)));
        
        // 更新状态的定时器
        Timer updateTimer = new Timer(1000, e -> updateStatus());
        updateTimer.start();
        
        System.out.println("当前工作目录: " + System.getProperty("user.dir"));
    }

    private void initView() {
        // 创建带渐变的主面板
        GradientPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        getContentPane().add(mainPanel);

        // ==================== 顶部标题栏 ====================
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("🚀 HTTP 文件服务器");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        titleLabel.setForeground(new Color(255, 255, 255));
        
        statusIndicator = new StatusIndicator();
        
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(statusIndicator, BorderLayout.EAST);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // ==================== 中间内容区 ====================
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BorderLayout(10, 10));

        // 左侧：操作与统计面板
        JPanel leftPanel = new RoundedPanel(15);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(255, 255, 255, 230));
        leftPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        leftPanel.setPreferredSize(new Dimension(250, 300));

        // 操作按钮
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        startBtn = new ModernButton("▶ 启动服务器", new Color(52, 211, 153), new Color(16, 185, 129));
        stopBtn = new ModernButton("⏹ 停止服务器", new Color(248, 113, 113), new Color(239, 68, 68));
        stopBtn.setEnabled(false);

        buttonPanel.add(startBtn);
        buttonPanel.add(Box.createVerticalStrut(12));
        buttonPanel.add(stopBtn);

        leftPanel.add(buttonPanel);
        leftPanel.add(Box.createVerticalStrut(20));

        // 统计信息
        JLabel statsTitle = new JLabel("📊 服务器统计");
        statsTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        statsTitle.setForeground(new Color(51, 65, 85));
        leftPanel.add(statsTitle);
        leftPanel.add(Box.createVerticalStrut(12));

        statusLabel = createStatLabel("状态: 已停止", new Color(107, 114, 128));
        requestCountLabel = createStatLabel("请求数: 0", new Color(59, 130, 246));
        upTimeLabel = createStatLabel("运行时间: 0s", new Color(34, 197, 94));

        leftPanel.add(statusLabel);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(requestCountLabel);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(upTimeLabel);
        leftPanel.add(Box.createVerticalGlue());

        // 右侧：日志面板
        JPanel rightPanel = new RoundedPanel(15);
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setBackground(new Color(255, 255, 255, 230));
        rightPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel logTitle = new JLabel("📋 实时日志");
        logTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        logTitle.setForeground(new Color(51, 65, 85));

        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(new Color(17, 24, 39));
        logPane.setForeground(new Color(229, 231, 235));
        logPane.setFont(new Font("Consolas", Font.PLAIN, 12));
        logPane.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        rightPanel.add(logTitle, BorderLayout.NORTH);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(leftPanel, BorderLayout.WEST);
        centerPanel.add(rightPanel, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // ==================== 底部信息栏 ====================
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        JLabel versionLabel = new JLabel("v1.0.0 | 现代化HTTP服务器");
        versionLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        versionLabel.setForeground(new Color(255, 255, 255, 180));
        
        bottomPanel.add(versionLabel);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // ==================== 事件绑定 ====================
        startBtn.addActionListener(e -> {
            server.start();
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            statusIndicator.setRunning(true);
            startTime = System.currentTimeMillis();
            statusLabel.setText("状态: 运行中 ✓");
            statusLabel.setForeground(new Color(34, 197, 94));
        });

        stopBtn.addActionListener(e -> {
            server.stop();
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            statusIndicator.setRunning(false);
            startTime = 0;
            statusLabel.setText("状态: 已停止 ✗");
            statusLabel.setForeground(new Color(239, 68, 68));
            upTimeLabel.setText("运行时间: 0s");
        });
    }

    private JLabel createStatLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        label.setForeground(color);
        return label;
    }

    private void appendLog(String msg) {
        StyledDocument doc = logPane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        
        // 根据日志类型设置颜色
        if (msg.contains("错误") || msg.contains("Error")) {
            StyleConstants.setForeground(attrs, new Color(248, 113, 113));
            StyleConstants.setBold(attrs, true);
        } else if (msg.contains("成功") || msg.contains("Success") || msg.contains("✓")) {
            StyleConstants.setForeground(attrs, new Color(52, 211, 153));
        } else if (msg.contains("警告") || msg.contains("Warning")) {
            StyleConstants.setForeground(attrs, new Color(251, 191, 36));
        } else {
            StyleConstants.setForeground(attrs, new Color(200, 210, 230));
        }
        
        try {
            doc.insertString(doc.getLength(), msg + "\n", attrs);
            logPane.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatus() {
        if (startTime > 0) {
            long seconds = (System.currentTimeMillis() - startTime) / 1000;
            upTimeLabel.setText("运行时间: " + formatTime(seconds));
        }
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServerFrame().setVisible(true);
        });
    }

    // ==================== 自定义组件 ====================

    /**
     * 渐变背景面板
     */
    static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();
            
            // 创建深蓝到紫色的渐变
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(30, 58, 138),
                w, h, new Color(88, 28, 135)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, w, h);
            
            super.paintComponent(g);
        }
    }

    /**
     * 圆角面板
     */
    static class RoundedPanel extends JPanel {
        private int radius;

        public RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(getBackground());
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            
            super.paintComponent(g);
        }
    }

    /**
     * 现代化按钮
     */
    static class ModernButton extends JButton {
        private Color normalColor;
        private Color hoverColor;
        private boolean isHovered = false;

        public ModernButton(String text, Color normalColor, Color hoverColor) {
            super(text);
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
            
            setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setBackground(normalColor);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 45));
            setMaximumSize(new Dimension(200, 45));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    setBackground(hoverColor);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    setBackground(normalColor);
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 绘制阴影
            if (isHovered) {
                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 22, 22);
            }
            
            // 绘制按钮背景
            g2d.setColor(getBackground());
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            
            super.paintComponent(g);
        }
    }

    /**
     * 状态指示器
     */
    static class StatusIndicator extends JPanel {
        private boolean isRunning = false;
        private float pulseAlpha = 1f;
        private boolean pulsing = true;

        public StatusIndicator() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, 40));
            
            Timer pulseTimer = new Timer(30, e -> {
                if (isRunning) {
                    if (pulsing) {
                        pulseAlpha -= 0.02f;
                        if (pulseAlpha <= 0.3f) pulsing = false;
                    } else {
                        pulseAlpha += 0.02f;
                        if (pulseAlpha >= 1f) pulsing = true;
                    }
                    repaint();
                }
            });
            pulseTimer.start();
        }

        public void setRunning(boolean running) {
            isRunning = running;
            pulseAlpha = 1f;
            pulsing = true;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int x = getWidth() - 30;
            int y = getHeight() / 2 - 8;
            
            if (isRunning) {
                // 脉冲外圈
                g2d.setColor(new Color(52, 211, 153, (int)(100 * pulseAlpha)));
                g2d.fillOval(x - 10, y - 8, 36, 16);
                
                // 绿色指示灯
                g2d.setColor(new Color(34, 197, 94));
                g2d.fillOval(x, y, 16, 16);
            } else {
                // 灰色指示灯
                g2d.setColor(new Color(156, 163, 175));
                g2d.fillOval(x, y, 16, 16);
            }
            
            // 文字
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            g2d.drawString(isRunning ? "运行中" : "已停止", x - 50, y + 13);
        }
    }
}
