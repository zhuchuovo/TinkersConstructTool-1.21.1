package com.tcmateditor.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Describes how part textures are generated for a material.
 * Greyscale part sprites from Tinkers' are recolored through {@link #palette}, then {@link #effects} are layered on top.
 */
public class TextureDoc {
  /** Base sprite style, matches Tinkers' render fallbacks such as metal, wood or rock. Empty uses the plain sprite. */
  public String baseStyle = "metal";
  /** Grey value to color stops, sorted by grey */
  public List<Stop> palette = new ArrayList<>();
  /** Effect layers applied in order */
  public List<Effect> effects = new ArrayList<>();
  /** Animation, only for sprites that support it */
  public Animation animation = new Animation();
  /** Emissive light level 0-15 */
  public int luminosity = 0;

  public static class Stop {
    public int grey;
    /** ARGB */
    public int color;

    public Stop() {}

    public Stop(int grey, int color) {
      this.grey = grey;
      this.color = color;
    }

    public Stop copy() {
      return new Stop(grey, color);
    }
  }

  public static class Effect {
    /** tint, noise, stripes, checker, sparkle, gradient, outline, hue, contrast, pattern */
    public String type = "noise";
    /** all, dark, mid, light, edge */
    public String region = "all";
    public int color = 0xFFFFFFFF;
    /** 0-1 strength */
    public float strength = 0.35f;
    /** pattern scale in pixels */
    public int scale = 3;
    public int seed = 0;
    public boolean enabled = true;
    /** pattern texture: either a resource location of a texture, or "file:<name>.png" from the patterns folder */
    public String pattern = "";

    public Effect copy() {
      Effect effect = new Effect();
      effect.type = type;
      effect.region = region;
      effect.color = color;
      effect.strength = strength;
      effect.scale = scale;
      effect.seed = seed;
      effect.enabled = enabled;
      effect.pattern = pattern;
      return effect;
    }
  }

  public static class Animation {
    /** none, shimmer, pulse, rainbow, flicker, flow */
    public String type = "none";
    public int frames = 8;
    /** ticks per frame */
    public int frametime = 3;
    public int color = 0xFFFFFFFF;
    public float strength = 0.5f;
    public boolean interpolate = true;

    public boolean isActive() {
      return !"none".equals(type) && frames > 1;
    }

    public Animation copy() {
      Animation animation = new Animation();
      animation.type = type;
      animation.frames = frames;
      animation.frametime = frametime;
      animation.color = color;
      animation.strength = strength;
      animation.interpolate = interpolate;
      return animation;
    }
  }

  /** Creates the default texture for a new material */
  public static TextureDoc createDefault(int color) {
    TextureDoc doc = new TextureDoc();
    doc.palette = paletteFromColor(color);
    return doc;
  }

  /**
   * Builds a 7 stop palette from a single base color, similar to the palettes Tinkers' uses for its own materials.
   * The base color is placed at grey 216 which is the main highlight of most part sprites.
   */
  public static List<Stop> paletteFromColor(int color) {
    float[] factors = { 0f, 0.18f, 0.38f, 0.56f, 0.76f, 1f, 1.18f };
    int[] greys = { 0, 63, 102, 140, 178, 216, 255 };
    List<Stop> stops = new ArrayList<>();
    int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
    for (int i = 0; i < greys.length; i++) {
      float f = factors[i];
      int nr, ng, nb;
      if (f <= 1f) {
        nr = Math.round(r * f);
        ng = Math.round(g * f);
        nb = Math.round(b * f);
      } else {
        // lighten towards white for the brightest stop
        float t = f - 1f;
        nr = Math.round(r + (255 - r) * t * 2.2f);
        ng = Math.round(g + (255 - g) * t * 2.2f);
        nb = Math.round(b + (255 - b) * t * 2.2f);
      }
      stops.add(new Stop(greys[i], 0xFF000000 | clamp(nr) << 16 | clamp(ng) << 8 | clamp(nb)));
    }
    return stops;
  }

  private static int clamp(int v) {
    return Math.max(0, Math.min(255, v));
  }

  public void sortPalette() {
    palette.sort(Comparator.comparingInt(s -> s.grey));
  }

  /** Main color of the palette, used for tinting fallbacks and the name color */
  public int mainColor() {
    return samplePalette(216);
  }

  /** Samples the palette at the given grey value, linear interpolation between stops */
  public int samplePalette(int grey) {
    if (palette.isEmpty()) {
      return 0xFF000000 | grey << 16 | grey << 8 | grey;
    }
    Stop first = palette.get(0);
    if (grey <= first.grey) {
      return first.color;
    }
    for (int i = 1; i < palette.size(); i++) {
      Stop next = palette.get(i);
      if (grey <= next.grey) {
        Stop prev = palette.get(i - 1);
        int span = next.grey - prev.grey;
        if (span <= 0) {
          return next.color;
        }
        return lerpColor(prev.color, next.color, (grey - prev.grey) / (float) span);
      }
    }
    return palette.get(palette.size() - 1).color;
  }

  public static int lerpColor(int a, int b, float t) {
    int aa = (a >>> 24), ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
    int ba = (b >>> 24), br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
    return Math.round(aa + (ba - aa) * t) << 24
      | Math.round(ar + (br - ar) * t) << 16
      | Math.round(ag + (bg - ag) * t) << 8
      | Math.round(ab + (bb - ab) * t);
  }

  public TextureDoc copy() {
    TextureDoc doc = new TextureDoc();
    doc.baseStyle = baseStyle;
    doc.luminosity = luminosity;
    doc.palette = new ArrayList<>(palette.stream().map(Stop::copy).toList());
    doc.effects = new ArrayList<>(effects.stream().map(Effect::copy).toList());
    doc.animation = animation.copy();
    return doc;
  }

  /** Repairs invalid data after loading */
  public void validate() {
    if (palette == null) palette = new ArrayList<>();
    if (effects == null) effects = new ArrayList<>();
    if (animation == null) animation = new Animation();
    if (baseStyle == null) baseStyle = "";
    palette.removeIf(s -> s == null);
    for (Stop stop : palette) {
      stop.grey = Math.max(0, Math.min(255, stop.grey));
    }
    sortPalette();
    // remove duplicate grey values, Tinkers' palettes require strictly increasing values
    for (int i = palette.size() - 1; i > 0; i--) {
      if (palette.get(i).grey == palette.get(i - 1).grey) {
        palette.remove(i);
      }
    }
    if (palette.size() < 2) {
      palette = paletteFromColor(palette.isEmpty() ? 0xFFB0B0B0 : palette.get(0).color);
    }
    effects.removeIf(e -> e == null);
    luminosity = Math.max(0, Math.min(15, luminosity));
    animation.frames = Math.max(1, Math.min(32, animation.frames));
    animation.frametime = Math.max(1, Math.min(40, animation.frametime));
  }
}
