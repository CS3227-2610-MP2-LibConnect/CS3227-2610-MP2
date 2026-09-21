package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifies that the Java 25 build and test toolchain are configured. */
class BootstrapTest {
    /** Verifies that the smoke test executes successfully. */
    @Test
    void smokeTest_testRunnerExecutes_success() {
        assertEquals(25, Runtime.version().feature());
    }
}
