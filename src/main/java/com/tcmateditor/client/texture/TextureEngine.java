package com.tcmateditor.client.texture;

import com.tcmateditor.model.TextureDoc;
import com.tcmateditor.model.TextureDoc.Animation;
import com.tcmateditor.model.TextureDoc.Effect;

import java.util.List;
import java.util.function.Function;

import static com.tcmateditor.client.texture.Colors.a;
import static com.tcmateditor.client.texture.Colors.b;
import static com.tcmateditor.client.texture.Colors.g;
import static com.tcmateditor.client.texture.Colors.r;

/**
 * Generates material textures from greyscale part sprites.
 * Step 1 matches Tinkers' grey to color recoloring exactly, the following steps are editor effects.
 */
public final class TextureEngine {
  public static final List<String> EFFECT_TYPES = List.of("tint", "noise", "stripes", "checker", "sparkle", "gradient", "outline", "hue", "contrast", "pattern");
  public static final List<String> REGIONS = List.of("all", "dark", "mid", "light", "edge");
  public static final List<String> ANIMATIONS = List.of("none", "shimmer", "pulse", "rainbow", "flicker", "flow");

  private TextureEngine() {}

  /**
   * Renders the final sprite, including all animation frames stacked vertically when animated
   * @param doc       Texture settings
   * @param base      Greyscale base sprite
   * @param animated  Whether the target sprite supports animation
   * @param patterns  Pattern lookup, may return null
   */
  public static Sprite render(TextureDoc doc, Sprite base, boolean animated, Function<String,Sprite> patterns) {
    Animation animation = doc.animation;
    if (animated && animation.isActive()) {
      Sprite frame = base.firstFrame();
      int frames = animation.frames;
      int w = frame.width(), h = frame.height();
      Sprite out = Sprite.blank(w, h * frames);
      for (int f = 0; f < frames; f++) {
        Sprite rendered = renderFrame(doc, frame, f, frames, patterns);
        System.arraycopy(rendered.argb(), 0, out.argb(), f * w * h, w * h);
      }
      return out;
    }
    return renderFrame(doc, base, 0, 1, patterns);
  }

  /** Renders a single frame */
  public static Sprite renderFrame(TextureDoc doc, Sprite base, int frame, int frames, Function<String,Sprite> patterns) {
    int w = base.width(), h = base.height();
    int[] src = base.argb();
    int[] out = new int[src.length];
    int[] lut = new int[256];
    for (int i = 0; i < 256; i++) {
      lut[i] = doc.samplePalette(i);
    }
    // step 1: palette
    for (int i = 0; i < src.length; i++) {
      int color = src[i];
      if (a(color) == 0) {
        out[i] = 0;
        continue;
      }
      int grey = Colors.grey(color);
      out[i] = scaleColor(color, lut[grey], grey);
    }
    // step 2: effects
    for (Effect effect : doc.effects) {
      if (effect.enabled) {
        applyEffect(effect, src, out, w, h, patterns);
      }
    }
    // step 3: animation
    if (frames > 1) {
      applyAnimation(doc.animation, src, out, w, h, frame / (float) frames, frame);
    }
    return new Sprite(w, h, out);
  }

  /** Same as Tinkers' GreyToColorMapping#scaleColor, keeps the shading of the original pixel */
  private static int scaleColor(int original, int newColor, int grey) {
    int alpha = a(original);
    int na = a(newColor);
    if (alpha < 255) {
      na = alpha * na / 255;
    }
    int nr = r(newColor), ng = g(newColor), nb = b(newColor);
    if (grey > 0) {
      if (r(original) < grey) nr = nr * r(original) / grey;
      if (g(original) < grey) ng = ng * g(original) / grey;
      if (b(original) < grey) nb = nb * b(original) / grey;
    }
    return Colors.argb(na, nr, ng, nb);
  }

  /** Region weight 0-1 for a pixel of the base sprite */
  public static float regionWeight(String region, int[] src, int w, int h, int x, int y) {
    int color = src[y * w + x];
    int grey = Colors.grey(color);
    return switch (region) {
      case "dark" -> 1f - smooth(60, 120, grey);
      case "light" -> smooth(150, 205, grey);
      case "mid" -> Math.max(0f, smooth(60, 120, grey) - smooth(150, 205, grey));
      case "edge" -> isEdge(src, w, h, x, y) ? 1f : 0f;
      default -> 1f;
    };
  }

  private static float smooth(float edge0, float edge1, float x) {
    float t = Colors.clamp01((x - edge0) / (edge1 - edge0));
    return t * t * (3 - 2 * t);
  }

  private static boolean isEdge(int[] src, int w, int h, int x, int y) {
    return transparent(src, w, h, x - 1, y) || transparent(src, w, h, x + 1, y) || transparent(src, w, h, x, y - 1) || transparent(src, w, h, x, y + 1);
  }

