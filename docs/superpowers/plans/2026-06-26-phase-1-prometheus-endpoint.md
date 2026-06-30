# 阶段 1：Micrometer + Prometheus 端点 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在阶段 0 商品查询服务基础上，把 `/actuator/prometheus` 打造成可供 Prometheus 拉取的标准文本指标端点，理解 Micrometer 命名与 Prometheus 导出格式的对应关系，并配置统一的公共标签（`application`、`environment`）。

**Architecture:** 复用 `metrics-learn-app` 现有 REST API 产生 HTTP 流量；通过已引入的 `micrometer-registry-prometheus` 将 Micrometer 指标转为 Prometheus 文本格式；用 MockMvc 对 `/actuator/prometheus` 做内容断言（`# HELP`、`# TYPE`、样本行、公共标签）；配套概念文档与 README 阶段导航。本阶段**不启动** Prometheus 容器（留待阶段 2）。

**Tech Stack:** Java 21、Spring Boot 3.3.5、Maven、Micrometer、micrometer-registry-prometheus、Spring Boot Actuator、JUnit 5、MockMvc、Hamcrest

---

## 实际场景说明

### 业务故事

阶段 0 完成后，运维同事确认应用健康与 JSON 指标端点正常。下一步部署 Prometheus（阶段 2）之前，需要你完成 **Prometheus 采集就绪检查**：

1. 应用是否暴露标准的 **`/actuator/prometheus`** 文本端点？
2. 指标名是否符合 Prometheus 命名习惯（下划线，而非 Actuator 里的点号）？
3. 所有指标是否带上 **`application`**、**`environment`** 公共标签，便于后续多环境筛选？
4. 你能否**读懂**一行真实的 Prometheus 样本数据（含标签与数值）？

### 本阶段延续的 API（阶段 0 已有，用于产生指标）

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/api/products` | 产生 HTTP 成功指标 |
| GET | `/api/products/{id}` | 产生带 `uri` 标签的 HTTP 指标 |
| GET | `/api/products/999` | 产生 `404` 状态指标（手动练习可选） |

### 阶段 1 结束时应达到的效果

| 维度 | 效果 |
|------|------|
| **端点** | `GET /actuator/prometheus` 返回 `200`，`Content-Type` 为 `text/plain`（含 `version=0.0.4`） |
| **格式** | 响应含 `# HELP`、`# TYPE` 与至少一行样本指标 |
| **指标名** | 可见 `jvm_memory_used_bytes`、`http_server_requests_seconds_count`（下划线命名） |
| **公共标签** | 样本行含 `application="metrics-learn"`、`environment="local"` |
| **对比理解** | 能说明 `http.server.requests`（Actuator）与 `http_server_requests_seconds`（Prometheus）的对应关系 |
| **自动化** | `mvn -pl metrics-learn-app test` 全部通过（含新增 Prometheus 端点测试） |
| **概念** | 阅读 `docs/learning/phase-1-prometheus-endpoint.md` 并回答 4 道自检题 |
| **范围边界** | 本阶段**不验收** Prometheus scrape、PromQL 查询、Grafana 面板 |

### 当前仓库状态（增量起点）

以下**已存在**（阶段 0 或更早），本计划不重复创建：

| 路径 | 状态 |
|------|------|
| `metrics-learn-app/pom.xml` | 已含 `micrometer-registry-prometheus` |
| `application.yml` | 已暴露 `prometheus` 端点；已有 `application` 标签 |
| `ProductController` / `ProductService` / `Product` | 商品 API 可用 |
| `ActuatorHealthEndpointTest` | 健康端点测试 |
| `ActuatorMetricsEndpointTest` | JSON 指标端点测试 |
| `ModuleLogPathEnvironmentPostProcessor` | 日志路径处理 |

本计划**补齐**阶段 1 缺失部分：

- `environment` 公共标签配置
- `ActuatorPrometheusEndpointTest`（Prometheus 文本格式断言）
- `docs/learning/phase-1-prometheus-endpoint.md`
- `README.md`（项目总览 + 阶段 0/1 导航；若已存在则追加阶段 1 章节）

