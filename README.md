# CS3227-2610-MP2

## LibConnect

LibConnect is a Java desktop application for library inventory and loan
management. The project uses Java SE 25, Gradle, and JUnit 5.

### Development commands

On Windows, use the Gradle wrapper:

```text
gradlew.bat build
gradlew.bat test
gradlew.bat run
```

On macOS or Linux, use:

```text
./gradlew build
./gradlew test
./gradlew run
```

The `run` task currently opens the initial LibConnect desktop window. The
application entry point is `libconnect.ui.LibConnectApplication`.

<!-- TODO: Update once whole application is merged -->
-------- (above jh, below librarian) TODO: merge to be resolved later ---------

## Build and test

LibConnect targets Java SE 25 and uses Maven with JUnit 5.

```bash
mvn test
```

The librarian implementation follows the layered architecture in [`PLAN.md`](PLAN.md).
Librarian services depend on repository and cross-role integration interfaces, so the
member-role implementation can be connected without changing librarian business rules.

## Temporary CLI

Until the JavaFX view is wired, run the interactive librarian CLI with:

```bash
mvn compile exec:java -Dexec.mainClass=libconnect.cli.Main
```

Start with `seed`, then `login e1`, and type `help` to see the available commands.
The CLI uses real librarian JSON persistence and temporary in-memory member, book,
book-copy, and loan adapters for manual integration testing.
