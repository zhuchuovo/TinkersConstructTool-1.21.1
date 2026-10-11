package com.tcmateditor.client.texture;

/** ARGB color helpers */
public final class Colors {
  private Colors() {}

  public static int a(int c) { return c >>> 24; }
  public static int r(int c) { return (c >> 16) & 0xFF; }
  public static int g(int c) { return (c >> 8) & 0xFF; }
  public static int b(int c) { return c & 0xFF; }

  public static int argb(int a, int r, int g, int b) {
    return clamp(a) << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
  }

  public static int clamp(int v) {
    return v < 0 ? 0 : Math.min(255, v);
  }

  public static float clamp01(float v) {
    return v < 0 ? 0 : Math.min(1, v);
  }

  /** Linear blend of RGB, keeping the alpha of {@code base} */
  public static int mix(int base, int other, float t) {
    t = clamp01(t);
    return argb(a(base),
      Math.round(r(base) + (r(other) - r(base)) * t),
      Math.round(g(base) + (g(other) - g(base)) * t),
      Math.round(b(base) + (b(other) - b(base)) * t));
  }

  /** Brightens (positive) or darkens (negative) a color */
  public static int brightness(int c, float amount) {
    if (amount >= 0) {
      return mix(c, 0xFFFFFFFF, amount);
    }
    return mix(c, 0xFF000000, -amount);
  }

  /** Max channel, the grey value Tinkers' uses */
  public static int grey(int c) {
    return Math.max(r(c), Math.max(g(c), b(c)));
  }

  public static float luminance(int c) {
    return (0.299f * r(c) + 0.587f * g(c) + 0.114f * b(c)) / 255f;
  }

  /** RGB to HSV, all components 0-1 */
  public static float[] toHsv(int c) {
    float r = r(c) / 255f, g = g(c) / 255f, b = b(c) / 255f;
    float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
    float delta = max - min;
    float h = 0;
    if (delta > 0) {
      if (max == r) {
        h = ((g - b) / delta) % 6f;
      } else if (max == g) {
        h = (b - r) / delta + 2f;
      } else {
        h = (r - g) / delta + 4f;
      }
      h /= 6f;
      if (h < 0) h += 1f;
    }
    float s = max == 0 ? 0 : delta / max;
    return new float[] { h, s, max };
  }

  public static int fromHsv(float h, float s, float v, int alpha) {
    h = h - (float) Math.floor(h);
    float c = v * s;
    float hp = h * 6f;
    float x = c * (1 - Math.abs(hp % 2f - 1));
    float r = 0, g = 0, b = 0;
    if (hp < 1) { r = c; g = x; }
    else if (hp < 2) { r = x; g = c; }
    else if (hp < 3) { g = c; b = x; }
    else if (hp < 4) { g = x; b = c; }
    else if (hp < 5) { r = x; b = c; }
    else { r = c; b = x; }
    float m = v - c;
    return argb(alpha, Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
  }

  public static int hueShift(int c, float degrees) {
    float[] hsv = toHsv(c);
    return fromHsv(hsv[0] + degrees / 360f, hsv[1], hsv[2], a(c));
  }

  /** Converts ARGB to the ABGR layout used by NativeImage */
  public static int toAbgr(int argb) {
    return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
  }

  public static int fromAbgr(int abgr) {
    return toAbgr(abgr);
  }

  public static String hex(int argb) {
    return String.format("#%06X", argb & 0xFFFFFF);
  }

  /** Parses #RRGGBB or RRGGBB, returns null when invalid */
  public static Integer parseHex(String text) {
    if (text == null) return null;
    String s = text.trim();
    if (s.startsWith("#")) s = s.substring(1);
    if (s.length() != 6) return null;
    try {
      return 0xFF000000 | Integer.parseInt(s, 16);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Deterministic hash noise 0-1 */
  public static float hash(int x, int y, int seed) {
    int h = x * 374761393 + y * 668265263 + seed * 1442695041;
    h = (h ^ (h >>> 13)) * 1274126177;
    h ^= h >>> 16;
    return (h & 0xFFFFFF) / (float) 0xFFFFFF;
  }
}