---

## 文件结构（本阶段新增/修改）

| 文件 | 职责 |
|------|------|
| `metrics-learn-app/src/main/resources/application.yml` | 增加 `environment` 公共标签 |
| `metrics-learn-app/src/test/java/com/metricslearn/actuator/ActuatorPrometheusEndpointTest.java` | Prometheus 文本端点测试 |
| `docs/learning/phase-1-prometheus-endpoint.md` | Micrometer 导出、命名转换、格式解读、自检题 |
| `README.md` | 阶段 1 操作指南与验收 checklist |

---

## Task 1: 公共标签与 Prometheus 端点配置

**Files:**
- Modify: `metrics-learn-app/src/main/resources/application.yml`

**背景:** `micrometer-registry-prometheus` 与 `management.endpoints.web.exposure.include=prometheus` 已配置。本 Task 补齐设计文档要求的 **`application` + `environment` 双公共标签**，并整理 YAML 格式。

- [ ] **Step 1: 更新 application.yml**

将 `management.metrics.tags` 区块改为（保留其余配置不变）：

```yaml
spring:
  application:
    name: ${APPLICATION_NAME:metrics-learn}

server:
  port: ${SERVER_PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${METRICS_ENVIRONMENT:local}
```

- [ ] **Step 2: 编译验证**

Run:

```bash
cd D:\Project_Install\JAVA_Develop\Operations-And-Maintenance\Grafana-Prometheus-Micrometer-Learn
mvn -pl metrics-learn-app -q compile
```

Expected: exit code `0`

- [ ] **Step 3: 手动快速预览（可选）**

Run:

```bash
mvn -pl metrics-learn-app spring-boot:run
```

另开终端：

```bash
curl -s http://localhost:8080/api/products/1
curl -s http://localhost:8080/actuator/prometheus | findstr /C:"application=\"metrics-learn\"" /C:"environment=\"local\"" /C:"jvm_memory_used_bytes"
```

Expected: 三行均有输出。完成后 `Ctrl+C` 停止应用。

- [ ] **Step 4: Commit**

```bash
git add metrics-learn-app/src/main/resources/application.yml
git commit -m "feat(phase-1): add environment common tag for prometheus export"
```

---

## Task 2: Prometheus 文本端点自动化测试

**Files:**
- Create: `metrics-learn-app/src/test/java/com/metricslearn/actuator/ActuatorPrometheusEndpointTest.java`

- [ ] **Step 1: 编写 Prometheus 端点测试**

```java
package com.metricslearn.actuator;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorPrometheusEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheusEndpointShouldReturnPlainTextMetrics() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(allOf(
                        containsString("# HELP jvm_memory_used_bytes"),
                        containsString("# TYPE jvm_memory_used_bytes gauge"),
                        containsString("# TYPE http_server_requests_seconds"),
                        containsString("http_server_requests_seconds_count"),
                        containsString("application=\"metrics-learn\""),
                        containsString("environment=\"local\"")
                )));
    }

    @Test
    void prometheusEndpointShouldContainHttpUriLabelAfterApiCall() throws Exception {
        mockMvc.perform(get("/api/products/2"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("uri=\"/api/products/{id}\"")));
    }
}
```

- [ ] **Step 2: 运行新增测试**

Run:

```bash
mvn -pl metrics-learn-app -q test -Dtest=ActuatorPrometheusEndpointTest
```

Expected: `Tests run: 2, Failures: 0`

- [ ] **Step 3: 运行全部测试**

Run:

```bash
mvn -pl metrics-learn-app -q test
```

Expected: 全部通过（阶段 0 的 7 个 + 本阶段 2 个 = **9 个**）

- [ ] **Step 4: Commit**

```bash
git add metrics-learn-app/src/test/java/com/metricslearn/actuator/ActuatorPrometheusEndpointTest.java
git commit -m "test(phase-1): verify prometheus text endpoint and common tags"
```

---

## Task 3: 概念学习文档

