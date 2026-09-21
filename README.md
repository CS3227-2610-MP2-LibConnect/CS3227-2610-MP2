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
application entry point is `ui.LibConnectApplication`.
