*** ** * ** ***

### Introduction to the Wait-Completion System

### The Challenge of Asynchrony in Integration Testing

Modern high-complexity Java applications are rarely linear. The use of reactive libraries, event-driven engines (such as
Apache MINA or QuickFix/J), thread pools, and messaging systems transforms a business scenario into a process
distributed over time.  
For an automation engineer, this creates a classic dilemma: **the test doesn't know when the application has truly
finished its work.** Traditional solutions---"magic pauses" (`Thread.sleep()`) or database/log polling loops---make the
test suite slow, unstable (flaky), and difficult to maintain.

### Philosophy and Approach: Instrumentation Instead of Guesswork

The **Wait-Completion** system offers a fundamentally different approach. Instead of observing external symptoms of the
application's work (database output or logs), the framework monitors the **internal life of the JVM** using dynamic
bytecode instrumentation powered by the **ByteBuddy** library.  
The core idea of the system is that any asynchronous activity in Java always has a tangible manifestation in the code.
This could be:

1. The creation of a task descriptor object (e.g., `CompletableFuture` or `Thread`).
2. The execution of a specific worker method (worker loop).
3. The accumulation of tasks in a service's internal queue.

The **Wait-Completion** system allows you to declaratively (via annotations) describe these points in the application
code. The framework "injects" invisible sensors into them that track the lifecycle of each task in real-time.

### Key Advantages

* **Absolute Precision:** The test resumes execution at the exact moment (down to the millisecond) when the last
  background task is completed. No more "buffer" periods of 5 seconds "just in case."
* **Application Code Purity:** You don't need to modify the main application code, add test hooks, or make methods
  public. Instrumentation happens "on the fly" in memory when the test starts.
* **Powerful DSL Engine:** Thanks to the built-in expression engine, you can flexibly configure filtering. For
  example: "wait for the completion of the `process()` method, but only if the first argument is of type `Order` and
  the `amount > 1000`."
* **Test Isolation:** The system is designed to track activity only within the current test context. Global registries
  and counters are reset between tests, preventing cross-test interference.
* **Garbage Collector Friendly:** The use of **WeakReferences** ensures that monitoring objects does not lead to memory
  leaks or prevent the natural cleanup of resources in the application.

### How it Works (Briefly)

The system consists of a set of specialized wait strategies. When a test starts, the **WaitCompletionManager** scans the
annotations on the test class. Each annotation is a contract that says: *"Treat everything that falls under this
Pointcut as asynchronous work."*  
During the test execution, the framework keeps track of all "open" tasks. When the test reaches the verification phase,
it invokes the `waitCompletion()` method, which blocks the test thread until the internal counters and activity
indicators show that the application has fully reached a state of rest.

*** ** * ** ***

### Defining Interception Points: Pointcut and When

Every wait annotation in the system (`@FutureLikeAwait`, `@MethodCountingAwait`, etc.) relies on two attributes:

* **pointcut** — which classes and methods to instrument (variables `t` and `m`);
* **when** — which calls count as tracked activity (variables `o` and `a`).

```text
pointcut = "t.inherits('com.app.BaseProcessor') && m.name('process')"
when     = "o.isSameClass(com.app.CriticalProcessor.class)"
```

The full syntax, the function reference (`t`, `m`, `o`, `a`), the empty filter rule, and performance guidance live on a
separate page: [Expression DSL](Expression-DSL.md).

*** ** * ** ***

### Overview of Wait Strategies

The choice of strategy depends on how your application signals that a task is complete: through a returned object,
through the duration of a method execution, or through the state of an internal queue.

### 1. Future-Like Strategy (`@FutureLikeAwait`)

**Purpose:** Waiting for objects that act as "task descriptors."

* **Principle:** The framework intercepts the moment an object is created (constructor) or obtained (factory method). A
  reference to the object is stored in the tracker. During the wait phase, the framework invokes a blocking method on
  every captured object.
* **When to choose:** If the application returns a `CompletableFuture`, `Thread`, `IoFuture` (Apache MINA), or any
  custom `Task` that has methods like `join()`, `get()`, or `await()`.
* **Specific Parameters:**
    * `await`: A DSL expression to call the blocking method (e.g., `o.call('join')`).
    * `awaitConsumer`: A class implementation for complex waiting logic (if a single-line expression is not enough).

*** ** * ** ***

### 2. Method Counting Strategy (`@MethodCountingAwait`)

**Purpose:** Tracking "event-driven" tasks that do not provide a return object.

* **Principle:** An "in-flight" counter mechanism. Entering the target method increments an atomic counter by `+1`,
  while exiting (including exits due to exceptions) decrements it by `-1`. The test waits until the counter returns
  to `0`.
* **When to choose:** For message handlers, event listeners, or workers where a task is considered complete exactly when
  the handler method finishes execution.
* **Key Detail:** Requires mandatory method filtering in the `pointcut` to avoid turning every single method call in the
  application into a "task."
* **Example:** Waiting for the `onMessage(Msg m)` method to finish processing.

*** ** * ** ***

### 3. Method Pair Strategy (`@MethodPairAwait`)

**Purpose:** Linking two separate events (methods) into a single logical operation.

* **Principle:** A "shared balance" mechanism. You specify two pointcuts: one for the **start** of the task and another
  for its **completion** . They operate on the same shared counter. The start pointcut performs `+1`, and the end
  pointcut performs `-1`.
* **When to choose:** When the task lifecycle is complex: for example, a request is sent from one component, but the
  response is processed in another, and there is no direct link through an object between them.
* **Specific Parameters:**
    * `startPointcut` / `startWhen`: Conditions for the task start.
    * `endPointcut` / `endWhen`: Conditions for the task completion.
* **Example:** `Gateway.sendRequest()` (`+1`) and `ResponseManager.onAck()` (`-1`).

*** ** * ** ***

### 4. Queue-Like Strategy (`@QueueLikeAwait`)

**Purpose:** Monitoring services that maintain internal queues or buffers.

* **Principle:** Active "Polling." The framework discovers the service and periodically invokes a function that returns
  the number of remaining tasks. The wait lasts until the sum of tasks across all discovered services reaches zero.
* **When to choose:** If the service stores tasks internally (task queues, thread pools) and provides a method to
  retrieve the current size (e.g., `getQueueSize()`).
* **Safety:** Services are stored via **WeakReference**, allowing the GC to remove them if the application no longer
  needs them.
* **Specific Parameters:**
    * `size`: A DSL expression to retrieve the task count (e.g., `o.call('size').asInt()`).

*** ** * ** ***

### Strategy Selection Matrix

| Scenario in Code                                 | Recommended Strategy | Completion Logic                   |
|--------------------------------------------------|----------------------|------------------------------------|
| Handled via `Future`, `Thread`, or `Task` object | **Future-Like**      | Blocking on a method of the object |
| Task is defined by method execution time         | **Method Counting**  | Method exit (return/exception)     |
| Start and End are different methods              | **Method Pair**      | Balance between Start/End calls    |
| Service has an internal queue/pool               | **Queue-Like**       | Polling `size() == 0`              |

*** ** * ** ***