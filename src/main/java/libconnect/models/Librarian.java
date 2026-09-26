package libconnect.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents a librarian account used by librarian-facing operations. */
public final class Librarian extends User {
    private final String employeeId;

    /** Creates a librarian with the supplied identity and account status. */
    public Librarian(String userId, String employeeId, String name, String email, String passwordHash, AccountStatus status) {
        this.employeeId = requireText(employeeId, "employeeId");
        super(userId, name, email, passwordHash);
    }

    /** Returns the stable employee identifier. */
    public String getEmployeeId() {
        return employeeId;
    }

    /** Returns whether this librarian may perform protected operations. */
    @JsonIgnore
    public boolean isActive() {
        return super.getStatus() == AccountStatus.ACTIVE;
    }

    /** Returns a copy with the supplied account status. */
    public Librarian withStatus(AccountStatus newStatus) {
        return new Librarian(super.getUserId(), employeeId, super.getName(), super.getEmail(), super.getPasswordHash(), newStatus);
    }

    /** Returns the stable identifier used by repositories. */
    @Override
    @JsonProperty("employeeId")
    public String getId() {
        return employeeId;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
