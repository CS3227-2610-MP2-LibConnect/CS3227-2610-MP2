# CS3227-2610-MP2

## LibConnect

LibConnect is a Java desktop application for library inventory and loan
management. The project uses Java SE 25, Maven, and JUnit 5.

### Documentation

- [User guide](docs/UserGuide.md): installation, login, member and librarian
  workflows, examples, expected results, and build commands.
- [Developer guide](docs/DeveloperGuide.md): architecture, persistence,
  integration contracts, testing, coverage, and release preparation.

### Development commands

Use Maven with Java SE 25 on Windows, macOS, and Linux:

```text
mvn clean verify
mvn test
mvn exec:java
mvn -Prelease clean package
```

The `exec:java` goal opens the initial LibConnect desktop window. The application
entry point is `libconnect.ui.LibConnectLauncher`. The release profile creates
the self-contained executable `target/libconnect-1.0.0.jar` with its runtime
dependencies.

The librarian implementation follows the layered architecture in [`PLAN.md`](PLAN.md).
Librarian services depend on repository and cross-role integration interfaces, so the
member-role implementation can be connected without changing librarian business rules.
