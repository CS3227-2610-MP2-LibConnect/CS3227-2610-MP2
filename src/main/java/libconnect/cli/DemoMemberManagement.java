package libconnect.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import libconnect.integration.MemberManagement;
import libconnect.integration.MemberSummary;

/** Provides clearly temporary in-memory member data for manual CLI testing. */
public final class DemoMemberManagement implements MemberManagement {
    private final Map<String, MemberSummary> members = new LinkedHashMap<>();

    /** Creates demo member data with one active member. */
    /** Creates the temporary member directory used by manual librarian verification. */
    public DemoMemberManagement() {
        members.put("m1", new MemberSummary("m1", "Ada", "ada@example.com", true));
    }

    /** Returns whether a demo member exists. */
    @Override
    public boolean exists(String memberId) {
        return members.containsKey(memberId);
    }

    /** Returns whether a demo member is active. */
    @Override
    public boolean isActive(String memberId) {
        MemberSummary member = members.get(memberId);
        return member != null && member.isActive();
    }

    /** Registers a demo member in memory. */
    @Override
    public void registerMember(String memberId, String name, String email) {
        requireText(memberId, "memberId");
        if (members.containsKey(memberId)) {
            throw new IllegalStateException("Member already exists");
        }
        members.put(memberId, new MemberSummary(memberId, name, email, true));
    }

    /** Edits a demo member in memory. */
    @Override
    public void editMember(String memberId, String name, String email) {
        MemberSummary current = getRequired(memberId);
        members.put(memberId, new MemberSummary(memberId, name, email, current.isActive()));
    }

    /** Deactivates a demo member in memory. */
    @Override
    public void deactivateMember(String memberId) {
        MemberSummary current = getRequired(memberId);
        members.put(memberId, new MemberSummary(memberId, current.getName(), current.getEmail(), false));
    }

    /** Searches demo members by identifier, name, or email. */
    @Override
    public List<MemberSummary> searchMembers(String query) {
        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return members.values().stream()
                .filter(member -> normalizedQuery.isBlank()
                        || member.getMemberId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || member.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || member.getEmail().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
    }

    private MemberSummary getRequired(String memberId) {
        MemberSummary member = members.get(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Member does not exist");
        }
        return member;
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
