package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import libconnect.models.Member;
import libconnect.models.User;

/** Tests session state transitions for the current application user. */
class SessionManagerTest {
    @Test
    void newSession_isLoggedOutAndHasNoCurrentUser() {
        SessionManager sessionManager = new SessionManager();

        assertFalse(sessionManager.isLoggedIn());
        assertNull(sessionManager.getCurrentUser());
    }

    @Test
    void login_storesUserAndMarksSessionAsLoggedIn() {
        SessionManager sessionManager = new SessionManager();
        User user = member("MEM-1");

        sessionManager.login(user);

        assertTrue(sessionManager.isLoggedIn());
        assertSame(user, sessionManager.getCurrentUser());
    }

    @Test
    void loginAgain_replacesTheExistingUser() {
        SessionManager sessionManager = new SessionManager();
        User firstUser = member("MEM-1");
        User secondUser = member("MEM-2");
        sessionManager.login(firstUser);

        sessionManager.login(secondUser);

        assertTrue(sessionManager.isLoggedIn());
        assertSame(secondUser, sessionManager.getCurrentUser());
        assertEquals("MEM-2", ((Member) sessionManager.getCurrentUser()).getMembershipId());
    }

    @Test
    void logout_clearsCurrentUserAndMarksSessionAsLoggedOut() {
        SessionManager sessionManager = new SessionManager();
        sessionManager.login(member("MEM-1"));

        sessionManager.logout();

        assertFalse(sessionManager.isLoggedIn());
        assertNull(sessionManager.getCurrentUser());
    }

    @Test
    void logoutWhenAlreadyLoggedOut_keepsSessionLoggedOut() {
        SessionManager sessionManager = new SessionManager();

        sessionManager.logout();

        assertFalse(sessionManager.isLoggedIn());
        assertNull(sessionManager.getCurrentUser());
    }

    private static Member member(String membershipId) {
        return new Member("USER-" + membershipId, "Alex Member", membershipId.toLowerCase()
                + "@example.com", "hash", membershipId);
    }
}
