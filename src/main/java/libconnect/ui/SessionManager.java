package libconnect.ui;

import libconnect.models.User;

/** Stores the user authenticated in the current application session. */
public final class SessionManager {
    private User currentUser;

    /**
     * Marks a user as authenticated for the current application session.
     *
     * @param user the authenticated user.
     */
    public void login(User user) {
        currentUser = user;
    }

    /**
     * Ends the current application session.
     */
    public void logout() {
        currentUser = null;
    }

    /**
     * Returns the user authenticated in the current session.
     *
     * @return the current user, or {@code null} if no user is authenticated.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns whether a user is authenticated in the current session.
     *
     * @return true if a user is logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
