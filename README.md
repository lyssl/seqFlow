

# SeqFlow 分布式 ID 生成器

SeqFlow 是一个基于号段模式的分布式 ID 生成器，专为 Spring Boot 应用设计。通过预分配 ID 号段的方式，实现高性能、高可用的分布式 ID 生成服务。

## 核心特性

- **高性能**：采用双号段缓冲机制，内存中预加载 ID 区间，减少数据库访问压力
- **简单易用**：提供静态 API 方法，无需注入依赖，开箱即用
- **Spring Boot 原生支持**：提供完整的自动配置能力，无需额外代码编写
- **灵活配置**：支持自定义号段步长、表名、自动初始化等参数
- **多种 ID 格式**：支持纯数字 ID、格式化业务编号、带日期前缀的编号等多种输出格式

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.github.lyssl</groupId>
    <artifactId>seqFlow</artifactId>
    <version>最新版本</version>
</dependency>
```

### 配置数据源

SeqFlow 需要数据库支持，请确保应用中已配置 DataSource：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 配置参数

```yaml
id-generator:
  auto-init-table: true    # 是否自动创建 ID 表
  table-name: id_segment   # 存储号段的表名
  step: 1000               # 号段步长，每次从数据库获取的 ID 数量
```

## API 使用

SeqFlow 提供简洁的静态方法进行 ID 生成：

```java
// 生成纯数字 ID
long id = SeqFlow.nextId("order");

// 生成指定长度的纯数字 ID（不足前面补0）
String formattedId = SeqFlow.nextId("order", 12);

// 生成业务编号时间默认取yyyyMMdd 例子: order202002130001
String bizNo = SeqFlow.nextBizNo("order");

// 生成指定日期前缀的业务编号
String dateBizNo = SeqFlow.nextBizNo("order", "yyyyMMdd");

// 生成指定长度的纯业务编号
String pureBizNo = SeqFlow.nextPureBizNo("order", 12);
```

## 配置选项

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| auto-init-table | boolean | true | 是否自动创建 ID 号段表 |
| table-name | String | id_segment | 存储号段的数据库表名 |
| step | Integer | 1000 | 号段步长，每次从数据库获取的 ID 数量 |

## 数据库表结构

SeqFlow 会自动创建以下结构的表用于存储 ID 号段信息：

```sql
CREATE TABLE id_segment (
    biz_tag VARCHAR(64) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step INT NOT NULL,
    description VARCHAR(256)
);
```

- **biz_tag**：业务标识，用于区分不同业务类型的 ID
- **max_id**：当前号段的最大 ID 值
- **step**：号段步长
- **description**：业务描述

## 工作原理

SeqFlow 采用经典的号段模式实现分布式 ID 生成：

1. **预分配**：应用启动时或当前号段耗尽时，从数据库预获取一段 ID
2. **内存缓冲**：将获取的号段加载到内存缓冲区，双号段设计确保 ID 获取不间断
3. **高效分配**：直接从内存中分配 ID，仅在号段耗尽时访问数据库
4. **异步刷新**：后台线程提前加载下一号段，保证 ID 生成的连续性

## 核心类说明

| 类名 | 说明 |
|------|------|
| `SeqFlow` | 入口类，提供所有 ID 生成的静态方法 |
| `IdSegmentManager` | 号段管理器，负责号段的加载、缓存和分配 |
| `IdAllocator` | ID 分配器，与数据库交互获取新号段 |
| `JdbcIdSegmentRepository` | JDBC 实现的消息存取层 |
| `IdGeneratorAutoConfiguration` | Spring Boot 自动配置类 |

## 许可证

本项目采用 Apache2.0 许可证开源。