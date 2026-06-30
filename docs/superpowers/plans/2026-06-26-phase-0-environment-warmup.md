# 阶段 0：环境与概念预热 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过一个可运行的「商品查询 API」小场景，在 Windows IDEA + Java 21 环境中验证 Spring Boot Actuator 可用，并建立 Metrics / 指标类型 / Pull vs Push 的基础概念认知。

**Architecture:** 在已有 Maven 父工程 `grafana-prometheus-micrometer-learn` 下的 `metrics-learn-app` 子模块中，新增内存版商品查询 REST API；通过 Spring Boot Actuator 暴露 `/actuator/health` 与 `/actuator/metrics`；用 MockMvc 集成测试验证端点行为；配套学习文档与 README 阶段导航。本阶段**不启动** Prometheus / Grafana（留待阶段 2～3）。

**Tech Stack:** Java 21、Spring Boot 3.3.5、Maven、Spring Web、Spring Boot Actuator、Micrometer（随 Actuator 引入，本阶段仅通过 `/actuator/metrics` 观察）、JUnit 5、MockMvc、IntelliJ IDEA（Windows）

---

## 实际场景说明

### 业务故事

你负责维护一个名为 **metrics-learn** 的「商品目录查询服务」。运维同事在接入 Prometheus 之前，要求你先完成 **应用自检**：

1. 服务能否正常启动并报告 **健康状态**？
2. 对外 API 被调用后，能否在 **Actuator Metrics** 中看到 HTTP 相关指标？
3. 你能说清楚后续 Prometheus 将如何 **拉取** 这些指标（概念层面，本阶段不实际部署）？

### 本阶段要实现的 API

| 方法 | 路径 | 说明 | 响应 |
|------|------|------|------|
| GET | `/api/products` | 列出全部商品 | `200` + JSON 数组 |
| GET | `/api/products/{id}` | 按 ID 查询 | `200` + JSON；不存在则 `404` |

内置 3 条内存数据（无需数据库）：

| id | name | price |
|----|------|-------|
| 1 | Prometheus 入门手册 | 49.90 |
| 2 | Grafana 仪表盘实战 | 59.90 |
| 3 | Micrometer 埋点指南 | 39.90 |

### 阶段 0 结束时应达到的效果

| 维度 | 效果 |
|------|------|
| **运行** | IDEA 使用 JDK 21 启动 `MetricsLearnApplication`，控制台无报错，监听 `8080` |
| **健康检查** | 浏览器或 curl 访问 `http://localhost:8080/actuator/health` 返回 `{"status":"UP",...}` |
| **指标端点** | 访问 `http://localhost:8080/actuator/metrics` 返回指标名称列表；访问 `http://localhost:8080/actuator/metrics/jvm.memory.used` 有具体数值 |
| **业务联动** | 调用 `GET /api/products/1` 后，能在 metrics 中找到 `http.server.requests` |
| **自动化验证** | `mvn -pl metrics-learn-app test` 全部通过 |
| **概念** | 阅读 `docs/learning/phase-0-concepts.md` 并能回答文内 4 道自检题 |
| **范围边界** | 本阶段**不验收** Prometheus scrape、Grafana 面板（`/actuator/prometheus` 可存在但属于阶段 1 重点） |

### 当前仓库状态（增量起点）

以下**已存在**，本计划不重复创建：

- 根 `pom.xml`（父工程，Java 21 + Spring Boot 3.3.5）
- `metrics-learn-app/pom.xml`（含 web、actuator、test 依赖）
- `MetricsLearnApplication.java`
- `application.yml`（已暴露 health/info/metrics/prometheus）
- `ModuleLogPathEnvironmentPostProcessor` + `logback-spring.xml`

本计划**补齐**阶段 0 缺失部分：业务 API、Actuator 测试、学习文档、README、`.gitignore` 日志目录。

---

## 文件结构（本阶段新增/修改）

