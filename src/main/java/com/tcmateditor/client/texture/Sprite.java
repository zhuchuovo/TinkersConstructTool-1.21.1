package com.tcmateditor.client.texture;

/** Simple ARGB image */
public record Sprite(int width, int height, int[] argb) {
  public Sprite {
    if (argb.length != width * height) {
      throw new IllegalArgumentException("Pixel count mismatch");
    }
  }

  public static Sprite blank(int width, int height) {
    return new Sprite(width, height, new int[width * height]);
  }

  public int get(int x, int y) {
    return argb[y * width + x];
  }

  public void set(int x, int y, int color) {
    argb[y * width + x] = color;
  }

  /** True if the sprite is a vertical strip of square frames */
  public boolean isAnimated() {
    return height > width && height % width == 0;
  }

  /** First square frame of the sprite */
  public Sprite firstFrame() {
    if (!isAnimated()) {
      return this;
    }
    int[] pixels = new int[width * width];
    System.arraycopy(argb, 0, pixels, 0, pixels.length);
    return new Sprite(width, width, pixels);
  }

  /** Draws another sprite on top using alpha blending, scaling it to fit if sizes differ */
  public void blendOver(Sprite top) {
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int src = top.get(x * top.width / width, y * top.height / height);
        int sa = src >>> 24;
        if (sa == 0) {
          continue;
        }
        if (sa == 255) {
          set(x, y, src);
          continue;
        }
        int dst = get(x, y);
        int da = dst >>> 24;
        float a = sa / 255f;
        int outA = Math.min(255, Math.round(sa + da * (1 - a)));
        int r = Math.round(((src >> 16) & 0xFF) * a + ((dst >> 16) & 0xFF) * (1 - a));
        int g = Math.round(((src >> 8) & 0xFF) * a + ((dst >> 8) & 0xFF) * (1 - a));
        int b = Math.round((src & 0xFF) * a + (dst & 0xFF) * (1 - a));
        set(x, y, outA << 24 | r << 16 | g << 8 | b);
      }
    }
  }
}
