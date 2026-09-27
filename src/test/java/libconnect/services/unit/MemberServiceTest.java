package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.AccountStatus;
import libconnect.models.Librarian;
import libconnect.models.Member;
import libconnect.services.MemberService;
import libconnect.services.NotFoundException;
import libconnect.services.ServiceException;
import libconnect.services.UserService;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileLibrarianRepository;
import libconnect.storage.file.FileMemberRepository;

/** Tests member-account behavior implemented by {@link MemberService}. */
class MemberServiceTest {
    @TempDir
    Path temporaryDirectory;

    private FileMemberRepository memberRepository;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = new FileMemberRepository(temporaryDirectory.resolve("members.json"));
        memberService = new MemberService(memberRepository);
    }

    @Test
    void registerMember_generatesIdsAndStoresHashedPassword() {
        memberService.registerMember("Ada", "ada@example.com", "secret-password");

        Member member = memberRepository.findByEmail("ada@example.com").orElseThrow();
        assertTrue(member.getUserId().startsWith("USER-"));
        assertTrue(member.getMembershipId().startsWith("MEMBER-"));
        assertTrue(member.getPasswordHash().startsWith("PBKDF2WithHmacSHA256$210000$"));
        assertNotEquals("secret-password", member.getPasswordHash());
        assertEquals(AccountStatus.ACTIVE, member.getStatus());
    }

    @Test
    void registerMember_duplicateEmailIgnoringCase_throwsServiceException() {
        memberService.registerMember("Ada", "ada@example.com", "secret-password");

        assertThrows(ServiceException.class,
                () -> memberService.registerMember("Another", " ADA@EXAMPLE.COM ", "another"));
    }

    @Test
    void registerMember_emailUsedByLibrarian_throwsServiceException() {
        FileLibrarianRepository librarianRepository = new FileLibrarianRepository(
                new StorageManager(temporaryDirectory), temporaryDirectory.resolve("librarians.json"));
        librarianRepository.save(new Librarian("USER-1", "EMP-1", "Ada", "ada@example.com",
                "password-hash", AccountStatus.ACTIVE));
        memberService = new MemberService(memberRepository, new UserService(memberRepository, librarianRepository));

        assertThrows(ServiceException.class,
                () -> memberService.registerMember("Ada", "ada@example.com", "secret-password"));
    }

    @Test
    void registerMember_emailUsedByLibrarianIgnoringCase_throwsServiceException() {
        FileLibrarianRepository librarianRepository = new FileLibrarianRepository(
                new StorageManager(temporaryDirectory), temporaryDirectory.resolve("librarians.json"));
        librarianRepository.save(new Librarian("USER-1", "EMP-1", "Ada", "ada@example.com",
                "password-hash", AccountStatus.ACTIVE));
        memberService = new MemberService(memberRepository, new UserService(memberRepository, librarianRepository));

        assertThrows(ServiceException.class,
                () -> memberService.registerMember("Ada", " ADA@EXAMPLE.COM ", "secret-password"));
    }

    @Test
    void updateMemberProfile_sameEmail_updatesName() {
        memberService.registerMember("Ada", "ada@example.com", "secret-password");
        Member member = memberRepository.findByEmail("ada@example.com").orElseThrow();

        memberService.updateMemberProfile(member.getMembershipId(), "Ada Lovelace", "ada@example.com");

        Member updatedMember = memberRepository.findByMembershipId(member.getMembershipId()).orElseThrow();
        assertEquals("Ada Lovelace", updatedMember.getName());
        assertEquals("ada@example.com", updatedMember.getEmail());
    }

    @Test
    void updateMemberProfile_emailOwnedByAnotherMember_throwsServiceException() {
        memberService.registerMember("Ada", "ada@example.com", "secret-password");
        memberService.registerMember("Grace", "grace@example.com", "another-password");
        Member ada = memberRepository.findByEmail("ada@example.com").orElseThrow();

        assertThrows(ServiceException.class,
                () -> memberService.updateMemberProfile(ada.getMembershipId(), "Ada", "grace@example.com"));
    }

    @Test
    void updatePassword_replacesPasswordHash() {
        memberService.registerMember("Ada", "ada@example.com", "old-password");
        Member member = memberRepository.findByEmail("ada@example.com").orElseThrow();
        String originalHash = member.getPasswordHash();

        memberService.updatePassword(member.getMembershipId(), "new-password");

        String updatedHash = memberRepository.findByMembershipId(member.getMembershipId()).orElseThrow()
                .getPasswordHash();
        assertTrue(updatedHash.startsWith("PBKDF2WithHmacSHA256$210000$"));
        assertNotEquals(originalHash, updatedHash);
    }

    @Test
    void deactivateAndActivateMember_persistAccountStatus() {
        memberService.registerMember("Ada", "ada@example.com", "secret-password");
        String membershipId = memberRepository.findByEmail("ada@example.com").orElseThrow().getMembershipId();

        memberService.deactivateMember(membershipId);
        assertEquals(AccountStatus.INACTIVE,
                memberRepository.findByMembershipId(membershipId).orElseThrow().getStatus());

        memberService.activateMember(membershipId);
        assertEquals(AccountStatus.ACTIVE,
                memberRepository.findByMembershipId(membershipId).orElseThrow().getStatus());
    }

    @Test
    void deleteMember_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> memberService.deleteMember("MEMBER-unknown"));
    }

    @Test
    void memberLookups_findExistingMemberAndReportUnknownValues() {
        memberService.registerMember("Ada", "ada@example.com", "secret-password");
        Member member = memberRepository.findByEmail("ada@example.com").orElseThrow();

        assertTrue(memberService.hasMemberWithEmail("ada@example.com"));
        assertTrue(memberService.findMemberByEmail("ada@example.com").equals(member));
        assertThrows(NotFoundException.class,
                () -> memberService.findMemberByEmail("unknown@example.com"));
        assertFalse(memberService.hasMemberWithEmail("unknown@example.com"));
    }

    @Test
    void deleteMember_existingMember_removesMember() {
        memberService.registerMember("Ada", "ada@example.com", "secret-password");
        String membershipId = memberRepository.findByEmail("ada@example.com").orElseThrow()
                .getMembershipId();

        memberService.deleteMember(membershipId);

        assertTrue(memberRepository.findByMembershipId(membershipId).isEmpty());
    }

    @Test
    void memberUpdates_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> memberService.updateMemberProfile("MEMBER-unknown", "Ada", "ada@example.com"));
        assertThrows(NotFoundException.class,
                () -> memberService.updatePassword("MEMBER-unknown", "new-password"));
        assertThrows(NotFoundException.class,
                () -> memberService.activateMember("MEMBER-unknown"));
        assertThrows(NotFoundException.class,
                () -> memberService.deactivateMember("MEMBER-unknown"));
    }

    @Test
    void registerMember_blankPassword_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> memberService.registerMember("Ada", "ada@example.com", "   "));
    }

    @Test
    void updatePassword_blankPassword_throwsIllegalArgumentException() {
        memberService.registerMember("Ada", "ada@example.com", "secret-password");
        String membershipId = memberRepository.findByEmail("ada@example.com").orElseThrow()
                .getMembershipId();

        assertThrows(IllegalArgumentException.class,
                () -> memberService.updatePassword(membershipId, "   "));
    }
}