| 文件 | 职责 |
|------|------|
| `metrics-learn-app/src/main/java/com/metricslearn/domain/Product.java` | 商品 DTO（record） |
| `metrics-learn-app/src/main/java/com/metricslearn/service/ProductService.java` | 内存商品数据与查询逻辑 |
| `metrics-learn-app/src/main/java/com/metricslearn/web/ProductController.java` | REST 入口，产生 HTTP 指标 |
| `metrics-learn-app/src/main/resources/application.yml` | 阶段 0 学习期将 health 详情改为 `always` |
| `metrics-learn-app/src/test/java/com/metricslearn/web/ProductControllerTest.java` | 业务 API 测试 |
| `metrics-learn-app/src/test/java/com/metricslearn/actuator/ActuatorHealthEndpointTest.java` | 健康端点测试 |
| `metrics-learn-app/src/test/java/com/metricslearn/actuator/ActuatorMetricsEndpointTest.java` | 指标端点 + HTTP 指标联动测试 |
| `docs/learning/phase-0-concepts.md` | 概念说明与自检题 |
| `README.md` | 项目总览与阶段 0 操作指南 |
| `.gitignore` | 增加 `**/log/` 忽略模块日志目录 |

---

## Task 1: 商品领域模型与服务

**Files:**
- Create: `metrics-learn-app/src/main/java/com/metricslearn/domain/Product.java`
- Create: `metrics-learn-app/src/main/java/com/metricslearn/service/ProductService.java`

- [ ] **Step 1: 创建 Product record**

```java
package com.metricslearn.domain;

public record Product(Long id, String name, double price) {
}
```

- [ ] **Step 2: 创建 ProductService（内存数据）**

```java
package com.metricslearn.service;

import com.metricslearn.domain.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    private final Map<Long, Product> products = Map.of(
            1L, new Product(1L, "Prometheus 入门手册", 49.90),
            2L, new Product(2L, "Grafana 仪表盘实战", 59.90),
            3L, new Product(3L, "Micrometer 埋点指南", 39.90)
    );

    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }
}
```

- [ ] **Step 3: 编译验证**

Run:

```bash
cd D:\Project_Install\JAVA_Develop\Operations-And-Maintenance\Grafana-Prometheus-Micrometer-Learn
mvn -pl metrics-learn-app -q compile
```

Expected: exit code `0`，无 ERROR 输出。

- [ ] **Step 4: Commit**

```bash
git add metrics-learn-app/src/main/java/com/metricslearn/domain/Product.java
git add metrics-learn-app/src/main/java/com/metricslearn/service/ProductService.java
git commit -m "feat(phase-0): add in-memory product catalog service"
```

---

## Task 2: 商品查询 REST API

**Files:**
- Create: `metrics-learn-app/src/main/java/com/metricslearn/web/ProductController.java`
- Test: `metrics-learn-app/src/test/java/com/metricslearn/web/ProductControllerTest.java`

- [ ] **Step 1: 编写失败的 API 测试**

```java
package com.metricslearn.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listProductsShouldReturnThreeItems() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void getProductByIdShouldReturn200WhenExists() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Prometheus 入门手册"))
                .andExpect(jsonPath("$.price").value(49.90));
    }

    @Test
    void getProductByIdShouldReturn404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
mvn -pl metrics-learn-app -q test -Dtest=ProductControllerTest
```

Expected: FAIL（`ProductController` 不存在或 404）

- [ ] **Step 3: 实现 ProductController**

```java
package com.metricslearn.web;

import com.metricslearn.domain.Product;
import com.metricslearn.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> listProducts() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run:

```bash
mvn -pl metrics-learn-app -q test -Dtest=ProductControllerTest
```

Expected: `Tests run: 3, Failures: 0`

- [ ] **Step 5: Commit**

```bash
git add metrics-learn-app/src/main/java/com/metricslearn/web/ProductController.java
git add metrics-learn-app/src/test/java/com/metricslearn/web/ProductControllerTest.java
git commit -m "feat(phase-0): add product REST API with tests"
```

---

## Task 3: Actuator 健康端点

**Files:**
- Modify: `metrics-learn-app/src/main/resources/application.yml`
- Test: `metrics-learn-app/src/test/java/com/metricslearn/actuator/ActuatorHealthEndpointTest.java`

- [ ] **Step 1: 调整 application.yml（学习期显示 health 详情）**

将 `management.endpoint.health.show-details` 改为 `always`：

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
```

