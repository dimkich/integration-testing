# Integration Testing Framework Documentation

[Русская версия](../ru/README.md)

A high-level framework for Spring Boot integration testing that allows you to define test scenarios declaratively in
XML/JSON with full control over the environment (Database, Time, Mocks, and Asynchrony).

## Core Features

* **Declarative Testing**: Define tests in external files without re-compiling Java code.
* **State Management**: Automatic "dirty table" tracking and intelligent SQL cleanup/reload.
* **Asynchrony Synchronization**: 4 built-in strategies to wait for background tasks (Wait-Completion).
* **Time Manipulation**: Full JVM-wide system time mocking via `@MockJavaTime`.
* **Advanced Mocking**: Support for static methods, constructors, and recording real interactions.

## Documentation Sections

### Basics

* **[Quick Start](Quick-Start.md)** — Run your first test in 5 minutes.
* **[Test Hierarchy](Test-Hierarchy.md)** — Understanding Container, Case, and Part roles (bean/method or inboundMessage
  triggers).
* **[TestSetupModule Configuration](TestSetupModule.md)** — Type registration, aliases, and cloning setup.
* **[Test Initialization](Initialization.md)** — Environment setup: Time, DB, Caches, and Mocks.

### Redis

* **[Redis Overview](redis/README.md)** — `@EnableTestRedis` setup, XML examples, data types.
* **[Data Types](redis/Data-Types.md)** — Initialization, `dataStorageDiff`, XML templates for Hash, List, Set, ZSet,
  Stream, HyperLogLog.
* **[Configuration](redis/Configuration.md)** — Connections, Deep Merge, Longest-Prefix Match,
  full `application-test.yml`.
* **[Binary Envelopes](redis/Binary-Envelopes.md)** — Envelope structure,
  tags `{CONTENT}`, `{LEN}`, `{VER}`, `{TS}`, `{CRC32}`, `{FIX}`, `{STR}`, custom tags.
* **[TTL & Time Shift](redis/TTL-Time-Shift.md)** — Virtual time, hash-field TTL (Redis 7.4+), step-by-step scenario.
* **[Troubleshooting](redis/Troubleshooting.md)** — `Method not found`, `No handler`, `Redis Sync Timeout`,
  empty `dataStorageDiff`.
* **[Extensibility](redis/Extensibility.md)** — Custom codec/schema/data access, `BinarySegment` tags, stream & event
  handlers, `TestSetupModule.addSubTypes()`.

### Tools & Integration

* **[IntelliJ IDEA Integration](IDEA-Plugin.md)** — **(New!)** Manage tests via the IDEA Plugin, Visual Diff tool, and
  Live Coding mode.
* **[Mocks in Integration Tests](Mocks.md)** — Using `@TestBeanMock`, `@TestConstructorMock`, and `@TestStaticMock`.
* **[Time Mocking](MockJavaTime.md)** — Deterministic time control with `@MockJavaTime`.

### Advanced Features

* **[Wait-Completion System](wait-completion.md)** — Handling asynchrony in reactive and event-driven stacks.
* **[Hooks and Converters](Hooks-and-Converters.md)** — Extending behavior via `BeforeTest`, `AfterTest`,
  and `TestConverter`.
* **[JsonMapAsEntries Annotation](JsonMapAsEntries.md)** — Advanced collection mapping for XML/JSON.
* **[ResettableIterator Mechanism](ResettableIterator.md)** — Reusable iterators for logging and multiple assertions.

---
[← Back to Main](../../README.md)