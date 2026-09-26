package libconnect.integration;

/** Defines book-copy incident recording owned by the member-role developer. */
public interface BookCopyManagement {
    /** Marks a copy as damaged. */
    void recordDamagedBook(String copyId);

    /** Marks a copy as lost. */
    void recordLostBook(String copyId);
}