- [ ] **Step 2: 编写健康端点测试**

```java
package com.metricslearn.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldReturnUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
```

- [ ] **Step 3: 运行测试**

Run:

```bash
mvn -pl metrics-learn-app -q test -Dtest=ActuatorHealthEndpointTest
```

Expected: `Tests run: 1, Failures: 0`

- [ ] **Step 4: Commit**

```bash
git add metrics-learn-app/src/main/resources/application.yml
git add metrics-learn-app/src/test/java/com/metricslearn/actuator/ActuatorHealthEndpointTest.java
git commit -m "feat(phase-0): verify actuator health endpoint"
```

---

## Task 4: Actuator 指标端点与 HTTP 指标联动

**Files:**
- Test: `metrics-learn-app/src/test/java/com/metricslearn/actuator/ActuatorMetricsEndpointTest.java`

- [ ] **Step 1: 编写指标端点测试**

```java
package com.metricslearn.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorMetricsEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void metricsIndexShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray())
                .andExpect(jsonPath("$.names[?(@ == 'jvm.memory.used')]").exists());
    }

    @Test
    void jvmMemoryUsedMetricShouldHaveMeasurements() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"))
                .andExpect(jsonPath("$.measurements[0].value").isNumber());
    }

    @Test
    void httpServerRequestMetricShouldExistAfterApiCall() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics/http.server.requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("http.server.requests"));
    }
}
```

- [ ] **Step 2: 运行测试**

Run:

```bash
mvn -pl metrics-learn-app -q test -Dtest=ActuatorMetricsEndpointTest
```

Expected: `Tests run: 3, Failures: 0`

- [ ] **Step 3: 运行全部测试**

Run:

```bash
mvn -pl metrics-learn-app -q test
```

Expected: `Tests run: 7, Failures: 0`（3 + 1 + 3）

- [ ] **Step 4: Commit**

```bash
git add metrics-learn-app/src/test/java/com/metricslearn/actuator/ActuatorMetricsEndpointTest.java
git commit -m "test(phase-0): verify actuator metrics and HTTP metric linkage"
```

---

## Task 5: 概念学习文档

**Files:**
- Create: `docs/learning/phase-0-concepts.md`

- [ ] **Step 1: 创建概念文档（完整内容）**

```markdown
# 阶段 0：概念预热

## 1. 可观测性三支柱

| 支柱 | 回答的问题 | 本仓库阶段 |
|------|-----------|-----------|
| Metrics（指标） | 系统有多忙？有多快？错误率？ | 阶段 0～5 主线 |
| Logs（日志） | 某次请求发生了什么？ | `metrics-learn-app/log/`（Logback） |
| Traces（链路） | 一次请求经过了哪些服务？ | 后续扩展，不在 A 阶段主线 |

本阶段重点：**Metrics**。你已通过 `/actuator/metrics` 看到 JVM 内存、HTTP 请求等指标。

## 2. Pull vs Push

| 模式 | 谁主动 | 本仓库中的体现 |
|------|--------|---------------|
| **Pull（拉）** | 监控系统定期来应用取数据 | Prometheus 访问 `/actuator/prometheus`（阶段 1 深入） |
| **Push（推）** | 应用主动发到远端 | 本学习计划 A 阶段不采用 |

阶段 0 只需建立直觉：**应用负责暴露指标端点；Prometheus 负责来拉。**

## 3. 四种指标类型（Micrometer）

| 类型 | 行为 | 典型场景 | 阶段 0 在哪见过 |
|------|------|----------|----------------|
| **Counter** | 只增不减（重启归零） | 订单总数、错误次数 | `http.server.requests` 的 `count` |
| **Gauge** | 可升可降 | 队列长度、内存使用量 | `jvm.memory.used` |
| **Timer** | 记录耗时分布 | HTTP 延迟、方法耗时 | `http.server.requests`（含 bucket） |
| **DistributionSummary** | 非耗时类分布 | 请求体大小 | 阶段 4 业务埋点时深入 |

## 4. 标签（Labels）与基数

标签用于切分维度，例如 `method=GET`、`uri=/api/products/{id}`、`status=200`。

**基数（cardinality）** = 标签组合的唯一数量。若把 `productId` 作为标签，商品越多，时间序列爆炸，Prometheus 内存飙升。

**原则：** 低基数标签（method、status）✅；高基数标签（userId、orderId）❌。

## 5. 阶段 0 动手回顾

```bash
# 1. 启动应用（IDEA 或命令行）
mvn -pl metrics-learn-app spring-boot:run

