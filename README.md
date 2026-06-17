# SeqFlow

SeqFlow 是一个面向 Spring Boot 应用的分布式 ID 生成 Starter，基于数据库号段模式实现高性能 ID 分配。

它通过一次性从数据库申请一段连续 ID，并在应用内存中完成后续分配，避免每次生成 ID 都访问数据库。SeqFlow 支持按业务标签隔离不同编号序列，适用于订单号、流水号、业务单号、分库分表主键等场景。

## SeqFlow 解决什么问题

在分布式系统中，业务编号通常需要同时满足唯一、趋势递增、可读和高性能。UUID、雪花算法、数据库自增都能生成 ID，但在订单号、流水号、业务单号等场景下，可能存在编号过长、不可读、机器号配置复杂、数据库压力大或多业务序列难隔离等问题。

SeqFlow 基于数据库号段模式，一次申请一段 ID，在本地内存中分配，减少数据库访问次数，并支持按 `bizTag` 维护独立序列。

## 适用场景

- 订单号、支付单号、出入库单号等业务编号
- 多业务线独立维护编号序列
- 需要趋势递增的数据库主键或业务 ID
- 希望在 Spring Boot 项目中快速接入发号能力的场景

## 不适用场景

- 极致高并发且完全不能依赖数据库的场景
- 必须使用雪花算法结构的场景
- 要求 ID 完全连续、不能接受号段浪费的场景
- 不希望引入数据库表作为协调点的纯本地生成场景
- 对安全性要求较高、不能暴露递增规律的公开 ID 场景

## 方案对比

### 为什么不用 UUID

UUID 简单、无中心依赖，但通常较长、不可读、无趋势递增特性。作为数据库主键可能影响索引写入性能，作为业务编号也不便于人工识别和排查。

SeqFlow 更适合需要短编号、趋势递增和业务可读性的场景。

### 为什么不用雪花算法

雪花算法性能高、可本地生成，但通常需要维护机器 ID、数据中心 ID，并处理时钟回拨问题。对于很多业务系统来说，这会增加部署和运维复杂度。

SeqFlow 使用数据库作为号段协调点，更容易理解和接入，也更适合按业务维度维护独立编号。

### 为什么不用数据库自增

数据库自增简单可靠，但每生成一个 ID 都依赖一次数据库写入，性能容易受数据库影响。多业务编号、批量生成、格式化业务单号等场景下也不够灵活。

SeqFlow 仍然使用数据库保证号段唯一性，但通过批量申请号段、本地内存分配，在可靠性和性能之间取得平衡。

## 核心特性

- 基于数据库号段模式生成 ID，降低数据库访问频率
- 支持按 `bizTag` 维护独立 ID 序列
- 内置本地号段缓存，提升高并发场景下的 ID 获取性能
- 提供 Spring Boot 自动配置能力，引入依赖后即可使用
- 支持自动初始化数据库表
- 支持纯数字 ID、固定长度补零 ID、日期前缀业务编号等多种格式

## 工作原理

```text
应用调用 SeqFlow.nextId("order")
        ↓
根据 bizTag 获取本地号段缓存
        ↓
如果当前号段未耗尽，直接从内存返回 ID
        ↓
如果当前号段已耗尽，访问数据库递增 max_id
        ↓
加载新的号段到内存，继续发号
```

SeqFlow 的核心思想是号段预分配。比如 `step = 100` 时，每次访问数据库会为某个 `bizTag` 申请 100 个连续 ID，应用随后在本地内存中完成这 100 个 ID 的分配。只有当前号段耗尽时，才会再次访问数据库申请新的号段。

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.github.lyssl</groupId>
    <artifactId>seqFlow</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 配置数据源

SeqFlow 依赖数据库保存每个业务标签的当前最大 ID。使用前请确保 Spring Boot 应用中已经配置 `DataSource`。

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 配置 SeqFlow

```yaml
id-generator:
  auto-init-table: true
  table-name: sys_id_generator
  step: 100
```

### API 示例
以上步骤完成后直接在项目中使用。

```java
// 生成纯数字 ID
long id = SeqFlow.nextId("order");

// 生成指定长度的补零 ID，例如 000000000123
String formattedId = SeqFlow.nextId("order", 12);

// 生成带业务标签和日期前缀的业务编号，例如 order202606170001
String bizNo = SeqFlow.nextBizNo("order");

// 生成指定日期格式和指定流水号长度的业务编号
String dateBizNo = SeqFlow.nextBizNo("order", "yyyyMMdd", 6);

// 生成业务标签 + 固定长度流水号，例如 order000000000123
String pureBizNo = SeqFlow.nextPureBizNo("order", 12);
```

## 配置说明

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `id-generator.auto-init-table` | `boolean` | `true` | 是否在应用启动时自动创建发号表 |
| `id-generator.table-name` | `String` | `sys_id_generator` | 发号表名称 |
| `id-generator.step` | `Integer` | `100` | 每次从数据库申请的号段长度 |

## 数据库表结构

当 `id-generator.auto-init-table=true` 时，SeqFlow 会自动创建如下表结构：

```sql
CREATE TABLE IF NOT EXISTS sys_id_generator (
    biz_tag VARCHAR(64) NOT NULL PRIMARY KEY,
    max_id BIGINT NOT NULL DEFAULT '1',
    step INT NOT NULL DEFAULT '1000',
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `biz_tag` | 业务标签，用于区分不同业务类型的 ID 序列 |
| `max_id` | 当前业务标签已申请到的最大 ID |
| `step` | 号段步长 |
| `description` | 业务说明 |

## 核心类说明

| 类名 | 说明 |
| --- | --- |
| `SeqFlow` | 对外入口类，提供静态 ID 生成方法 |
| `IdSegmentManager` | 号段管理器，负责本地缓存和 ID 分配 |
| `IdAllocator` | 号段分配器，负责向数据库申请新的号段 |
| `IdSegmentRepository` | 发号表访问接口 |
| `JdbcIdSegmentRepository` | 基于 Spring JDBC 的发号表访问实现 |
| `IdGeneratorAutoConfiguration` | Spring Boot 自动配置类 |
| `IdGeneratorDatabaseInitializer` | 数据库表初始化组件 |

## 当前限制

- 当前版本主要面向 Spring Boot 和 JDBC 场景
- 默认使用方式依赖 Spring 容器完成自动配置和组件注入
- 当前版本采用本地号段缓存机制，在号段耗尽时申请新的号段
- 生产环境建议根据并发量和可接受的号段浪费情况调整 `step`
- 如果关闭自动建表，需要提前创建与配置项一致的发号表


## License

SeqFlow 使用 Apache License 2.0 开源协议。
