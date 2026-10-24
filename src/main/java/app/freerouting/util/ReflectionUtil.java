package app.freerouting.util;

import app.freerouting.logger.FRLogger;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

/** Utility methods for reflective field access and copying between settings objects. */
public final class ReflectionUtil {

  private ReflectionUtil() {}

  /**
   * Sets a nested field value on an object using a dot-separated property path.
   *
   * @param obj the target object
   * @param propertyName path to the field (supports {@code .}, {@code :}, and {@code -} separators)
   * @param newValue the value to assign
   * @throws Exception if reflection fails or the field cannot be found
   */
  public static void setFieldValue(Object obj, String propertyName, Object newValue)
      throws Exception {
    String[] propertyPath = propertyName.split("[.:\\-]");
    setPropertyRecursive(obj, propertyPath, 0, newValue);
  }

  private static void setPropertyRecursive(
      Object currentObject, String[] propertyPath, int pathIndex, Object newValue)
      throws Exception {
    if (currentObject == null) {
      return;
    }

    String currentName = propertyPath[pathIndex];
    Field field = getFieldByNameOrSerializedName(currentObject.getClass(), currentName);
    field.setAccessible(true);

    if (pathIndex == propertyPath.length - 1) {
      // Leaf field - set the value
      Object convertedValue = convertValue(field.getType(), newValue);
      field.set(currentObject, convertedValue);
      return;
    }

    // Intermediate field - check if it is an array
    if (field.getType().isArray()) {
      Class<?> componentType = field.getType().getComponentType();
      // We are navigating through an array. The next part of the path is a property of the array
      // element.
      // E.g., layers.routable
      // The newValue is expected to be a comma-separated list of values for each array element.
      String[] valTokens = newValue.toString().split(",");
      int size = valTokens.length;

      Object array = field.get(currentObject);
      if (array == null) {
        array = java.lang.reflect.Array.newInstance(componentType, size);
        field.set(currentObject, array);
      }

      int arrayLength = java.lang.reflect.Array.getLength(array);
      int limit = Math.min(arrayLength, size);

      for (int i = 0; i < limit; i++) {
        Object element = java.lang.reflect.Array.get(array, i);
        if (element == null) {
          element = componentType.getDeclaredConstructor().newInstance();
          java.lang.reflect.Array.set(array, i, element);
        }
        // Recursively set the property on the array element
        setPropertyRecursive(element, propertyPath, pathIndex + 1, valTokens[i].trim());
      }
    } else {
      // Normal object navigation
      Object nestedObject = field.get(currentObject);
      if (nestedObject == null) {
        nestedObject = field.getType().getDeclaredConstructor().newInstance();
        field.set(currentObject, nestedObject);
      }
      setPropertyRecursive(nestedObject, propertyPath, pathIndex + 1, newValue);
    }
  }

