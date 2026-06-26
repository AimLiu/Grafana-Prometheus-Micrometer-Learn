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