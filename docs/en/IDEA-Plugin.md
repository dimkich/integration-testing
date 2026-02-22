IntelliJ IDEA Integration (Plugin Support)
==========================================

The **Integration Testing** plugin transforms IntelliJ IDEA into an interactive environment for test development and
recording. It automatically injects JVM arguments (-D...) into all **JUnit** and **TestNG** run configurations, allowing
you to manage test behavior visually without editing project configuration files.

## 1. Tool Window

Once installed, the **Integration Testing** tab appears in the IDEA tool window bar. All settings selected here are
applied "on the fly" to the next execution of any integration test in the project.

### Assertion Modes

Defines how test results are verified:

* **String** : Standard string comparison of expected and actual responses displayed in the JUnit console.
* **SaveActualData** : Recording mode. Assertions are skipped; the framework simply collects actual data from the code
  for initial XML population.
* **File** : Interactive Debug mode. If results do not match, IDEA opens the **Standard Visual Diff Tool** . You can
  visually transfer data from Actual to Expected. Use the **Save file assertion** button to persist these changes to the
  source file.
* **SingleFile** : Combines all failed tests from a single run into one large Diff window for mass editing.

### Repeat Modes

* **Once** : Standard single run.
* **UntilStopped** : Infinite execution loop. This is the primary mode for **Live Coding** new tests.

*** ** * ** ***

## 2. Live Test Development

The UntilStopped mode combined with **Debug** execution allows you to write tests on the fly without restarting the
Spring context.

### Technical Mechanism

In UntilStopped mode, the internal InfiniteTestIterator re-executes the init() method at the end of each cycle, which *
*physically re-reads the XML file from the disk** (testMapper.readAllTests()).

### Development Workflow:

* **Breakpoint** : Set a breakpoint in DynamicTestBuilder.java inside the InfiniteTestIterator.init() method on the
  line:

```
  Test test = testMapper.readAllTests();
```      

* **Run** : Start the test in **Debug** mode with repeat = UntilStopped and assertion = File.
* **Iteration** :
    * When the debugger hits the breakpoint, the system pauses before reading the file. Add a new <test type="Case">
      block to your XML (only the <bean>, <method>, and <request> are required).
    * Press **Resume (F9)** .
    * The framework reads the updated file and executes the new test immediately.
    * If the result in the opened Diff window is correct, move it to "Expected" and click the **Save file assertion**
      button in the plugin.
    * Repeat the process for the next test case without stopping the debug session.

*** ** * ** ***

## 3. Mock Management (Mockito)

The plugin dynamically changes the behavior of objects declared with @TestBeanMock, @TestConstructorMock, and
@TestStaticMock:

* **useMocks** : Enables mock mode (environment=mock). This allows running tests locally without starting heavy Docker
  containers, significantly speeding up development.
* **mockAlwaysCallRealMethods** : Forces all mocks to call real methods. Used to automatically harvest baseline data
  from a working system.
* **mockCallRealMethodsOnNoData** : If no <result> section is found in the XML for a specific call, the real method is
  executed. If data exists, the mocked value is returned.
* **mockReturnMockOnNoData** : Returns a "Deep Mock" (empty stub) if data is missing in the XML, helping to draft the
  test structure.
* **spyCreateData** : Automatically records the results of spy object calls and prepares them for XML storage.

*** ** * ** ***

## 4. Database Debugging (Hibernate)

Enables detailed SQL logs without modifying application.properties:

* **hibernateShowSql / FormatSql** : Prints and formats SQL queries in the console.
* **hibernateUseSqlComments** : Adds SQL comments to queries, helping to identify which method triggered the SQL.
* **hibernateShowBindParams** : Sets loggers to TRACE level to reveal the actual parameter values injected into the
  SQL (replaces ? with real values).

*** ** * ** ***

## 5. "Save file assertion" Button

This button performs a physical overwrite of the source XML test files in your src/test/resources folder.

**How it works:**   
The plugin takes the changes you manually transferred in the IDEA Diff window (which are stored as temporary files in
java.io.tmpdir/java_tests), matches them against the original file on disk (using helper files settings.txt and
template.xml), and patches the source code while preserving the original XML structure and indentation.

*** ** * ** ***

## 6. Technical Details

* **Automation** : Works as a JavaProgramPatcher to intercept JVM startup and append system properties.
* **Safety** : File writing is wrapped in WriteCommandAction, ensuring compatibility with IDEA's **Local History** and
  providing **Undo/Redo** support.
