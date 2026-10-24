package app.freerouting.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The optional "Profile ID" column of the API key sheet. */
class GoogleSheetsProfileColumnTest {

  private static final String KEY_A = "11111111-1111-1111-1111-111111111111";
  private static final String KEY_B = "22222222-2222-2222-2222-222222222222";
  private static final String PROFILE = "33333333-3333-3333-3333-333333333333";

  @Test
  void profileCellBindsTheKeyAndEmptyCellLeavesItUnbound() {
    var table =
        GoogleSheetsApiKeyProvider.parseRows(
            List.of(
                List.of("API Key", "Access granted?", "Profile ID"),
                List.of(KEY_A, "Yes", PROFILE),
                List.of(KEY_B, "Yes", "")));

    assertEquals(UUID.fromString(PROFILE), table.keys().get(KEY_A).profileId());
    assertNull(table.keys().get(KEY_B).profileId());
    assertEquals(2, table.grantedCount());
  }

  @Test
  void sheetWithoutTheColumnKeepsWorkingWithUnboundKeys() {
    var table =
        GoogleSheetsApiKeyProvider.parseRows(
            List.of(List.of("API Key", "Access granted?"), List.of(KEY_A, "Yes")));

    assertTrue(table.keys().get(KEY_A).granted());
    assertNull(table.keys().get(KEY_A).profileId());
  }

  @Test
  void rowWithInvalidProfileIsSkippedRatherThanGrantedUnbound() {
    var table =
        GoogleSheetsApiKeyProvider.parseRows(
            List.of(
                List.of("Profile ID", "API Key", "Access granted?"),
                List.of("someone@example.com", KEY_A, "Yes")));

    assertFalse(table.keys().containsKey(KEY_A));
    assertEquals(1, table.skippedRows());
  }

  @Test
  void theServiceReturnsTheProfileOfTheGrantingProvider() {
    ApiKeyProvider bound =
        provider(ApiKeyValidationResult.ACCESS_GRANTED, UUID.fromString(PROFILE));
    ApiKeyProvider unbound = provider(ApiKeyValidationResult.ACCESS_GRANTED, null);
    ApiKeyProvider undecided = provider(ApiKeyValidationResult.UNDECIDED, UUID.randomUUID());

    assertEquals(
        UUID.fromString(PROFILE),
        ApiKeyValidationService.authenticate(List.of(undecided, bound), KEY_A).profileId());
    assertNull(ApiKeyValidationService.authenticate(List.of(unbound, bound), KEY_A).profileId());
    assertNull(ApiKeyValidationService.authenticate(List.of(undecided), KEY_A));
  }

  private static ApiKeyProvider provider(ApiKeyValidationResult result, UUID profile) {
    return new ApiKeyProvider() {
      @Override
      public ApiKeyValidationResult validateApiKey(String apiKey) {
        return result;
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
    };
  }
}