**Files:**
- Create: `docs/learning/phase-1-prometheus-endpoint.md`

- [ ] **Step 1: 创建阶段 1 概念文档（完整内容）**

```markdown
# 阶段 1：Micrometer + Prometheus 端点

## 1. 本阶段在整条链路中的位置

```text
业务代码 / Spring 自动埋点
    → Micrometer MeterRegistry（内存中的指标模型）
    → /actuator/metrics（JSON，给人读）
    → /actuator/prometheus（文本，给 Prometheus 拉）
    → [阶段 2] Prometheus scrape
    → [阶段 3] Grafana 查询展示
```

阶段 0 你认识了 JSON 指标端点；阶段 1 重点是 **Prometheus 专用导出格式**。

## 2. Micrometer 是什么？

Micrometer 是 Java 应用的 **指标门面**（类似 SLF4J 之于日志）：

- 代码和框架往 `MeterRegistry` 注册 Counter / Gauge / Timer
- Spring Boot Actuator 自动把 JVM、HTTP、Tomcat 等注册进去
- 通过不同 **Registry 实现** 导出到不同后端；`micrometer-registry-prometheus` 负责 Prometheus 文本格式

你不需要在本阶段手写 `MeterRegistry`，但要理解：**Actuator 指标底层就是 Micrometer**。

## 3. 两种端点：/actuator/metrics vs /actuator/prometheus

| 对比项 | /actuator/metrics | /actuator/prometheus |
|--------|-------------------|----------------------|
| 格式 | JSON | Prometheus 文本（text/plain; version=0.0.4） |
| 主要读者 | 人、调试 | Prometheus Server |
| 指标命名 | 点号：`http.server.requests` | 下划线：`http_server_requests_seconds` |
| 典型用途 | 开发期查看单个指标 | 生产期被 Pull 采集 |

**命名转换规则（常见）：**

- `.` → `_`
- Timer 常带 `_seconds` 后缀
- 直方图/摘要分解为 `_bucket`、`_count`、`_sum`

## 4. Prometheus 文本格式三要素

### 4.1 HELP（指标说明）

```text
# HELP jvm_memory_used_bytes The amount of used memory
```

### 4.2 TYPE（指标类型）

```text
# TYPE jvm_memory_used_bytes gauge
# TYPE http_server_requests_seconds histogram
```

Prometheus 侧类型：`counter`、`gauge`、`histogram`、`summary`。

### 4.3 样本行（时间序列）

```text
http_server_requests_seconds_count{application="metrics-learn",environment="local",method="GET",outcome="SUCCESS",status="200",uri="/api/products/{id}",} 1.0
```

读法：

- **指标名：** `http_server_requests_seconds_count`
- **标签：** `{application="...", environment="...", method="GET", ...}`
- **值：** `1.0`（请求计数）

## 5. 公共标签（Common Tags）

本项目配置：

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${METRICS_ENVIRONMENT:local}
```

**作用：** 未来同一 Prometheus 抓取多个服务时，可用 `application="metrics-learn"` 筛选；多环境用 `environment` 区分。

**注意：** 公共标签会附加到**几乎所有**指标上，请保持低基数（不要用随机值）。

## 6. 自动埋点：本阶段可见的关键指标

| Actuator 名（JSON） | Prometheus 名（文本） | 含义 |
|---------------------|----------------------|------|
| `jvm.memory.used` | `jvm_memory_used_bytes` | JVM 已用内存 |
| `http.server.requests` | `http_server_requests_seconds_*` | HTTP 请求耗时与次数 |
| `process.cpu.usage` | `process_cpu_usage` | 进程 CPU 使用率 |

调用 `GET /api/products/1` 后，重点观察 `http_server_requests_seconds_count` 的标签：

- `method=GET`
- `status=200`
- `uri=/api/products/{id}`（Spring 使用路径模板，而非具体 id，有利于低基数）

## 7. 动手练习

