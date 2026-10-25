package app.freerouting.util;

import java.math.BigInteger;

/**
 * Compares Freerouting version strings such as {@code 2.5.0}, {@code v2.6.0} or {@code
 * 2.6.0-SNAPSHOT}.
 */
public final class Versions {

  private Versions() {}

  /**
   * Compares two versions by semantic-versioning precedence: the dot-separated release numbers
   * first, then a pre-release suffix ({@code 2.6.0-SNAPSHOT} is newer than {@code 2.5.0} but older
   * than {@code 2.6.0}). A leading {@code v} and build metadata after {@code +} are ignored, and
   * missing release numbers count as 0. Parts that are not numbers compare as text.
   *
   * @return negative if {@code v1} is older than {@code v2}, zero if they are equal, positive if
   *     {@code v1} is newer; {@code null} is older than any version
   */
  public static int compare(String v1, String v2) {
    if (v1 == null || v2 == null) {
      return v1 == null ? (v2 == null ? 0 : -1) : 1;
    }
    String[] a = splitPreRelease(v1);
    String[] b = splitPreRelease(v2);
    int cmp = compareIdentifiers(a[0].split("\\."), b[0].split("\\."), "0");
    if (cmp != 0) {
      return cmp;
    }
    if (a[1] == null || b[1] == null) {
      // A release is newer than any pre-release of the same version.
      return a[1] == null ? (b[1] == null ? 0 : 1) : -1;
    }
    return compareIdentifiers(a[1].split("\\."), b[1].split("\\."), null);
  }

  /** Returns {@code {release, preRelease}}; {@code preRelease} is null for a release. */
  private static String[] splitPreRelease(String version) {
    String v = version.trim();
    if (v.startsWith("v") || v.startsWith("V")) {
      v = v.substring(1);
    }
    int build = v.indexOf('+');
    if (build >= 0) {
      v = v.substring(0, build);
    }
    int dash = v.indexOf('-');
    return dash >= 0
        ? new String[] {v.substring(0, dash), v.substring(dash + 1)}
        : new String[] {v, null};
  }

  /**
   * Compares identifier lists part by part; a missing part is {@code missing}, or, when that is
   * null, older than any present part (so {@code alpha} is older than {@code alpha.1}).
   */
  private static int compareIdentifiers(String[] a, String[] b, String missing) {
    for (int i = 0; i < Math.max(a.length, b.length); i++) {
      String x = i < a.length ? a[i] : missing;
      String y = i < b.length ? b[i] : missing;
      if (x == null || y == null) {
        return x == null ? -1 : 1;
      }
      int cmp = compareIdentifier(x, y);
      if (cmp != 0) {
        return cmp;
      }
    }
    return 0;
  }

  /** Numbers compare numerically and are older than text; text compares case-insensitively. */
  private static int compareIdentifier(String x, String y) {
    boolean firstIsNumber = isNumber(x);
    boolean secondIsNumber = isNumber(y);
    if (firstIsNumber && secondIsNumber) {
      return new BigInteger(x).compareTo(new BigInteger(y));
    }
    if (firstIsNumber != secondIsNumber) {
      return firstIsNumber ? -1 : 1;
    }
    return x.compareToIgnoreCase(y);
  }

  private static boolean isNumber(String s) {
    return !s.isEmpty() && s.chars().allMatch(Character::isDigit);
  }
}
