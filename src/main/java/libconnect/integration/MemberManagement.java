package libconnect.integration;

import java.util.List;

/** Defines the member-management boundary owned by the member-role developer. */
public interface MemberManagement extends MemberDirectory {
    /** Registers a member using the shared member-domain implementation. */
    void registerMember(String memberId, String name, String email);

    /** Edits a member using the shared member-domain implementation. */
    void editMember(String memberId, String name, String email);

    /** Deactivates a member using the shared member-domain implementation. */
    void deactivateMember(String memberId);

    /** Searches members using the shared member-domain implementation. */
    List<MemberSummary> searchMembers(String query);
}