```bash
# 启动应用
mvn -pl metrics-learn-app spring-boot:run

# 产生流量
curl http://localhost:8080/api/products/1
curl http://localhost:8080/api/products/999

# 查看 Prometheus 文本端点（建议重定向到文件便于阅读）
curl -s http://localhost:8080/actuator/prometheus -o prometheus.txt

# Windows 下筛选关键行
findstr /C:"# HELP jvm_memory_used_bytes" prometheus.txt
findstr /C:"http_server_requests_seconds_count" prometheus.txt
findstr /C:"application=\"metrics-learn\"" prometheus.txt
```

任选一行业务 HTTP 指标，手动标注其：**指标名、3 个标签、数值**。

## 8. 自检题

1. Micrometer、Actuator、`micrometer-registry-prometheus` 三者分别扮演什么角色？
2. 为什么 Prometheus 拉取的是 `/actuator/prometheus`，而不是 `/actuator/metrics`？
3. `http.server.requests` 和 `http_server_requests_seconds_count` 是什么关系？Timer 为什么常出现 `_bucket`、`_count`、`_sum`？
4. 公共标签 `application` 和 `environment` 解决什么问题？为什么不建议把 `productId` 设为标签？

## 9. 参考答案要点

1. Micrometer = 指标 API/注册中心；Actuator = 对外暴露端点（含 metrics/prometheus）；prometheus registry = 把 Micrometer 指标序列化为 Prometheus 文本。
2. `/actuator/metrics` 是 JSON，非 Prometheus 标准格式；Prometheus Server 的 Pull 解析器需要 text format。
3. 同一指标在两种端点下的命名风格不同；Timer 在 Prometheus 中以 histogram/summary 形式展开，`_count` 是请求次数，`_bucket` 用于分位数计算。
4. `application`/`environment` 用于多服务、多环境筛选；`productId` 高基数，会导致时间序列爆炸。

## 10. 下一阶段预告

阶段 2 将在 WSL Docker 中启动 Prometheus，配置 `scrape_configs` 指向 `host.docker.internal:8080/actuator/prometheus`，并在 Prometheus UI 中确认 Target 为 **UP**。
```

- [ ] **Step 2: Commit**

```bash
git add docs/learning/phase-1-prometheus-endpoint.md
git commit -m "docs(phase-1): add micrometer prometheus endpoint concepts"
```

---

## Task 4: README 阶段导航

**Files:**
- Create or Modify: `README.md`

- [ ] **Step 1: 创建或更新 README.md**

若根目录 **尚无** `README.md`，创建以下完整内容；若 **已有**，在文末追加「阶段 1」章节（保留阶段 0 内容）。

```markdown
# Grafana · Prometheus · Micrometer 学习项目

Java 21 + Spring Boot 3.x 渐进式学习仓库。

- 设计文档：[docs/superpowers/specs/2026-06-26-grafana-prometheus-micrometer-learning-design.md](docs/superpowers/specs/2026-06-26-grafana-prometheus-micrometer-learning-design.md)
- 实现计划：[docs/superpowers/plans/](docs/superpowers/plans/)

## 环境要求

| 项 | 版本 |
|----|------|
| JDK | 21 |
| Maven | 3.9+ |
| IDE | IntelliJ IDEA（Open 仓库根目录） |

## 快速启动

```bash
mvn -pl metrics-learn-app spring-boot:run
```

## 阶段 0：环境与概念预热（已完成）

- API：`GET /api/products`、`GET /api/products/{id}`
- 端点：`/actuator/health`、`/actuator/metrics`
- 概念：[docs/learning/phase-0-concepts.md](docs/learning/phase-0-concepts.md)（若已创建）
- 计划：[docs/superpowers/plans/2026-06-26-phase-0-environment-warmup.md](docs/superpowers/plans/2026-06-26-phase-0-environment-warmup.md)

## 阶段 1：Micrometer + Prometheus 端点

### 场景

在部署 Prometheus（阶段 2）前，确认应用已暴露标准 Prometheus 文本指标，且带有 `application`、`environment` 公共标签。

### 验证 Prometheus 端点