# 2. 健康检查
curl http://localhost:8080/actuator/health

# 3. 指标列表
curl http://localhost:8080/actuator/metrics

# 4. 查看 JVM 内存
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 5. 产生 HTTP 流量后再查 HTTP 指标
curl http://localhost:8080/api/products/1
curl http://localhost:8080/actuator/metrics/http.server.requests
```

## 6. 自检题（请口头或书面回答）

1. Metrics / Logs / Traces 分别解决什么问题？本阶段为何从 Metrics 开始？
2. Counter 和 Gauge 有什么区别？各举一个本项目中见过的指标名。
3. Pull 和 Push 在 Prometheus 场景下分别是谁主动？阶段 0 应用暴露了哪个端点供后续 Pull？
4. 为什么不要把 `productId` 做成 Prometheus 标签？

## 7. 下一阶段预告

阶段 1 将聚焦 `/actuator/prometheus` 的文本格式，以及 Micrometer 如何把 Actuator 指标导出为 Prometheus 可拉取格式。
```

- [ ] **Step 2: Commit**

```bash
git add docs/learning/phase-0-concepts.md
git commit -m "docs(phase-0): add observability concepts and self-check questions"
```

---

## Task 6: README 与 .gitignore 收尾

**Files:**
- Create: `README.md`
- Modify: `.gitignore`

- [ ] **Step 1: 在 .gitignore 的日志区块追加一行**

在 `# Logs & runtime output` 小节中 `logs/` 下方添加：

```gitignore
**/log/
```

- [ ] **Step 2: 创建 README.md（完整内容）**

```markdown
# Grafana · Prometheus · Micrometer 学习项目

Java 21 + Spring Boot 3.x 渐进式学习仓库。设计文档见
[docs/superpowers/specs/2026-06-26-grafana-prometheus-micrometer-learning-design.md](docs/superpowers/specs/2026-06-26-grafana-prometheus-micrometer-learning-design.md)。

## 环境要求

| 项 | 版本 |
|----|------|
| JDK | 21 |
| Maven | 3.9+ |
| IDE | IntelliJ IDEA（推荐 Open 根目录） |
| 监控栈（阶段 2+） | WSL Docker Compose |

## 项目结构

```text
├── pom.xml                  # 父 POM
├── metrics-learn-app/       # Spring Boot 应用
├── docker/observability/    # 阶段 2 起：Prometheus + Grafana
└── docs/                    # 设计与学习文档
```

## 阶段 0：环境与概念预热

### 场景

商品目录查询服务 `metrics-learn-app` 上线前自检：验证 Actuator 健康与指标端点，并通过 API 调用产生可观测的 HTTP 指标。

### 启动

```bash
# 在仓库根目录
mvn -pl metrics-learn-app spring-boot:run
```

或在 IDEA 中运行 `com.metricslearn.MetricsLearnApplication`（JDK 21）。

### 验证 API

```bash
curl http://localhost:8080/api/products
curl http://localhost:8080/api/products/1
curl http://localhost:8080/api/products/999   # 期望 404
```

### 验证 Actuator

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### 运行测试

```bash
mvn -pl metrics-learn-app test
```

### 概念学习

阅读 [docs/learning/phase-0-concepts.md](docs/learning/phase-0-concepts.md) 并完成 4 道自检题。

### 阶段 0 验收 Checklist

- [ ] IDEA 使用 JDK 21 启动成功
- [ ] `/actuator/health` 返回 `UP`
- [ ] `/actuator/metrics` 可列出 `jvm.memory.used`
- [ ] 调用 `/api/products/1` 后可查到 `http.server.requests`
- [ ] `mvn -pl metrics-learn-app test` 全部通过
- [ ] 能回答概念文档中的 4 道自检题

### 下一步

进入阶段 1：深入 `/actuator/prometheus` 与 Micrometer 导出格式。见 `docs/superpowers/plans/`（待编写）。
```

