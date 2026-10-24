package app.freerouting.api.security;

/**
 * The request names a profile other than the one its API key is issued to. It is answered with 403
 * Forbidden: the key is valid, but not for that profile.
 */
public final class ProfileMismatchException extends SecurityException {
  private static final long serialVersionUID = 1L;

  /** Creates the exception with the message the client sees. */
  public ProfileMismatchException() {
    super(
        "The API key is issued to another profile than the one in the Freerouting-Profile-ID"
            + " header. Send the key's own profile ID, or omit the header.");
  }
}
