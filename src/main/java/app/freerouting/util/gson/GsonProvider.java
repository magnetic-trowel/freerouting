package app.freerouting.util.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

/** Shared Gson instance configured for Freerouting API and settings serialization. */
public final class GsonProvider {

  public static final Gson GSON =
      new GsonBuilder()
          .setPrettyPrinting()
          .disableHtmlEscaping()
          .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
          .registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
          .registerTypeAdapter(Path.class, new PathTypeAdapter())
          .registerTypeAdapter(Float.class, new FiniteFloatAdapter())
          .registerTypeAdapter(float.class, new FiniteFloatAdapter())
          .registerTypeAdapter(Double.class, new FiniteDoubleAdapter())
          .registerTypeAdapter(double.class, new FiniteDoubleAdapter())
          .registerTypeAdapterFactory(new RouterSettingsTypeAdapterFactory())
          .setStrictness(Strictness.LENIENT)
          .create();

  private GsonProvider() {}

  /**
   * Writes a float in its shortest exact form, so that it reads back as the same value (KiCad JSON
   * coordinates, settings such as clearance_tolerance_um). A value that JSON cannot represent is
   * written as null.
   */
  private static final class FiniteFloatAdapter extends TypeAdapter<Float> {
    @Override
    public void write(JsonWriter out, Float value) throws IOException {
      if (value == null || !Float.isFinite(value)) {
        out.nullValue();
      } else {
        out.value(value.floatValue());
      }
    }

    @Override
    public Float read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return (float) in.nextDouble();
    }
  }

  /** Writes a double in its shortest exact form; see {@link FiniteFloatAdapter}. */
  private static final class FiniteDoubleAdapter extends TypeAdapter<Double> {
    @Override
    public void write(JsonWriter out, Double value) throws IOException {
      if (value == null || !Double.isFinite(value)) {
        out.nullValue();
      } else {
        out.value(value.doubleValue());
      }
    }

    @Override
    public Double read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return in.nextDouble();
    }
  }
}
