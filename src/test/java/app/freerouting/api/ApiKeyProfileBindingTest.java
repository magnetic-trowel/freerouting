package app.freerouting.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.freerouting.api.security.ApiKeyPrincipal;
import app.freerouting.api.security.ProfileMismatchException;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A key issued to a profile acts only as that profile; an unbound key and disabled authentication
 * keep the header's profile.
 */
class ApiKeyProfileBindingTest {

  private final UUID keyProfile = UUID.randomUUID();

  /** Exposes the protected authenticateUser. */
  private static final class Probe extends BaseController {
    UUID caller() {
      return authenticateUser();
    }
  }

  private static UUID callerWith(ApiKeyPrincipal principal, String profileHeader) {
    Probe probe = new Probe();
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getHeaderString("Freerouting-Profile-ID")).thenReturn(profileHeader);
    probe.setHttpHeaders(headers);
    if (principal != null) {
      probe.setSecurityContext(principal.asSecurityContext(false));
    }
    return probe.caller();
  }

  @Test
  void boundKeyAcceptsItsOwnProfile() {
    ApiKeyPrincipal key = new ApiKeyPrincipal("key", keyProfile);
    assertEquals(keyProfile, callerWith(key, keyProfile.toString()));
  }

  @Test
  void boundKeyActsAsItsProfileWhenNoHeaderIsSent() {
    ApiKeyPrincipal key = new ApiKeyPrincipal("key", keyProfile);
    assertEquals(keyProfile, callerWith(key, null));
  }

  @Test
  void boundKeyRefusesAnotherProfileWith403() {
    ApiKeyPrincipal key = new ApiKeyPrincipal("key", keyProfile);
    ProfileMismatchException mismatch =
        assertThrows(
            ProfileMismatchException.class, () -> callerWith(key, UUID.randomUUID().toString()));
    assertThrows(ProfileMismatchException.class, () -> callerWith(key, "not-a-uuid"));
    assertEquals(403, new ApiExceptionMapper().toResponse(mismatch).getStatus());
  }

  @Test
  void unboundKeyAndDisabledAuthenticationUseTheHeader() {
    UUID header = UUID.randomUUID();
    assertEquals(header, callerWith(new ApiKeyPrincipal("key", null), header.toString()));
    assertEquals(header, callerWith(null, header.toString()));
    assertThrows(
        BaseController.MissingProfileException.class,
        () -> callerWith(new ApiKeyPrincipal("key", null), null));
  }

  @Test
  void webSocketRuleIsTheSame() {
    ApiKeyPrincipal bound = new ApiKeyPrincipal("key", keyProfile);
    assertEquals(keyProfile, bound.resolveProfile((String) null));
    assertThrows(
        ProfileMismatchException.class, () -> bound.resolveProfile(UUID.randomUUID().toString()));
    assertNull(new ApiKeyPrincipal(null, null).resolveProfile("not-a-uuid"));
  }
}
