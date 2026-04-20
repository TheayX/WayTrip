package com.travel;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WayTrip 后端应用启动入口。
 *
 * <p>负责初始化 Spring Boot 容器，并启用定时任务能力；
 * 同时在应用启动前为日志目录设置统一的默认值，避免不同启动目录导致日志路径漂移。
 */
@SpringBootApplication
@EnableScheduling
public class TravelApplication {

    /**
     * 启动后端服务进程。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        configureLogHomeDefault();
        SpringApplication.run(TravelApplication.class, args);
    }

    /**
     * 在应用启动前补齐日志根目录的默认值。
     *
     * <p>优先使用显式配置的 `logging.log-home`、`LOG_HOME` 系统属性或环境变量；
     * 如果都未配置，则根据当前启动目录自动选择相对路径，保证日志最终落到同一逻辑位置。
     */
    private static void configureLogHomeDefault() {
        String explicit = firstNonBlank(
                System.getProperty("logging.log-home"),
                System.getProperty("LOG_HOME"),
                System.getenv("LOG_HOME")
        );
        if (explicit != null) {
            System.setProperty("logging.log-home", explicit);
            return;
        }

        // Keep one logical log location for both cwd=WayTrip and cwd=travel-server.
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path serverDir = cwd.resolve("travel-server");
        String detected = Files.isDirectory(serverDir) ? "./travel-server/logs" : "./logs";
        System.setProperty("logging.log-home", detected);
    }

    /**
     * 按顺序返回第一个非空白字符串。
     *
     * @param values 候选值列表
     * @return 第一个非空白字符串；若全部为空则返回 {@code null}
     */
    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
