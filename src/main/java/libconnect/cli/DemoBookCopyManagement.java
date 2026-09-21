package libconnect.cli;

import java.util.HashMap;
import java.util.Map;

import libconnect.integration.BookCopyManagement;

/** Provides temporary in-memory book-copy incident tracking for the CLI. */
final class DemoBookCopyManagement implements BookCopyManagement {
    private final Map<String, String> statuses = new HashMap<>();

    /** Marks a demo copy as damaged. */
    @Override
    public void recordDamagedBook(String copyId) {
        statuses.put(requireText(copyId), "DAMAGED");
    }

    /** Marks a demo copy as lost. */
    @Override
    public void recordLostBook(String copyId) {
        statuses.put(requireText(copyId), "LOST");
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("copyId must not be blank");
        }
        return value;
    }
}
