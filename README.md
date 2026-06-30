# Grafana · Prometheus · Micrometer 学习项目

Java 21 + Spring Boot 3.x 渐进式学习仓库，通过「商品查询服务」串联 Micrometer → Prometheus → Grafana 完整可观测性链路。

- **设计文档：** [docs/superpowers/specs/2026-06-26-grafana-prometheus-micrometer-learning-design.md](docs/superpowers/specs/2026-06-26-grafana-prometheus-micrometer-learning-design.md)
- **实现计划：** [docs/superpowers/plans/](docs/superpowers/plans/)

## 学习进度

| 阶段 | 主题 | 状态 |
|------|------|------|
| 0 | 环境与概念预热（Actuator、商品 API） | ✅ 已完成 |
| 1 | Micrometer + `/actuator/prometheus` 端点 | ✅ 已完成 |
| 2 | Prometheus 采集（WSL Docker Pull） | ✅ 已完成 |
| 3 | Grafana 可视化（Dashboard + PromQL） | ✅ 已完成 |
| 4 | 业务指标 + PostgreSQL / Redis 自动指标 | ⏳ 下一步 |
| 5 | 巩固与模式地图 | 待开始 |

---

## 环境要求

| 项 | 版本 / 说明 |
|----|-------------|
| JDK | 21 |
| Maven | 3.9+ |
| IDE | IntelliJ IDEA（Open 仓库根目录） |
| 应用运行 | **Windows**（IDEA 或 `mvn spring-boot:run`） |
| 监控栈 | **WSL** Docker Compose（Prometheus + Grafana） |

---

## 架构概览

```text
Windows (IDEA)
  metrics-learn-app :8080
    └── /actuator/prometheus
           │
           │  Pull scrape（WSL 网关 IP → Windows）
           ▼
WSL Docker Compose
  prometheus-learn :9090
    └── TSDB ← ./prometheus-data
           │
           │  PromQL（容器内网 http://prometheus:9090）
           ▼
  grafana-learn :3000
    └── Dashboard ← ./grafana-data
```

**本机网络要点：** Prometheus 从 WSL 访问 Windows 上的应用，scrape target 使用 WSL 默认网关 IP（本机为 **`192.168.16.1:8080`**）。若环境变化，在 WSL 执行 `ip route show | awk '/default/ {print $3}'` 重新获取。

---

## 快速启动

### 1. 启动 Spring Boot 应用（Windows）

```powershell
cd D:\Project_Install\JAVA_Develop\Operations-And-Maintenance\Grafana-Prometheus-Micrometer-Learn
mvn -pl metrics-learn-app spring-boot:run
```

或在 IDEA 中运行 `com.metricslearn.MetricsLearnApplication`（JDK 21）。

验证：

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/products/1
```

### 2. 启动监控栈（WSL）

```bash
cd /mnt/d/Project_Install/JAVA_Develop/Operations-And-Maintenance/Grafana-Prometheus-Micrometer-Learn/docker/observability
# 若使用 /work/Metrics 部署，进入对应目录即可

mkdir -p prometheus-data grafana-data
sudo chown -R 65534:65534 prometheus-data
sudo chown -R 472:472 grafana-data

