package libconnect.integration;

/** Exposes the minimum member information required by librarian screens. */
public final class MemberSummary {
    private final String memberId;
    private final String name;
    private final String email;
    private final boolean active;

    /** Creates a member summary for cross-role display and search. */
    public MemberSummary(String memberId, String name, String email, boolean active) {
        this.memberId = requireText(memberId, "memberId");
        this.name = requireText(name, "name");
        this.email = requireText(email, "email");
        this.active = active;
    }

    /** Returns the stable member identifier. */
    public String getMemberId() {
        return memberId;
    }

    /** Returns the member's display name. */
    public String getName() {
        return name;
    }

    /** Returns the member's email address. */
    public String getEmail() {
        return email;
    }

    /** Returns whether the member account is active. */
    public boolean isActive() {
        return active;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