  private static Field getFieldByNameOrSerializedName(Class<?> clazz, String name)
      throws NoSuchFieldException {
    String camelName = snakeToLowerCamel(name);
    for (Field field : clazz.getDeclaredFields()) {
      SerializedName annotation = field.getAnnotation(SerializedName.class);
      if (annotation != null) {
        if (annotation.value().equalsIgnoreCase(name)
            || annotation.value().equalsIgnoreCase(camelName)
            || snakeToLowerCamel(annotation.value()).equalsIgnoreCase(name)
            || snakeToLowerCamel(annotation.value()).equalsIgnoreCase(camelName)) {
          return field;
        }
        for (String alt : annotation.alternate()) {
          if (alt.equalsIgnoreCase(name)
              || alt.equalsIgnoreCase(camelName)
              || snakeToLowerCamel(alt).equalsIgnoreCase(name)
              || snakeToLowerCamel(alt).equalsIgnoreCase(camelName)) {
            return field;
          }
        }
      }
      if (field.getName().equalsIgnoreCase(name)
          || field.getName().equalsIgnoreCase(camelName)
          || snakeToLowerCamel(field.getName()).equalsIgnoreCase(camelName)) {
        return field;
      }
    }
    if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
      return getFieldByNameOrSerializedName(clazz.getSuperclass(), name);
    }
    throw new NoSuchFieldException("No field found with name or SerializedName: " + name);
  }

  private static String snakeToLowerCamel(String name) {
    if (!name.contains("_")) {
      return name;
    }
    String[] parts = name.split("_");
    StringBuilder sb = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
    for (int i = 1; i < parts.length; i++) {
      if (!parts[i].isEmpty()) {
        sb.append(Character.toUpperCase(parts[i].charAt(0)))
            .append(parts[i].substring(1).toLowerCase(Locale.ROOT));
      }
    }
    return sb.toString();
  }

  private static Object convertValue(Class<?> targetType, Object value) {
    if (targetType.isInstance(value)) {
      return value;
    }
    if (targetType == int.class || targetType == Integer.class) {
      return Integer.parseInt(value.toString());
    }
    if (targetType == long.class || targetType == Long.class) {
      return Long.parseLong(value.toString());
    }
    if (targetType == double.class || targetType == Double.class) {
      return Double.parseDouble(value.toString());
    }
    if (targetType == boolean.class || targetType == Boolean.class) {
      return parseBoolean(value.toString());
    }
    if (targetType == float.class || targetType == Float.class) {
      return Float.parseFloat(value.toString());
    }
    if (targetType.isEnum()) {
      for (Object constant : targetType.getEnumConstants()) {
        if (((Enum<?>) constant).name().equalsIgnoreCase(value.toString().trim())) {
          return constant;
        }
      }
    }
    if (targetType == String[].class) {
      // Parse a comma-separated string into a String array.
      // This allows CLI arguments and environment variables to supply list values,
      // e.g. --api_server-endpoints=http://0.0.0.0:37864,http://127.0.0.1:37864
      String raw = value.toString().trim();
      if (raw.isEmpty()) {
        return new String[0];
      }
      String[] tokens = raw.split(",");
      for (int i = 0; i < tokens.length; i++) {
        tokens[i] = tokens[i].trim();
      }
      return tokens;
    }
    if (targetType == double[].class) {
      String raw = value.toString().trim();
      if (raw.isEmpty()) {
        return new double[0];
      }
      String[] tokens = raw.split(",");
      double[] result = new double[tokens.length];
      for (int i = 0; i < tokens.length; i++) {
        result[i] = Double.parseDouble(tokens[i].trim());
      }
      return result;
    }
    if (targetType == int[].class) {
      String raw = value.toString().trim();
      if (raw.isEmpty()) {
        return new int[0];
      }
      String[] tokens = raw.split(",");
      int[] result = new int[tokens.length];
      for (int i = 0; i < tokens.length; i++) {
        result[i] = Integer.parseInt(tokens[i].trim());
      }
      return result;
    }
    // Add more type conversions as needed
    return value;
  }

  /**
   * Parses a boolean setting value: {@code true}/{@code false}, {@code 1}/{@code 0}, {@code
   * yes}/{@code no} and {@code on}/{@code off}, ignoring case and surrounding whitespace.
   *
   * @throws IllegalArgumentException for any other value, instead of reading it as {@code false}
   */
  static boolean parseBoolean(String value) {
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "true", "1", "yes", "on" -> true;
      case "false", "0", "no", "off" -> false;
      default ->
          throw new IllegalArgumentException(
              "'" + value + "' is not a boolean value (expected true or false)");
    };
  }

  /**
   * Copy all non-null fields from one object to another recursively.
   *
   * @param source The source object
   * @param target The target object
   * @return The number of fields that were copied
   */
  @SuppressWarnings("unchecked")
  public static int copyFields(Object source, Object target) {
    int numberOfFieldsChanged = 0;

    for (Field field : source.getClass().getDeclaredFields()) {
      try {
        // check if the field is static and skip it if it is
        if (Modifier.isStatic(field.getModifiers())) {
          continue;
        }

        // check if the field is private and skip it if it is
        if (!Modifier.isPublic(field.getModifiers())) {
          continue;
        }

        field.setAccessible(true);
        Object sourceValue = field.get(source);

        // For nullable wrappers (Boolean/Integer/...), non-null already means "explicitly set".
        // A primitive cannot be unset, so its value is always copied: skipping the Java default
        // (false, 0) dropped explicit values such as "enabled": false or a timeout of 0 whenever
        // the target's initializer held a different value. Settings that must be able to stay
        // unset in a source (all RouterSettings fields) are nullable wrappers.
        boolean shouldCopy = sourceValue != null;

        if (shouldCopy) {
          // Check if the field is a primitive, wrapper type, or a string
          if (field.getType().isPrimitive()
              || field.getType() == String.class
              || field.getType() == Integer.class
              || field.getType() == Long.class
              || field.getType() == Float.class
              || field.getType() == Double.class
              || field.getType() == Boolean.class
              || field.getType() == Byte.class
              || field.getType() == Short.class
              || field.getType() == Character.class) {
            // check if the target field is null or its default value
            var targetValue = field.get(target);

            if (targetValue != sourceValue) {
              field.set(target, sourceValue);
              numberOfFieldsChanged++;
            }
          } else if (field.getType().isEnum()) {
            var enumType = (Class<Enum>) field.getType();
            var enumValue = Enum.valueOf(enumType, sourceValue.toString());

            // Copy the enum value
            field.set(target, enumValue);
            numberOfFieldsChanged++;
          } else if (field.getType().isArray()) {
            // Is the array of primitive types or strings?
            if (field.getType().getComponentType().isPrimitive()
                || field.getType().getComponentType() == String.class) {
              // Copy the array so that later edits of the merged settings cannot write back
              // into the source (strings are immutable, so a shallow copy is enough).
              int length = java.lang.reflect.Array.getLength(sourceValue);
              Object copy =
                  java.lang.reflect.Array.newInstance(field.getType().getComponentType(), length);
              System.arraycopy(sourceValue, 0, copy, 0, length);
              field.set(target, copy);
              numberOfFieldsChanged++;
            } else {
              // The field is an array of objects (like LayerSettings[])
              Object[] sourceArray = (Object[]) sourceValue;
              Class<?> componentType = field.getType().getComponentType();

              Object targetArrayObj = field.get(target);
              int targetLength =
                  targetArrayObj != null ? java.lang.reflect.Array.getLength(targetArrayObj) : 0;

              if (targetLength >= sourceArray.length) {
                // Merge source elements into existing target elements
                Object[] targetObjArray = (Object[]) targetArrayObj;
                for (int i = 0; i < sourceArray.length; i++) {
                  if (sourceArray[i] != null) {
                    if (targetObjArray[i] == null) {
                      targetObjArray[i] = componentType.getDeclaredConstructor().newInstance();
                    }
                    copyFields(sourceArray[i], targetObjArray[i]);
                  }
                }
                numberOfFieldsChanged += sourceArray.length;
              } else {
                // Allocate a new target array of the specific component type
                Object[] targetArray =
                    (Object[])
                        java.lang.reflect.Array.newInstance(componentType, sourceArray.length);
                for (int i = 0; i < sourceArray.length; i++) {
                  if (sourceArray[i] != null) {
                    Object targetElement = componentType.getDeclaredConstructor().newInstance();
                    copyFields(sourceArray[i], targetElement);
                    targetArray[i] = targetElement;
                  }
                }
                field.set(target, targetArray);
                numberOfFieldsChanged += sourceArray.length;
              }
            }
          } else if (java.util.Collection.class.isAssignableFrom(field.getType())) {
            java.util.Collection<?> sourceCol = (java.util.Collection<?>) sourceValue;
            if (java.util.Set.class.isAssignableFrom(field.getType())) {
              field.set(target, new java.util.LinkedHashSet<>(sourceCol));
            } else {
              field.set(target, new java.util.ArrayList<>(sourceCol));
            }
            numberOfFieldsChanged++;
          } else {
            // The field is an object, so we need to copy its fields
            Object targetField = field.get(target);
            if (targetField == null) {
              targetField = field.getType().getDeclaredConstructor().newInstance();
              field.set(target, targetField);
            }
            numberOfFieldsChanged += copyFields(sourceValue, targetField);
          }
        }
      } catch (Exception e) {
        FRLogger.error("Error copying fields", e);
      }
    }

    return numberOfFieldsChanged;
  }
}
