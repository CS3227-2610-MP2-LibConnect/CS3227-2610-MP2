# CS3227-2610-MP2

## Build and test

LibConnect targets Java SE 25 and uses Maven with JUnit 5.

```bash
mvn test
```

The librarian implementation follows the layered architecture in [`PLAN.md`](PLAN.md).
Librarian services depend on repository and cross-role integration interfaces, so the
member-role implementation can be connected without changing librarian business rules.
