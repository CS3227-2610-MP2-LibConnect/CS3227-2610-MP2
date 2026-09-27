# CS3227-2610-MP2

## LibConnect

LibConnect is a Java desktop application for library inventory and loan
management. The project uses Java SE 25, Maven, and JUnit 5.

### Development commands

Use Maven for development on Windows, macOS, and Linux:

```text
mvn clean verify
mvn test
mvn exec:java
```

The `exec:java` goal opens the initial LibConnect desktop window. The
application entry point is `libconnect.ui.LibConnectLauncher`.

The librarian implementation follows the layered architecture in [`PLAN.md`](PLAN.md).
Librarian services depend on repository and cross-role integration interfaces, so the
member-role implementation can be connected without changing librarian business rules.