- [ ] **Step 3: Commit**

```bash
git add .gitignore README.md
git commit -m "docs(phase-0): add README and ignore module log directory"
```

---

## Task 7: 手动验收（IDEA + 浏览器）

**Files:** 无（人工操作）

- [ ] **Step 1: IDEA 运行配置检查**

1. Open 根目录 `Grafana-Prometheus-Micrometer-Learn`
2. Project SDK = **21**
3. Run `MetricsLearnApplication`
4. 控制台出现 `Started MetricsLearnApplication`，无异常栈

- [ ] **Step 2: 浏览器验收**

| URL | 期望 |
|-----|------|
| http://localhost:8080/api/products | JSON 数组，3 条商品 |
| http://localhost:8080/actuator/health | `"status":"UP"` |
| http://localhost:8080/actuator/metrics | 含 `names` 数组 |
| http://localhost:8080/actuator/metrics/http.server.requests | 调用 API 后存在 |

- [ ] **Step 3: 完成概念自检**

打开 `docs/learning/phase-0-concepts.md` 第 6 节，回答 4 道题。参考答案要点：

1. Metrics 看趋势与聚合；Logs 看细节；Traces 看跨服务路径。先 Metrics 因与 Prometheus/Grafana 主线直接相关。
2. Counter 只增（如请求 count）；Gauge 可升降（如 `jvm.memory.used`）。
3. Pull = Prometheus 主动拉；Push = 应用推。阶段 0 暴露 `/actuator/metrics`（阶段 1 重点是 `/actuator/prometheus`）。
4. `productId` 高基数，会导致时间序列过多，Prometheus 内存暴涨。

- [ ] **Step 4: 在 README 阶段 0 Checklist 打勾（本地自用，不必 commit）**

---

## Spec 覆盖自检

| 设计文档阶段 0 要求 | 对应 Task |
|--------------------|-----------|
| 初始化 Git + Maven 骨架 | 已有；Task 6 README 说明 |
| 引入 Actuator | 已有 |
| 访问 `/actuator/health`、`/actuator/metrics` | Task 3、4 测试 + Task 7 手动 |
| 能说明 Counter/Gauge/Timer | Task 5 概念文档 + Task 7 自检 |
| 能说明 Pull vs Push | Task 5 概念文档 + Task 7 自检 |
| Java 21 + SB 3.x IDEA 启动 | Task 7 |

**范围外（刻意不做）：** Prometheus scrape、Grafana 面板、自定义业务 Counter、数据库/Redis——分别属于阶段 1～4。

**占位符扫描：** 无 TBD / TODO /「类似上文」省略。

---

## 预估耗时

| Task | 时间 |
|------|------|
| Task 1～2 商品 API | 30～45 分钟 |
| Task 3～4 Actuator 测试 | 30～45 分钟 |
| Task 5～6 文档 | 20～30 分钟 |
| Task 7 手动验收 | 15～20 分钟 |
| **合计** | **约 2～2.5 小时** |

---

*Plan version: 1.0 · 2026-06-26*