  private static boolean transparent(int[] src, int w, int h, int x, int y) {
    if (x < 0 || y < 0 || x >= w || y >= h) {
      return true;
    }
    return a(src[y * w + x]) == 0;
  }

  private static void applyEffect(Effect effect, int[] src, int[] out, int w, int h, Function<String,Sprite> patterns) {
    float s = effect.strength;
    int k = Math.max(1, effect.scale);
    int c = effect.color;
    Sprite pattern = "pattern".equals(effect.type) && patterns != null ? patterns.apply(effect.pattern) : null;
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int i = y * w + x;
        int color = out[i];
        if (a(color) == 0) {
          continue;
        }
        float weight = "outline".equals(effect.type) ? (isEdge(src, w, h, x, y) ? 1f : 0f) : regionWeight(effect.region, src, w, h, x, y);
        if (weight <= 0) {
          continue;
        }
        float t = s * weight;
        out[i] = switch (effect.type) {
          case "tint" -> Colors.mix(color, c, t);
          case "noise" -> {
            // blocky value noise, scale controls the block size
            float n = Colors.hash(x / k, y / k, effect.seed) * 2f - 1f;
            yield Colors.brightness(color, n * t * 0.6f);
          }
          case "stripes" -> (((x + y) / k) % 2 == 0) ? Colors.mix(color, c, t) : color;
          case "checker" -> (((x / k) + (y / k)) % 2 == 0) ? Colors.mix(color, c, t) : color;
          case "sparkle" -> Colors.hash(x, y, effect.seed + 17) < s * 0.25f ? Colors.mix(color, c, weight) : color;
          case "gradient" -> {
            // diagonal from the bottom left (handle end) to the top right (tip)
            float g = (x + (h - 1 - y)) / (float) Math.max(1, w + h - 2);
            yield Colors.mix(color, c, g * t);
          }
          case "outline" -> Colors.mix(color, c, t);
          case "hue" -> Colors.hueShift(color, 360f * t);
          case "contrast" -> contrast(color, 1f + 2f * t);
          case "pattern" -> {
            if (pattern == null) {
              yield color;
            }
            // tile the pattern, the scale setting offsets it so different parts can look different
            int p = pattern.get(Math.floorMod(x + effect.seed * k, pattern.width()), Math.floorMod(y + effect.seed, pattern.height()));
            if (a(p) == 0) {
              yield color;
            }
            // overlay the pattern, multiplied with the effect color
            int tinted = Colors.argb(255, r(p) * r(c) / 255, g(p) * g(c) / 255, b(p) * b(c) / 255);
            yield Colors.mix(color, overlay(color, tinted), t * a(p) / 255f);
          }
          default -> color;
        };
      }
    }
  }

  private static int contrast(int color, float factor) {
    return Colors.argb(a(color),
      Math.round((r(color) - 128) * factor + 128),
      Math.round((g(color) - 128) * factor + 128),
      Math.round((b(color) - 128) * factor + 128));
  }

  /** Overlay blend mode */
  private static int overlay(int base, int top) {
    return Colors.argb(a(base), overlayChannel(r(base), r(top)), overlayChannel(g(base), g(top)), overlayChannel(b(base), b(top)));
  }

  private static int overlayChannel(int a, int b) {
    return a < 128 ? (2 * a * b / 255) : (255 - 2 * (255 - a) * (255 - b) / 255);
  }

  private static void applyAnimation(Animation animation, int[] src, int[] out, int w, int h, float phase, int frame) {
    float s = animation.strength;
    int c = animation.color;
    float pulse = 0.5f - 0.5f * (float) Math.cos(phase * Math.PI * 2);
    float band = Math.max(2f, (w + h) / 8f);
    float center = phase * (w + h + band * 2) - band;
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int i = y * w + x;
        int color = out[i];
        if (a(color) == 0) {
          continue;
        }
        out[i] = switch (animation.type) {
          case "shimmer" -> {
            float u = x + (h - 1 - y);
            float d = Math.abs(u - center) / band;
            yield d < 1 ? Colors.mix(color, c, s * (1 - d)) : color;
          }
          case "pulse" -> Colors.mix(color, c, s * pulse);
          case "rainbow" -> Colors.mix(color, Colors.hueShift(color, 360f * phase), s);
          case "flicker" -> Colors.hash(x, y, frame * 31 + 7) < s * 0.15f ? Colors.mix(color, c, 0.85f) : color;
          case "flow" -> {
            int shift = Math.round(phase * h);
            float n = Colors.hash(x / 2, (y + shift) % h / 2, 99) * 2f - 1f;
            yield Colors.mix(Colors.brightness(color, n * s * 0.4f), c, Math.max(0, n) * s * 0.5f);
          }
          default -> color;
        };
      }
    }
  }
}