docker compose up -d
docker compose ps
```

期望：`prometheus-learn`、`grafana-learn` 均为 **Up (healthy)**。

### 3. 访问地址

| 服务 | URL | 账号 |
|------|-----|------|
| 应用 API | http://localhost:8080/api/products | — |
| Actuator | http://localhost:8080/actuator/prometheus | — |
| Prometheus | http://localhost:9090 | 无 |
| Prometheus Targets | http://localhost:9090/targets | job `metrics-learn` 应为 **UP** |
| Grafana | http://localhost:3000 | `admin` / `metrics-learn` |

若通过 WSL 主机 IP 访问（如 `192.168.19.64`），将 `localhost` 替换为对应 IP 即可。

---

## 各阶段摘要

### 阶段 0：环境与概念预热

- 商品 API：`GET /api/products`、`GET /api/products/{id}`
- Actuator：`/actuator/health`、`/actuator/metrics`
- 计划：[2026-06-26-phase-0-environment-warmup.md](docs/superpowers/plans/2026-06-26-phase-0-environment-warmup.md)

### 阶段 1：Prometheus 端点

- `/actuator/prometheus` 文本格式导出
- 公共标签：`application=metrics-learn`、`environment=local`
- 计划：[2026-06-26-phase-1-prometheus-endpoint.md](docs/superpowers/plans/2026-06-26-phase-1-prometheus-endpoint.md)

**测试注意：** `@SpringBootTest` 需在 `src/test/resources/application.properties` 中开启 `management.prometheus.metrics.export.enabled=true`。

### 阶段 2：Prometheus 采集

- WSL Docker 运行 `prometheus-learn`
- `prometheus.yml` scrape target：`192.168.16.1:8080`
- 数据目录：`./prometheus-data`（与 compose 同级 bind mount）
- 计划：[2026-06-26-phase-2-prometheus-scrape.md](docs/superpowers/plans/2026-06-26-phase-2-prometheus-scrape.md)

**PromQL 验证：**

```promql
up{job="metrics-learn"}
http_server_requests_seconds_count{application="metrics-learn"}
```

### 阶段 3：Grafana 可视化

- WSL Docker 运行 `grafana-learn`，依赖 Prometheus healthy 后启动
- 数据源：Prometheus → `http://prometheus:9090`（Compose 内网）
- Dashboard：**Metrics Learn → Metrics Learn Overview**（HTTP QPS、P95、JVM 堆、GC）
- 计划：[2026-06-26-phase-3-grafana-visualization.md](docs/superpowers/plans/2026-06-26-phase-3-grafana-visualization.md)

**P95 前置配置**（`application.yml` 已启用）：

```yaml
management.metrics.distribution.percentiles-histogram:
  "[http.server.requests]": true
```

否则有 `_count` 但无 `_bucket`，`histogram_quantile` 无法计算 P95。

**常用 PromQL：**

```promql
# QPS
sum(rate(http_server_requests_seconds_count{application="metrics-learn"}[1m]))

# P95 延迟（建议 [5m] 窗口）
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="metrics-learn"}[5m])) by (le))

# JVM 堆
sum(jvm_memory_used_bytes{application="metrics-learn", area="heap"})
```

**产生流量观察曲线（Windows）：**

```powershell
1..20 | ForEach-Object { curl -s http://localhost:8080/api/products/1 | Out-Null; Start-Sleep -Milliseconds 200 }
```

---

## 项目结构

```text
├── pom.xml                              # 父 POM（Java 21、Spring Boot 3.3.x）
├── metrics-learn-app/                   # Spring Boot 应用
├── docker/observability/
│   ├── docker-compose.yml               # prometheus-learn + grafana-learn
│   ├── prometheus/
│   │   └── prometheus.yml
│   ├── grafana/                         # provisioning + dashboards（阶段 3）
│   ├── prometheus-data/               # TSDB（gitignore，本地创建）
│   └── grafana-data/                    # Grafana 数据（gitignore，本地创建）
├── docs/
│   ├── superpowers/specs/               # 设计文档
│   └── superpowers/plans/               # 分阶段实现计划
└── README.md
```

---

## 运行测试

```bash
mvn -pl metrics-learn-app test
```

---

## 常见问题

| 现象 | 处理 |
|------|------|
| Prometheus Target **DOWN** | 确认 Windows 应用已启动；检查 `prometheus.yml` 中 IP 是否为 WSL 网关 |
| WSL 无法访问 Windows `:8080` | Windows 防火墙放行 8080；WSL 用 `curl http://192.168.16.1:8080/actuator/prometheus` 验证 |
| Grafana 无数据 | 先确认 Prometheus Targets UP；Dashboard 时间范围选 **Last 15 minutes** |
| P95 Panel 空白 | 确认已开启 `percentiles-histogram`；先有 HTTP 流量；PromQL 使用 `[5m]` |
| 测试 `/actuator/prometheus` 404 | 测试环境需 `management.prometheus.metrics.export.enabled=true` |

---

## 下一步：阶段 4

在 `metrics-learn-app` 中增加：

- 业务自定义指标（Counter / Timer）
- PostgreSQL（HikariCP 自动指标）
- Redis（Lettuce 自动指标）

详见设计文档 [阶段 4](docs/superpowers/specs/2026-06-26-grafana-prometheus-micrometer-learning-design.md) 与后续实现计划。
