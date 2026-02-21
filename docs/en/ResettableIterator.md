*** ** * ** ***

### ResettableIterator Mechanism

In standard Java, an `Iterator` is a "one-time use" object. In integration testing, this poses a challenge: if the
library logs an object (serializes it) before the test execution, the iterator becomes exhausted, and the application
code receives no data.  
The `ResettableIterator` mechanism within this library addresses this issue by enabling **multiple serializations** of
the same data.

### 1. Key Features

* **Automatic Wrapping** : When deserializing (reading JSON/XML) data into an `Iterator` type, the library stores the
  elements in an internal list and returns a `ResettableIterator` implementation.
* **Automatic Reset** : The serializer automatically invokes the `.reset()` method before and after writing data to the
  stream.
* **Data Persistence**: You can serialize the same object containing an iterator as many times as needed (e.g., for
  logging, passing to MockMvc, and using in test assertions) --- the data will always be available from the beginning.

### 2. Usage Example

If your DTO contains an iterator:  
java

    public class MyTestDto {
        private Iterator<String> data;
    }

When this object is processed by the library's infrastructure, the `data` field will hold an iterator that does not "
expire" after its first traversal.

*** ** * ** ***