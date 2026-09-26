package libconnect.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/** Represents a librarian account used by librarian-facing operations. */
public final class Librarian implements libconnect.storage.repositories.Identifiable {
    private final String employeeId;
    private final String name;
    private final String email;
    private final AccountStatus status;

    /** Creates a librarian with the supplied identity and account status. */
    public Librarian(String employeeId, String name, String email, AccountStatus status) {
        this.employeeId = requireText(employeeId, "employeeId");
        this.name = requireText(name, "name");
        this.email = requireText(email, "email");
        this.status = Objects.requireNonNull(status, "status");
    }

    /** Returns the stable employee identifier. */
    public String getEmployeeId() {
        return employeeId;
    }

    /** Returns the librarian's display name. */
    public String getName() {
        return name;
    }

    /** Returns the librarian's email address. */
    public String getEmail() {
        return email;
    }

    /** Returns the current account status. */
    public AccountStatus getStatus() {
        return status;
    }

    /** Returns whether this librarian may perform protected operations. */
    @JsonIgnore
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /** Returns a copy with the supplied account status. */
    public Librarian withStatus(AccountStatus newStatus) {
        return new Librarian(employeeId, name, email, newStatus);
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
