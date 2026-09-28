package libconnect.integration;

/** Provides the minimal member lookup contract required by librarian services. */
public interface MemberDirectory {
    /** Returns whether the member identifier exists. */
    boolean exists(String memberId);

    /** Returns whether the member account is active. */
    boolean isActive(String memberId);
}
