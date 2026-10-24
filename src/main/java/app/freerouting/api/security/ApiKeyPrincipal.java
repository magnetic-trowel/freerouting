package app.freerouting.api.security;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.UUID;

/**
 * The caller of a request that an API key authenticated: the key and the profile the key is issued
 * to.
 *
 * <p>A provider may bind a key to one profile (the Google Sheets provider's optional {@code Profile
 * ID} column). A bound key acts only as that profile: a request that names another profile is
 * refused ({@link ProfileMismatchException}, HTTP 403), and a request that names none acts as the
 * key's profile. A key without a profile is unbound: the profile header decides, as without
 * authentication, and the data the key creates is still bound to the key (see {@code
 * Session.isOwnedBy}).
 *
 * @param apiKey the validated API key
 * @param profileId the profile the key is issued to, or {@code null} when the key is unbound
 */
public record ApiKeyPrincipal(String apiKey, UUID profileId) implements Principal {

  @Override
  public String getName() {
    return apiKey;
  }

  /**
   * Returns the profile of a request made with this key.
   *
   * @param requestedProfile the profile the request names in {@code Freerouting-Profile-ID}, or
   *     {@code null} when it names none
   * @return the key's profile when the key is bound, otherwise {@code requestedProfile}
   * @throws ProfileMismatchException when the key is bound to another profile
   */
  public UUID resolveProfile(UUID requestedProfile) {
    if (profileId == null) {
      return requestedProfile;
    }
    if (requestedProfile != null && !profileId.equals(requestedProfile)) {
      throw new ProfileMismatchException();
    }
    return profileId;
  }

  /**
   * Returns the profile of a request made with this key, from its {@code Freerouting-Profile-ID}
   * header value.
   *
   * @param profileIdHeader the header value, or {@code null}
   * @return the key's profile when the key is bound, otherwise the header's profile ({@code null}
   *     when the header is absent or not a UUID)
   * @throws ProfileMismatchException when the key is bound and the header names anything else
   */
  public UUID resolveProfile(String profileIdHeader) {
    if (profileIdHeader == null || profileIdHeader.isBlank()) {
      return resolveProfile((UUID) null);
    }
    UUID requested;
    try {
      requested = UUID.fromString(profileIdHeader.trim());
    } catch (IllegalArgumentException _) {
      if (profileId != null) {
        throw new ProfileMismatchException();
      }
      return null;
    }
    return resolveProfile(requested);
  }

  /** Returns a security context with this principal, for the request filters. */
  public SecurityContext asSecurityContext(boolean secure) {
    ApiKeyPrincipal principal = this;
    return new SecurityContext() {
      @Override
      public Principal getUserPrincipal() {
        return principal;
      }

      @Override
      public boolean isUserInRole(String role) {
        return true;
      }

      @Override
      public boolean isSecure() {
        return secure;
      }

      @Override
      public String getAuthenticationScheme() {
        return "BEARER";
      }
    };
  }

  /** Keeps the key itself out of logs. */
  @Override
  public String toString() {
    return "ApiKeyPrincipal[profileId=" + profileId + "]";
  }
}
