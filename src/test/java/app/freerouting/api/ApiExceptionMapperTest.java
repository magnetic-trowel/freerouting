package app.freerouting.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonSyntaxException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/** Client errors that reach the catch-all mapper keep a 4xx status instead of becoming a 500. */
class ApiExceptionMapperTest {

  private final ApiExceptionMapper mapper = new ApiExceptionMapper();

  @Test
  void webApplicationExceptionKeepsItsStatus() {
    assertEquals(415, mapper.toResponse(new NotSupportedException()).getStatus());
  }

  @Test
  void malformedJsonIsABadRequestWithoutParserDetails() {
    Response response =
        mapper.toResponse(new JsonSyntaxException("Expected BEGIN_OBJECT at line 1 column 2"));

    assertEquals(400, response.getStatus());
    assertFalse(String.valueOf(response.getEntity()).contains("BEGIN_OBJECT"));
  }

  @Test
  void missingProfileIsABadRequest() {
    Response response =
        mapper.toResponse(new BaseController.MissingProfileException("profile header missing"));

    assertEquals(400, response.getStatus());
    assertTrue(String.valueOf(response.getEntity()).contains("profile header missing"));
  }

  @Test
  void unexpectedExceptionIsAnInternalError() {
    assertEquals(500, mapper.toResponse(new IllegalStateException("boom")).getStatus());
  }
}