```bash
curl -s http://localhost:8080/api/products/1
curl -s http://localhost:8080/actuator/prometheus | findstr /C:"jvm_memory_used_bytes" /C:"http_server_requests_seconds_count" /C:"application=\"metrics-learn\""
```

### 对比两种端点

```bash
# JSON（人读）
curl -s http://localhost:8080/actuator/metrics/http.server.requests

# Prometheus 文本（机器拉）
curl -s http://localhost:8080/actuator/prometheus | findstr http_server_requests_seconds_count
```

### 运行测试

```bash
mvn -pl metrics-learn-app test
```

### 概念学习

[docs/learning/phase-1-prometheus-endpoint.md](docs/learning/phase-1-prometheus-endpoint.md)

### 阶段 1 验收 Checklist

- [ ] `/actuator/prometheus` 返回 200 且 Content-Type 为 text/plain
- [ ] 响应含 `# HELP`、`# TYPE` 与样本行
- [ ] 可见 `jvm_memory_used_bytes`、`http_server_requests_seconds_count`
- [ ] 样本行含 `application="metrics-learn"`、`environment="local"`
- [ ] 能手工解读一行 HTTP 指标的标签与数值
- [ ] `mvn -pl metrics-learn-app test` 全部通过（9 个测试）
- [ ] 完成概念文档 4 道自检题

### 下一步

阶段 2：WSL Docker 部署 Prometheus，`scrape_configs` 拉取本端点。
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs(phase-1): add README section for prometheus endpoint"
```

---

## Task 5: 手动验收与指标解读练习

**Files:** 无（人工操作）

- [ ] **Step 1: 启动并产生多状态流量**

```bash
mvn -pl metrics-learn-app spring-boot:run
```

```bash
curl http://localhost:8080/api/products/1
curl http://localhost:8080/api/products/999
curl http://localhost:8080/api/products
```

- [ ] **Step 2: 导出并阅读 prometheus 文本**

```bash
curl -s http://localhost:8080/actuator/prometheus -o prometheus.txt
```

在 `prometheus.txt` 中找到一行 `http_server_requests_seconds_count`，填写：

| 字段 | 你的答案 |
|------|----------|
| 指标名 | |
| 至少 3 个标签（键=值） | |
| 数值 | |

- [ ] **Step 3: 对比 JSON 与 Prometheus 命名**

```bash
curl -s http://localhost:8080/actuator/metrics/http.server.requests
```

确认 JSON 端点使用 `http.server.requests`，而 prometheus 文本使用 `http_server_requests_seconds_*`。

- [ ] **Step 4: 完成自检题**

打开 `docs/learning/phase-1-prometheus-endpoint.md` 第 8 节，回答 4 道题（参考答案见第 9 节）。

- [ ] **Step 5: 在 README 阶段 1 Checklist 打勾**

---

## Spec 覆盖自检

| 设计文档阶段 1 要求 | 对应 Task |
|--------------------|-----------|
| 添加 `micrometer-registry-prometheus` | 已有；Task 1 说明 |
| 查看 `/actuator/prometheus` 原始输出 | Task 2 测试 + Task 5 手动 |
| 理解 `# HELP`、`# TYPE`、样本行 | Task 3 文档 + Task 5 练习 |
| 可见 `jvm_*`、`http_server_requests_*` | Task 2 断言 + Task 5 |
| 配置 `management.metrics.tags.application` | 已有；Task 1 增加 `environment` |
| 能读懂一行样本指标及其标签 | Task 3、Task 5 |

**范围外（刻意不做）：** `docker-compose` Prometheus、PromQL、`scrape_configs`、Grafana——属于阶段 2～3。

**占位符扫描：** 无 TBD / TODO / 省略实现。

---

## 预估耗时

| Task | 时间 |
|------|------|
| Task 1 配置 | 10～15 分钟 |
| Task 2 测试 | 25～35 分钟 |
| Task 3 概念文档 | 20～30 分钟 |
| Task 4 README | 10～15 分钟 |
| Task 5 手动验收 | 15～20 分钟 |
| **合计** | **约 1.5～2 小时** |

---

*Plan version: 1.0 · 2026-06-26*
