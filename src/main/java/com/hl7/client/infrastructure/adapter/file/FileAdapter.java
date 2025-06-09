package com.hl7.client.infrastructure.adapter.file;

import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 文件适配器
 * 用于从文件中读取数据
 */
@Slf4j
@Component
public class FileAdapter implements DeviceAdapter {

    private Device device;
    private Path watchDirectory;
    private String filePattern;
    private WatchService watchService;
    private Thread watchThread;
    private volatile boolean running = false;
    private final BlockingQueue<String> fileContents = new LinkedBlockingQueue<>();

    @Override
    public void initialize(Device device) {
        this.device = device;
        try {
            // 从连接参数解析监视目录和文件模式 (格式: /path/to/directory:*.hl7)
            String[] params = device.getConnectionParams().split(":");
            String directoryPath = params[0];
            this.filePattern = params.length > 1 ? params[1] : "*";

            this.watchDirectory = Paths.get(directoryPath);
            if (!Files.exists(watchDirectory)) {
                Files.createDirectories(watchDirectory);
            }
        } catch (Exception e) {
            log.error("初始化文件适配器失败: {}", e.getMessage());
            throw new IllegalArgumentException("连接参数格式错误，应为directory:filePattern");
        }
    }

    @Override
    public boolean connect() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            watchDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            running = true;
            watchThread = new Thread(this::watchFiles);
            watchThread.setDaemon(true);
            watchThread.start();

            // 扫描现有文件
            scanExistingFiles();

            log.info("成功启动文件监视，目录: {}, 文件模式: {}", watchDirectory, filePattern);
            return true;
        } catch (IOException e) {
            log.error("启动文件监视失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void disconnect() {
        running = false;
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }

        if (watchService != null) {
            try {
                watchService.close();
                watchService = null;
            } catch (IOException e) {
                log.error("关闭文件监视服务失败: {}", e.getMessage());
            }
        }

        log.info("已停止文件监视");
    }

    @Override
    public boolean send(String data) {
        log.warn("文件适配器不支持发送数据");
        return false;
    }

    @Override
    public String receive() {
        try {
            String content = fileContents.poll(30, TimeUnit.SECONDS);
            if (content != null) {
                log.debug("从文件中读取到数据，长度: {}", content.length());
            }
            return content;
        } catch (InterruptedException e) {
            log.error("接收数据被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public boolean isConnected() {
        return running && watchThread != null && watchThread.isAlive();
    }

    @Override
    public Device getDevice() {
        return device;
    }

    /**
     * 处理接收到的原始数据
     * 文件适配器不直接处理接收数据，而是通过文件监视自动处理
     * @param rawData 接收到的原始数据字符串
     * @return 如果需要响应，返回响应内容；否则返回null
     */
    @Override
    public String processReceivedData(String rawData) {
        log.debug("文件适配器不直接处理接收数据: {}", rawData);
        return null;
    }

    /**
     * 扫描现有文件
     */
    private void scanExistingFiles() {
        try (Stream<Path> paths = Files.list(watchDirectory)) {
            // 提前编译正则表达式以提高性能
            Pattern pattern = Pattern.compile(filePattern.replace("*", ".*"));

            paths.filter(path -> pattern.matcher(path.getFileName().toString()).matches())
                    .forEach(this::processFile);
        } catch (IOException e) {
            log.error("扫描现有文件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 监视文件夹中的新文件
     */
    private void watchFiles() {
        try {
            while (running) {
                // 阻塞等待，直到有文件系统事件发生或线程被中断
                // take() 会移除并返回下一个 WatchKey；如果队列为空，则等待。
                WatchKey key = watchService.take();
                // 处理与此 WatchKey 关联的所有待处理事件
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        // 使用 resolve 将被监视目录的路径与相对路径组合，得到新创建文件的完整路径
                        @SuppressWarnings("unchecked")
                        Path file = watchDirectory.resolve(((WatchEvent<Path>) event).context());
                        if (file.getFileName().toString().matches(filePattern.replace("*", ".*"))) {
                            processFile(file);
                        }
                    }
                }
                // 重置 WatchKey 以接收后续事件
                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.debug("文件监视线程被中断");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("文件监视线程出错: {}", e.getMessage());
        }
    }

    /**
     * 处理文件
     *
     * @param file 文件路径
     */
    private void processFile(Path file) {
        try {
            // 等待文件写入完成
            Thread.sleep(500);

            // 读取文件内容
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            String content = String.join("\n", lines);

            // 将内容放入队列
            fileContents.put(content);

            // 处理完成后删除文件
            Files.delete(file);
            log.info("成功处理文件: {}", file);
        } catch (IOException e) {
            log.error("处理文件 {} 失败: {}", file, e.getMessage());
        } catch (InterruptedException e) {
            log.error("处理文件被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
