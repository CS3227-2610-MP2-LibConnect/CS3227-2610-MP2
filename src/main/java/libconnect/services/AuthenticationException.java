package libconnect.services;

/** Signals that an authentication attempt was rejected. */
public class AuthenticationException extends ServiceException {
    /**
     * Creates an authentication exception with a user-displayable message.
     *
     * @param message the reason that authentication was rejected.
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
