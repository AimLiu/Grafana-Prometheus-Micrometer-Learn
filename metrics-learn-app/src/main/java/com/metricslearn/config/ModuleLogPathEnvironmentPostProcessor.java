package com.metricslearn.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 在 Logback 初始化前解析日志目录，固定输出到 {@code metrics-learn-app/log}，
 * 避免从仓库根目录启动时日志写到 {@code Grafana-Prometheus-Micrometer-Learn/log}。
 * <p>
 * 须注册在 {@code META-INF/spring.factories}（Spring Boot 3.4 仍使用该机制）。
 * 可通过环境变量 {@code LOGGING_FILE_PATH} 或配置项 {@code logging.file.path} 覆盖。
 */
public class ModuleLogPathEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String MODULE_NAME = "metrics-learn-app";
    private static final String LOGGING_FILE_PATH = "logging.file.path";
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (hasExplicitLogPath(environment)) {
            return;
        }
        Path logDir = resolveModuleLogDir();
        String logPath = logDir.toString();
        environment.getPropertySources().addFirst(
                new MapPropertySource("moduleLogPath", Map.of(LOGGING_FILE_PATH, logPath)));
        System.setProperty(LOGGING_FILE_PATH, logPath);
    }
    @Override
    public int getOrder() {
        // 在 application.yml 等配置加载完成后再解析，避免被空配置覆盖
        return Ordered.LOWEST_PRECEDENCE;
    }
    private boolean hasExplicitLogPath(ConfigurableEnvironment environment) {
        String fromEnv = environment.getProperty(LOGGING_FILE_PATH);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return true;
        }
        String loggingFilePath = System.getenv("LOGGING_FILE_PATH");
        if (loggingFilePath != null && !loggingFilePath.isBlank()) {
            return true;
        }
        String typoEnv = System.getenv("LOGGIN_FILE_PATH");
        return typoEnv != null && !typoEnv.isBlank();
    }

    /**
     * 若当前工作目录已是模块根目录则使用 {@code ./log}；
     * 若从多模块仓库根目录启动则使用 {@code metrics-learn-app/log}。
     */
    static Path resolveModuleLogDir() {
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (MODULE_NAME.equals(cwd.getFileName().toString())) {
            return cwd.resolve("log");
        }
        Path moduleUnderCwd = cwd.resolve(MODULE_NAME);
        if (Files.isDirectory(moduleUnderCwd)) {
            return moduleUnderCwd.resolve("log");
        }
        return cwd.resolve("log");
    }
}
