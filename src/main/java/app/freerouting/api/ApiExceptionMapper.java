package app.freerouting.api;

import static app.freerouting.util.gson.GsonProvider.GSON;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link jakarta.ws.rs.ext.ExceptionMapper} that catches any unhandled {@link Throwable}
 * thrown by a controller or filter and converts it into an HTTP 500 Internal Server Error response
 * with a JSON body containing an {@code "error"} field and a {@code "documentation"} link.
 *
 * <p>This mapper acts as the last line of defence: structured error cases (e.g. 404, 400) are
 * handled directly inside the controller methods or by {@link NotFoundExceptionMapper}; this mapper
 * covers unexpected runtime exceptions.
 *
 * <p>Client errors keep their 4xx status: a {@link WebApplicationException} raised by the JAX-RS
 * runtime (405, 406, 413, 415, ...) keeps its own status, a request body that is not valid JSON for
 * the expected type is a 400, and so is a missing or unparsable profile header ({@link
 * BaseController.MissingProfileException}).
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

  @Override
  @Produces(MediaType.APPLICATION_JSON)
  public Response toResponse(Throwable exception) {
    int status = statusOf(exception);
    String message =
        exception instanceof JsonParseException
            ? "The request body is not valid JSON for this endpoint."
            : exception.getMessage();
    JsonObject errorMessage = new JsonObject();
    errorMessage.addProperty("error", message);
    errorMessage.addProperty(
        "documentation",
        "https://github.com/freerouting/freerouting/blob/master/docs/API/API_v1.md");

    String prettyErrorMessage = GSON.toJson(errorMessage);

    return Response.status(status)
        .entity(prettyErrorMessage)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  private static int statusOf(Throwable exception) {
    if (exception instanceof WebApplicationException webException
        && webException.getResponse() != null) {
      return webException.getResponse().getStatus();
    }
    if (exception instanceof JsonParseException
        || exception instanceof BaseController.MissingProfileException) {
      return Response.Status.BAD_REQUEST.getStatusCode();
    }
    return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
  }
}
