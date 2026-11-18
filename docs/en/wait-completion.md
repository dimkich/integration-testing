# Wait-Completion System

[Russian version](../ru/wait-completion.md)

Wait-Completion waits for the application's background tasks without `Thread.sleep()` and without
modifying the application code. You describe with an annotation where work starts and ends, and the
framework holds the test until that work is finished.

## Enabling

The subsystem is always active whenever the framework is enabled; no separate property is needed.
Wait annotations are placed on a test class that already enables the framework
(`@IntegrationTesting` or a module annotation).

## Common attributes

* `pointcut` — which classes and methods to instrument (variables `t` and `m`);
* `when` — which calls count as tracked activity (variables `o` and `a`); default is `true`.

```text
pointcut = "t.inherits('com.app.BaseProcessor') && m.name('process')"
when     = "o.isSameClass(com.app.CriticalProcessor.class)"
```

The full syntax and the `t`, `m`, `o`, `a` functions are in [Expression DSL](Expression-DSL.md).
Keep `pointcut` narrow: instrumenting every application method slows the tests down.

## Choosing a strategy

| Situation in code                      | Strategy                    | When the task is considered complete     |
|----------------------------------------|-----------------------------|------------------------------------------|
| There is a task handle object          | `@FutureLikeAwait`          | A blocking method of the object returned |
| The task is a method execution         | `@MethodCountingAwait`      | The method exits                         |
| Start and end are different methods    | `@MethodPairAwait`          | Start/end calls are balanced             |
| The service has a queue or a counter   | `@QueueLikeAwait`           | The queue is empty                       |
| The application reacts to Redis events | `@RedisKeyspaceEventsAwait` | The handler has processed every event    |

### `@FutureLikeAwait`

For `CompletableFuture`, `Thread`, Apache MINA `IoFuture`, or a custom task class with a blocking
method (`join()`, `get()`, `await()`).

```java
@FutureLikeAwait(
        pointcut = "t.name('com.app.Task') && m.isConstructor()",
        await = "o.call('await')")
```

* `await` — a DSL call of the blocking method;
* `awaitConsumer` — a `Consumer<Object>` class for complex waiting; it needs a public no-args
  constructor.

### `@MethodCountingAwait`

The task is a method execution: entering the target method starts it, exiting (including via an
exception) finishes it. The test waits until all such tasks are done.

```java
@MethodCountingAwait(
        pointcut = "t.name('com.app.OrderHandler') && m.name('onMessage')")
```

Suitable for message handlers, listeners and workers. Always name a specific method in `pointcut`,
otherwise every call becomes a task.

### `@MethodPairAwait`

Links two different methods into one task: for example, a request leaves one component and the
response is processed in another.

```java
@MethodPairAwait(
        startPointcut = "t.name('com.app.Gateway') && m.name('send')",
        endPointcut = "t.name('com.app.ResponseHandler') && m.name('onAck')")
```

* `startPointcut` / `startWhen` — the start of the task;
* `endPointcut` / `endWhen` — the completion of the task.

`startWhen` and `endWhen` must be consistent: if start is counted but end is not, the counter
stays positive and the wait never completes.

### `@QueueLikeAwait`

For a service that knows the size of its queue or pool.

```java
@QueueLikeAwait(
        pointcut = "t.name('com.app.Worker') && m.isConstructor()",
        size = "o.call('getQueueSize').asInt()")
```

* `size` — a DSL expression returning an `Integer`;
* `sizeFunction` — a `Function<Object, Integer>` class for complex counting; it needs a public
  no-args constructor.

When `pointcut` has no method matcher, constructors of the matching type are instrumented.

### `@RedisKeyspaceEventsAwait` (Redis module)

For tests where the application reacts to Redis events: a TTL expiry, a key deletion, a value
write, and so on. The handler receives events asynchronously and may not finish before the checks.
The annotation replaces `Thread.sleep()`: the test continues when the handler has been called for
every event.

Key change notifications must be enabled in Redis (keyspace notifications,
`notify-keyspace-events`, for example `KEA`). If the required type is not enabled, Redis sends
nothing and the wait passes immediately without checking anything.

```java
@RedisKeyspaceEventsAwait(
        events = {"expired"},
        connection = "myRedis",
        pointcut = "t.name('com.app.ExpiredListener') && m.name('onMessage')")
```

* `events` — event names as used in `notify-keyspace-events`: `expired`, `del`, `set`, `hset`,
  `lpush`, `sadd`, `zadd`, `expire`, `persist`, `rename_from`, `rename_to`, and others;
* `pointcut` — the method that handles the notifications;
* `connection` — connection name from `integration.testing.redis.connections`; it can be omitted
  when only one connection is configured;
* `timeoutMs` — how long to wait for a handler call, `5000` by default.

Things to keep in mind:

* The wait counts handler calls, not keys: the handler must be called for every notification even
  when it ignores some keys inside. If the filtering happens before the method is called, the wait
  misses calls and fails with `Timed out waiting for keyspace handler calls`.
* One command may produce several notifications: `mset` of three keys — three `set` events,
  `rename` — `rename_from` and `rename_to`. The handler must be called as many times.
* Only notifications published during the current test action are counted; anything published
  earlier is not awaited again.
* If the handler is not called within `timeoutMs`, the test fails with a wait error instead of
  hanging.

The annotation is repeatable: different event types may be handled by different methods,
including on different connections.

## How it works

Before every test action (and before initialization) a new activity window opens. After the action
the coordinator runs all strategies in parallel; each waits until its subsystem is empty. If at
least one strategy reports activity, the loop repeats: an event in one subsystem may have caused a
cascade in another. When every strategy returns `false`, the test proceeds to assertions.

A wait error (a Kafka timeout, a dead Redis replicator) is propagated to the test thread immediately
instead of waiting for the global test timeout.

## Limitations

* A task must pass through an interception point. If a library creates tasks internally, add its
  class or package to `pointcut`.
* For `@MethodPairAwait`, keep `startWhen` and `endWhen` consistent.
* Do not put `pointcut` on the whole classpath: every matching call is instrumented.
* Waiting does not replace checking the response: the diff and `<response>` are still compared after
  the background work finishes.

---
[← Back to home](README.md)
