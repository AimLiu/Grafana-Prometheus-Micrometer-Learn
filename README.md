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
| 4 | 业务指标 + PostgreSQL / Redis 自动指标 | ✅ 已完成 |
| 5 | 巩固与模式地图 | ⏳ 下一步 |

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
  metrics-learn-app :8080  (profile: dev)
    ├── /api/products、/api/orders、/api/cache/demo
    ├── PostgreSQL (WSL postgres-alpine :5432)
    ├── Redis (WSL redis-alpine :6379)
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
    └── Dashboard「Metrics Learn Overview」← ./grafana-data
```

**本机网络要点：** Prometheus 从 WSL 访问 Windows 上的应用，scrape target 使用 WSL 默认网关 IP（本机为 **`192.168.16.1:8080`**）。若环境变化，在 WSL 执行 `ip route show | awk '/default/ {print $3}'` 重新获取。

---

## 快速启动

### 1. 启动 Spring Boot 应用（Windows）

确保 WSL 中 **PostgreSQL**、**Redis** 已运行；应用默认使用 `dev` profile（见 `application-dev.yml`）。

```powershell
cd D:\Project_Install\JAVA_Develop\Operations-And-Maintenance\Grafana-Prometheus-Micrometer-Learn
mvn -pl metrics-learn-app spring-boot:run
```

或在 IDEA 中运行 `com.metricslearn.MetricsLearnApplication`（JDK 21，`dev` profile）。

验证：

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/products/1
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"productId\":1,\"quantity\":1}"
curl http://localhost:8080/api/cache/demo/hello
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
| 商品 API | http://localhost:8080/api/products | — |
| 订单 API | http://localhost:8080/api/orders | `POST` 创建 / `GET /{id}` 查询 |
| 缓存 API | http://localhost:8080/api/cache/demo | `GET /{key}` / `POST` 写入 |
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
- Dashboard：**Metrics Learn → Metrics Learn Overview**
  - HTTP QPS、**P95 延迟**、JVM 堆、GC
  - 阶段 4 扩展：订单业务、HikariCP 连接池、Lettuce Redis 命令
- 计划：[2026-06-26-phase-3-grafana-visualization.md](docs/superpowers/plans/2026-06-26-phase-3-grafana-visualization.md)

**P95 前置配置**（`application.yml` 已启用）：

```yaml
management.metrics.distribution.percentiles-histogram:
  "[http.server.requests]": true
```

否则有 `_count` 但无 `_bucket`，`histogram_quantile` 无法计算 P95。

**常用 PromQL（HTTP / JVM）：**

```promql
# QPS
sum(rate(http_server_requests_seconds_count{application="metrics-learn"}[1m]))

# P95 延迟（建议 [5m] 窗口）
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="metrics-learn"}[5m])) by (le))

# JVM 堆
sum(jvm_memory_used_bytes{application="metrics-learn", area="heap"})
```

### 阶段 4：业务指标 + 中间件自动指标

- **4a 业务埋点（手动）：** `OrderService` 使用 `MeterRegistry`
  - Counter：`app_orders_total{status="success|failure"}`
  - Timer：`app_orders_query_seconds`（查单耗时）
- **4b PostgreSQL（自动）：** HikariCP → `hikaricp_connections_*`
- **4c Redis（自动）：** Lettuce → `lettuce_command_completion_seconds_*`
- 计划：[2026-06-26-phase-4-business-and-middleware-metrics.md](docs/superpowers/plans/2026-06-26-phase-4-business-and-middleware-metrics.md)

**新增 API：**

| 方法 | 路径 | 指标来源 |
|------|------|----------|
| `POST` | `/api/orders` | `app_orders_total` |
| `GET` | `/api/orders/{id}` | HikariCP + `app_orders_query_seconds` |
| `GET` | `/api/cache/demo/{key}` | `lettuce_command_*` |
| `POST` | `/api/cache/demo` | `lettuce_command_*` |

**中间件配置：** `application-dev.yml`（`dev` profile，连接 WSL PostgreSQL / Redis；按本机 IP 修改 `spring.datasource.url` 与 `spring.data.redis.host`）。

**Grafana Dashboard 已验证 Panel：**

| Panel | PromQL 示例 |
|-------|-------------|
| 订单成功速率 | `sum(rate(app_orders_total{application="metrics-learn",status="success"}[1m]))` |
| HikariCP 活跃连接 | `hikaricp_connections_active{application="metrics-learn"}` |
| Redis 命令速率 | `sum(rate(lettuce_command_completion_seconds_count{application="metrics-learn"}[1m]))` |
| HTTP P95 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="metrics-learn"}[5m])) by (le))` |

**指标导出快速检查：**

```powershell
curl -s http://localhost:8080/actuator/prometheus | findstr /C:"app_orders_total" /C:"hikaricp_connections" /C:"lettuce_command"
```

**产生流量观察曲线（可用 JMeter 并发或 PowerShell）：**

```powershell
# 商品 + 订单 + 缓存混合流量
1..20 | ForEach-Object {
  curl -s http://localhost:8080/api/products/1 | Out-Null
  curl -s -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"productId\":1,\"quantity\":1}" | Out-Null
  curl -s http://localhost:8080/api/cache/demo/test | Out-Null
  Start-Sleep -Milliseconds 200
}
```

**测试：** 单元/集成测试使用 `test` profile（H2 内存库），不依赖 WSL 中间件；`mvn -pl metrics-learn-app test`。

---

## 项目结构

```text
├── pom.xml                              # 父 POM（Java 21、Spring Boot 3.3.x）
├── metrics-learn-app/                   # Spring Boot 应用
│   ├── domain/Order.java                # 订单 JPA 实体
│   ├── service/OrderService.java        # 业务 Counter / Timer 埋点
│   ├── service/CacheDemoService.java    # Redis 缓存封装
│   └── controller/                      # Product / Order / CacheDemo
├── docker/observability/
│   ├── docker-compose.yml               # prometheus-learn + grafana-learn
│   ├── prometheus/
│   │   └── prometheus.yml
│   ├── prometheus-data/                 # TSDB（gitignore，本地创建）
│   └── grafana-data/                    # Grafana 数据 + Dashboard（gitignore）
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
| 无 `app_orders_total` / `hikaricp_*` | 确认 `dev` profile 已激活；调用订单 API 产生写库流量 |
| 无 `lettuce_command_*` | 确认 Redis 可连；调用 `/api/cache/demo` 接口 |
| 测试 `/actuator/prometheus` 404 | 测试环境需 `management.prometheus.metrics.export.enabled=true` |
| 订单 API 500 / 连不上 DB | 检查 `application-dev.yml` 中 PostgreSQL 地址与库 `metrics_learn` |

---

## 下一步：阶段 5

巩固「手动埋点 vs 自动埋点」模式，整理指标分类与 PromQL 速查表，完成学习自检。

详见设计文档 [阶段 5](docs/superpowers/specs/2026-06-26-grafana-prometheus-micrometer-learning-design.md)。
