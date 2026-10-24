package app.freerouting.api.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.freerouting.Freerouting;
import app.freerouting.api.security.ApiKeyPrincipal;
import app.freerouting.api.security.ApiKeyProvider;
import app.freerouting.api.security.ApiKeyValidationResult;
import app.freerouting.settings.GlobalSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** The MCP key filter passes the key's profile on, as the REST filter does. */
class McpApiKeyProfileTest {

  private static final String KEY = "11111111-1111-1111-1111-111111111111";
  private final UUID profile = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
    Freerouting.globalSettings.mcpServerSettings.authentication.isEnabled = true;
    McpApiKeyValidationService.resetForTesting();
    McpApiKeyValidationService.getInstance()
        .getProviders()
        .add(
            new ApiKeyProvider() {
              @Override
              public ApiKeyValidationResult validateApiKey(String apiKey) {
                return KEY.equals(apiKey)
                    ? ApiKeyValidationResult.ACCESS_GRANTED
                    : ApiKeyValidationResult.UNDECIDED;
              }

              @Override
              public UUID getProfileId(String apiKey) {
                return profile;
              }

              @Override
              public void refresh() {}

              @Override
              public String getProviderName() {
                return "test";
              }
            });
  }

  @AfterEach
  void tearDown() {
    McpApiKeyValidationService.resetForTesting();
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void theMcpPrincipalCarriesTheKeysProfile() throws Exception {
    ContainerRequestContext request = request("Bearer " + KEY);

    new McpApiKeyValidationFilter().filter(request);

    ArgumentCaptor<SecurityContext> context = ArgumentCaptor.forClass(SecurityContext.class);
    verify(request).setSecurityContext(context.capture());
    verify(request, never()).abortWith(any(Response.class));
    ApiKeyPrincipal principal = (ApiKeyPrincipal) context.getValue().getUserPrincipal();
    assertEquals(profile, principal.profileId());
    assertEquals(profile, principal.resolveProfile((String) null));
  }

  @Test
  void unknownKeyIsRefused() throws Exception {
    ContainerRequestContext request = request("Bearer 22222222-2222-2222-2222-222222222222");

    new McpApiKeyValidationFilter().filter(request);

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(request).abortWith(response.capture());
    assertEquals(401, response.getValue().getStatus());
  }

  private static ContainerRequestContext request(String authorization) {
    ContainerRequestContext request = mock(ContainerRequestContext.class);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getPath()).thenReturn("v1/mcp");
    when(request.getUriInfo()).thenReturn(uriInfo);
    when(request.getHeaderString("Authorization")).thenReturn(authorization);
    return request;
  }
}
