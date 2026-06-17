# SeqFlow

SeqFlow is a distributed ID generation starter for Spring Boot applications. It uses the database segment allocation pattern to generate high-performance, trend-increasing IDs.

Instead of visiting the database for every generated ID, SeqFlow allocates a continuous ID segment from the database and then serves IDs from local memory. Different business sequences are isolated by `bizTag`, making it suitable for order numbers, transaction numbers, workflow numbers, and database primary keys.

## What Problem Does SeqFlow Solve?

In distributed systems, business IDs often need to be unique, trend-increasing, readable, and efficient. UUID, Snowflake, and database auto-increment can all generate IDs, but they may introduce long unreadable IDs, machine ID configuration, high database pressure, or difficulty in isolating multiple business sequences.

SeqFlow uses the database segment allocation pattern. It allocates a batch of IDs from the database and serves them from local memory, reducing database access while supporting independent sequences by `bizTag`.

## Use Cases

- Order numbers, payment numbers, inventory document numbers, and other business identifiers
- Independent ID sequences for multiple business domains
- Trend-increasing IDs for database primary keys or business records
- Spring Boot applications that need a lightweight ID generation component

## Unsuitable Use Cases

- database dependency is unacceptable
- Scenarios that require a Snowflake-style ID structure
- Scenarios that require perfectly continuous IDs and cannot tolerate segment waste
- Pure local ID generation without a database coordination table
- Public-facing IDs where exposing increasing patterns is not acceptable

## Comparison With Other Approaches

### Why Not UUID?

UUID is simple and decentralized, but it is usually long, unreadable, and not trend-increasing. It may hurt database index locality and is not convenient for business troubleshooting.

SeqFlow is more suitable when shorter, trend-increasing, and readable business IDs are needed.

### Why Not Snowflake?

Snowflake is fast and can generate IDs locally, but it usually requires machine ID management, data center ID management, and clock rollback handling, which increases deployment complexity.

SeqFlow uses the database as the segment coordination point, making it easier to adopt and better suited for business-specific sequences.

### Why Not Database Auto-Increment?

Database auto-increment is simple and reliable, but every generated ID depends on a database write. It is also less flexible for multiple business sequences, batch generation, and formatted business numbers.

SeqFlow still uses the database to guarantee uniqueness, but allocates IDs in batches and serves them from memory, balancing reliability and performance.

## Features

- Database segment-based ID allocation
- Independent ID sequence per `bizTag`
- Local segment cache to reduce database access
- Spring Boot auto-configuration
- Optional database table initialization
- Multiple output formats, including numeric IDs, zero-padded IDs, and date-prefixed business numbers

## How It Works

```text
Application calls SeqFlow.nextId("order")
        |
        v
Load the local segment cache for the bizTag
        |
        v
Return the next ID directly from memory if the segment still has capacity
        |
        v
If the segment is exhausted, increment max_id in the database
        |
        v
Load the new segment into memory and continue generating IDs
```

The core idea is segment pre-allocation. For example, when `step = 100`, SeqFlow requests 100 continuous IDs from the database for a specific `bizTag`. The application then allocates those IDs locally from memory. The database is accessed again only when the current segment is exhausted.

## Benchmark

The following benchmark was run in a local environment and is for reference only. Actual performance depends on CPU, JDK, database, network, connection pool, and the configured `step`.

Test conditions:

- Threads: 100
- Total IDs: 1,000,000
- ID uniqueness check: enabled
- Duplicate IDs: 0
- Failed requests: 0

| step | DB segment loads | elapsed | TPS | success | unique | duplicate |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 100 | 10,000 | 118.335s | 8,450.60/s | 1,000,000 | 1,000,000 | 0 |
| 1,000 | 1,000 | 14.563s | 68,666.14/s | 1,000,000 | 1,000,000 | 0 |
| 10,000 | 100 | 2.273s | 439,901.49/s | 1,000,000 | 1,000,000 | 0 |

The result shows that a larger `step` reduces database segment allocation frequency and improves throughput. In production, choose `step` based on concurrency, acceptable segment waste, and database pressure.

## Quick Start

### Add Dependency

```xml
<dependency>
    <groupId>io.github.lyssl</groupId>
    <artifactId>seqFlow</artifactId>
    <version>0.1.1</version>
</dependency>
```

### Configure DataSource

SeqFlow uses a database table to store the current maximum ID for each business tag. Make sure your Spring Boot application has a `DataSource` configured.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Configure SeqFlow

```yaml
id-generator:
  auto-init-table: true
  table-name: sys_id_generator
  step: 100
```

### API Examples

```java
// Generate a numeric ID
long id = SeqFlow.nextId("order");

// Generate a zero-padded ID, for example 000000000123
String formattedId = SeqFlow.nextId("order", 12);

// Generate a date-prefixed business number, for example order202606170001
String bizNo = SeqFlow.nextBizNo("order");

// Generate a date-prefixed business number with a custom date format and sequence length
String dateBizNo = SeqFlow.nextBizNo("order", "yyyyMMdd", 6);

// Generate bizTag + zero-padded sequence, for example order000000000123
String pureBizNo = SeqFlow.nextPureBizNo("order", 12);
```

## Configuration

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `id-generator.auto-init-table` | `boolean` | `true` | Whether to create the ID table automatically on application startup |
| `id-generator.table-name` | `String` | `sys_id_generator` | ID table name |
| `id-generator.step` | `Integer` | `100` | Segment size requested from the database each time |

## Database Table

When `id-generator.auto-init-table=true`, SeqFlow creates the following table automatically:

```sql
CREATE TABLE IF NOT EXISTS sys_id_generator (
    biz_tag VARCHAR(64) NOT NULL PRIMARY KEY,
    max_id BIGINT NOT NULL DEFAULT '1',
    step INT NOT NULL DEFAULT '1000',
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Field description:

| Field | Description |
| --- | --- |
| `biz_tag` | Business tag used to isolate different ID sequences |
| `max_id` | Maximum ID already allocated for the business tag |
| `step` | Segment size |
| `description` | Business description |

## Core Classes

| Class | Description |
| --- | --- |
| `SeqFlow` | Public entry point that provides static ID generation methods |
| `IdSegmentManager` | Manages local segment cache and ID allocation |
| `IdAllocator` | Allocates new segments from the database |
| `IdSegmentRepository` | Repository interface for the ID table |
| `JdbcIdSegmentRepository` | Spring JDBC implementation of the repository |
| `IdGeneratorAutoConfiguration` | Spring Boot auto-configuration |
| `IdGeneratorDatabaseInitializer` | Database table initialization component |

## Current Limitations

- The current version mainly targets Spring Boot and JDBC use cases.
- The default usage depends on the Spring container for auto-configuration and component injection.
- The current implementation requests a new segment when the local segment is exhausted.
- In production, tune `step` based on concurrency requirements and acceptable segment waste.
- If automatic table initialization is disabled, create the ID table manually before startup.

## License

SeqFlow is released under the Apache License 2.0.
