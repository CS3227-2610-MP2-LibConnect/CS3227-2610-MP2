# CS3227-2610-MP2

## Build and test

LibConnect targets Java SE 25 and uses Maven with JUnit 5.

```bash
mvn test
```

The librarian implementation follows the layered architecture in [`PLAN.md`](PLAN.md).
Librarian services depend on repository and cross-role integration interfaces, so the
member-role implementation can be connected without changing librarian business rules.

## Temporary CLI

Run the interactive librarian CLI with:

```bash
mvn compile exec:java -Dexec.mainClass=libconnect.cli.Main
```

Start with `seed`, then `login e1`, and type `help` to see the available commands.
The CLI uses real librarian JSON persistence and temporary in-memory member, book,
book-copy, and loan adapters for manual integration testing.

## JavaFX desktop GUI

Run the simple librarian desktop GUI with:

```bash
mvn javafx:run
```

On a clean data directory, select `Prepare demo account` and then log in with
employee ID `e1`. The GUI uses the same librarian services and JSON persistence as
the CLI while the member-role providers remain temporary integration adapters.

## UI test pipeline

The default test command runs unit tests and excludes JavaFX integration tests:

```bash
mvn test
```

Run the complete suite, including TestFX UI integration tests, in a virtual display:

```bash
xvfb-run --auto-servernum mvn -Pui-tests test
```

The same command runs automatically on every push and pull request through the
GitHub Actions workflow in `.github/workflows/ui-tests.yml`.
