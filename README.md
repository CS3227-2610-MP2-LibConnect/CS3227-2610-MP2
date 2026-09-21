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

Until the JavaFX view is wired, run the interactive librarian CLI with:

```bash
mvn compile exec:java -Dexec.mainClass=libconnect.cli.Main
```

Start with `seed`, then `login e1`, and type `help` to see the available commands.
The CLI uses real librarian JSON persistence and temporary in-memory member, book,
book-copy, and loan adapters for manual integration testing.
