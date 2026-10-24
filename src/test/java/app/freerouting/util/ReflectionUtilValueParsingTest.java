package app.freerouting.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.settings.AutorouterSettings;
import app.freerouting.settings.FanoutSettings;
import app.freerouting.settings.LayerSettings;
import app.freerouting.settings.OptimizerScoreSettings;
import app.freerouting.settings.OptimizerSettings;
import app.freerouting.settings.RouterScoreSettings;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.RoutingCostSettings;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReflectionUtilValueParsingTest {

  /** Settings classes merged by {@link ReflectionUtil#copyFields} in the router settings merge. */
  private static final List<Class<?>> ROUTER_SETTINGS_CLASSES =
      List.of(
          RouterSettings.class,
          AutorouterSettings.class,
          FanoutSettings.class,
          OptimizerSettings.class,
          RoutingCostSettings.class,
          RouterScoreSettings.class,
          OptimizerScoreSettings.class,
          LayerSettings.class);

  /** Holder with primitive fields whose initializers differ from the Java defaults. */
  public static class PrimitiveHolder {
    public boolean enabled = true;
    public int timeout = 5;
    public double tolerance = 1.0;
  }

  @Test
  void booleanValuesAcceptCommonSpellings() throws Exception {
    for (String value : new String[] {"true", "TRUE", " 1 ", "yes", "on", "On"}) {
      RouterSettings settings = new RouterSettings();
      ReflectionUtil.setFieldValue(settings, "optimizer.enabled", value);
      assertTrue(settings.optimizer.enabled, value);
    }
    for (String value : new String[] {"false", "0", "no", "off", "OFF"}) {
      RouterSettings settings = new RouterSettings();
      ReflectionUtil.setFieldValue(settings, "optimizer.enabled", value);
      assertFalse(settings.optimizer.enabled, value);
    }
  }

  @Test
  void unrecognisedBooleanValueIsRejectedInsteadOfReadAsFalse() {
    RouterSettings settings = new RouterSettings();
    assertThrows(
        IllegalArgumentException.class,
        () -> ReflectionUtil.setFieldValue(settings, "optimizer.enabled", "enable"));
    assertEquals(null, settings.optimizer.enabled, "the setting stays unset");
  }

  @Test
  void primitiveFalseAndZeroAreCopied() {
    PrimitiveHolder source = new PrimitiveHolder();
    source.enabled = false;
    source.timeout = 0;
    source.tolerance = 0.0;
    PrimitiveHolder target = new PrimitiveHolder();

    ReflectionUtil.copyFields(source, target);

    assertFalse(target.enabled);
    assertEquals(0, target.timeout);
    assertEquals(0.0, target.tolerance);
  }

  /**
   * The router settings merge relies on "null means not set by this source". A primitive field
   * cannot be unset, so it would override every lower-priority source.
   */
  @Test
  void routerSettingsHaveNoPrimitiveFields() {
    for (Class<?> settingsClass : ROUTER_SETTINGS_CLASSES) {
      for (Field field : settingsClass.getDeclaredFields()) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
          continue;
        }
        assertFalse(
            field.getType().isPrimitive(),
            settingsClass.getSimpleName() + "." + field.getName() + " must be a nullable wrapper");
      }
    }
  }
}
