package com.tcmateditor.client.texture;

import com.tcmateditor.model.FluidDoc;

import java.util.List;
import java.util.function.Function;

/** Tileable procedural fluid surfaces, with the same palette/effects/animation engine as materials. */
public final class FluidTextureEngine {
  public static final List<String> STYLES = List.of("water", "molten", "slime", "oil", "honey", "cloud", "crystal");
  private static final int SIZE = 16;
  private static final double TAU = Math.PI * 2;

  private FluidTextureEngine() {}

  public static Sprite render(FluidDoc doc, boolean flowing, Function<String,Sprite> patterns) {
    Sprite base = base(doc);
    int frames = doc.texture.animation.isActive() ? doc.texture.animation.frames : 1;
    int size = flowing ? SIZE * 2 : SIZE;
    Sprite output = Sprite.blank(size, size * frames);
    for (int frame = 0; frame < frames; frame++) {
      Sprite tile = TextureEngine.renderFrame(doc.texture, base, frame, frames, patterns);
      int shift = flowing ? frame * SIZE / frames : 0;
      for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
          int color = tile.get(x % SIZE, (y + shift) % SIZE);
          int alpha = (color >>> 24) * doc.opacity / 255;
          output.set(x, frame * size + y, (color & 0x00FFFFFF) | alpha << 24);
        }
      }
    }
    return output;
  }

  /** Sampling uses periodic noise and waves so all four texture edges wrap. */
  public static Sprite base(FluidDoc doc) {
    Sprite output = Sprite.blank(SIZE, SIZE);
    int cells = Math.max(2, 10 - doc.textureScale);
    double phase = Colors.hash(0, 0, doc.textureSeed) * TAU;
    for (int y = 0; y < SIZE; y++) {
      for (int x = 0; x < SIZE; x++) {
        double u = x / (double) SIZE, v = y / (double) SIZE;
        double noise = noise(u, v, cells, doc.textureSeed);
        double detail = noise(u, v, cells * 2, doc.textureSeed + 41);
        double wave = Math.sin(TAU * (u + v) + phase);
        double cross = Math.cos(TAU * (u - v) - phase);
        double value = switch (doc.textureStyle) {
          case "molten" -> 0.25 + 0.70 * Math.pow(1 - Math.abs(noise * 2 - 1), 3) + 0.05 * detail;
          case "slime" -> 0.50 + 0.28 * noise + 0.14 * Math.pow(detail, 6) + 0.08 * wave;
          case "oil" -> 0.45 + 0.08 * wave + 0.06 * cross + 0.12 * noise;
          case "honey" -> 0.56 + 0.18 * Math.sin(TAU * v * 2 + wave) + 0.12 * noise;
          case "cloud" -> 0.30 + 0.65 * (noise * 0.7 + detail * 0.3);
          case "crystal" -> 0.35 + 0.45 * Math.abs(wave) + 0.12 * Math.floor(detail * 4) / 4;
          default -> 0.52 + 0.17 * wave + 0.10 * cross + 0.13 * noise;
        };
        int grey = Colors.clamp((int) Math.round(value * 255));
        output.set(x, y, 0xFF000000 | grey << 16 | grey << 8 | grey);
      }
    }
    return output;
  }

  private static double noise(double u, double v, int cells, int seed) {
    double px = u * cells, py = v * cells;
    int x = (int) Math.floor(px), y = (int) Math.floor(py);
    double tx = smooth(px - x), ty = smooth(py - y);
    double a = Colors.hash(x % cells, y % cells, seed);
    double b = Colors.hash((x + 1) % cells, y % cells, seed);
    double c = Colors.hash(x % cells, (y + 1) % cells, seed);
    double d = Colors.hash((x + 1) % cells, (y + 1) % cells, seed);
    return (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty;
  }

  private static double smooth(double value) { return value * value * (3 - 2 * value); }
}
