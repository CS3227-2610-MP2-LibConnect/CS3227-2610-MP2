package libconnect.integration;

import java.util.List;

/** Defines the member-management boundary owned by the member-role developer. */
public interface MemberManagement extends MemberDirectory {
    /** Registers a member using the shared member-domain implementation. */
    void registerMember(String memberId, String name, String email);

    /** Registers a member using the supplied plaintext password. */
    default void registerMember(String memberId, String name, String email, String password) {
        registerMember(memberId, name, email);
    }

    /** Registers a member with an automatically generated membership ID. */
    default void registerMemberWithGeneratedId(String name, String email, String password) {
        registerMember(name, email, password);
    }

    /** Edits a member using the shared member-domain implementation. */
    void editMember(String memberId, String name, String email);

    /** Deactivates a member using the shared member-domain implementation. */
    void deactivateMember(String memberId);

    /** Activates a member using the shared member-domain implementation. */
    default void activateMember(String memberId) {
        throw new UnsupportedOperationException("Member activation is not supported");
    }

    /** Resets a member's password using the shared member-domain implementation. */
    default void resetMemberPassword(String memberId, String newPassword) {
        throw new UnsupportedOperationException("Member password reset is not supported");
    }

    /** Searches members using the shared member-domain implementation. */
    List<MemberSummary> searchMembers(String query);
}
