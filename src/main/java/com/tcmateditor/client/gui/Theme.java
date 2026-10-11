package com.tcmateditor.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Flat style shared by every widget, with a dark and a light palette */
public final class Theme {
  private Theme() {}

  private static boolean light = false;

  public static int WINDOW, SIDEBAR, PANEL, CARD, CARD_HOVER, INPUT, BORDER, BORDER_HOVER;
  public static int ACCENT, ACCENT_HOVER, ACCENT_DIM;
  public static int TEXT, TEXT_DIM, TEXT_FAINT;
  public static int SUCCESS, WARNING, DANGER, DANGER_DIM, ON_DANGER_DIM;
  /** Toggle track when off, slider knob, transparency checkerboard, screen dimming */
  public static int TOGGLE_OFF, TOGGLE_OFF_HOVER, KNOB, KNOB_HOVER, CHECKER_A, CHECKER_B, DIM;

  static {
    apply(false);
  }

  public static boolean isLight() {
    return light;
  }

  /** Switches the palette, screens need to rebuild afterwards */
  public static void apply(boolean lightTheme) {
    light = lightTheme;
    if (lightTheme) {
      WINDOW = 0xFAF6F7F9;
      SIDEBAR = 0xFFECEEF2;
      PANEL = 0xFFEEF0F4;
      CARD = 0xFFE2E6EC;
      CARD_HOVER = 0xFFD5DAE2;
      INPUT = 0xFFFFFFFF;
      BORDER = 0xFFCDD2DA;
      BORDER_HOVER = 0xFFA4ACB8;
      ACCENT = 0xFF2F7BEA;
      ACCENT_HOVER = 0xFF4A8FF0;
      ACCENT_DIM = 0xFFD3E3FC;
      TEXT = 0xFF1F2328;
      TEXT_DIM = 0xFF57606A;
      TEXT_FAINT = 0xFF8C959F;
      SUCCESS = 0xFF1A7F37;
      WARNING = 0xFF9A6700;
      DANGER = 0xFFCF222E;
      DANGER_DIM = 0xFFFFE3E3;
      ON_DANGER_DIM = 0xFFCF222E;
      TOGGLE_OFF = 0xFFB9C0CA;
      TOGGLE_OFF_HOVER = 0xFF9AA2AE;
      KNOB = 0xFF6E7781;
      KNOB_HOVER = 0xFF3D444D;
      CHECKER_A = 0xFFE3E6EA;
      CHECKER_B = 0xFFD5D9DF;
      DIM = 0x55000000;
    } else {
      WINDOW = 0xF2141519;
      SIDEBAR = 0xFF17191F;
      PANEL = 0xFF1B1D23;
      CARD = 0xFF22252D;
      CARD_HOVER = 0xFF2A2E38;
      INPUT = 0xFF111216;
      BORDER = 0xFF2F333D;
      BORDER_HOVER = 0xFF464B58;
      ACCENT = 0xFF4C9EFF;
      ACCENT_HOVER = 0xFF6BB0FF;
      ACCENT_DIM = 0xFF24466F;
      TEXT = 0xFFE8EAF0;
      TEXT_DIM = 0xFF9AA0AE;
      TEXT_FAINT = 0xFF636977;
      SUCCESS = 0xFF46C25A;
      WARNING = 0xFFE0A63C;
      DANGER = 0xFFE5534B;
      DANGER_DIM = 0xFF5A2427;
      ON_DANGER_DIM = 0xFFFFFFFF;
      TOGGLE_OFF = 0xFF2F333D;
      TOGGLE_OFF_HOVER = 0xFF464B58;
      KNOB = 0xFFD0D4DC;
      KNOB_HOVER = 0xFFFFFFFF;
      CHECKER_A = 0xFF2A2D34;
      CHECKER_B = 0xFF23262C;
      DIM = 0x99000000;
    }
  }

  public static Font font() {
    return Minecraft.getInstance().font;
  }

  /** Rectangle with the corner pixels removed, reads as slightly rounded */
  public static void box(GuiGraphics g, int x, int y, int w, int h, int color) {
    if (w <= 2 || h <= 2) {
      g.fill(x, y, x + w, y + h, color);
      return;
    }
    g.fill(x + 1, y, x + w - 1, y + h, color);
    g.fill(x, y + 1, x + 1, y + h - 1, color);
    g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
  }

  /** Box with a 1px outline */
  public static void outlined(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
    box(g, x, y, w, h, border);
    box(g, x + 1, y + 1, w - 2, h - 2, fill);
  }

  public static void text(GuiGraphics g, Component text, int x, int y, int color) {
    g.drawString(font(), text, x, y, color, false);
  }

  public static void text(GuiGraphics g, String text, int x, int y, int color) {
    g.drawString(font(), text, x, y, color, false);
  }

  public static void centered(GuiGraphics g, Component text, int cx, int y, int color) {
    Font font = font();
    g.drawString(font, text, cx - font.width(text) / 2, y, color, false);
  }

  /** Draws text cut to the given width with an ellipsis */
  public static void ellipsis(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
    Font font = font();
    if (font.width(text) <= maxWidth) {
      g.drawString(font, text, x, y, color, false);
      return;
    }
    FormattedCharSequence cut = net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(text, Math.max(0, maxWidth - font.width("…"))));
    int end = g.drawString(font, cut, x, y, color, false);
    g.drawString(font, "…", end, y, color, false);
  }

  public static int brighten(int color, float amount) {
    int a = color >>> 24;
    int r = (color >> 16) & 0xFF, gr = (color >> 8) & 0xFF, b = color & 0xFF;
    r = Math.min(255, Math.round(r + (255 - r) * amount));
    gr = Math.min(255, Math.round(gr + (255 - gr) * amount));
    b = Math.min(255, Math.round(b + (255 - b) * amount));
    return a << 24 | r << 16 | gr << 8 | b;
  }

  /** Readable text color on top of the given background */
  public static int contrastText(int background) {
    int r = (background >> 16) & 0xFF, gr = (background >> 8) & 0xFF, b = background & 0xFF;
    return (0.299 * r + 0.587 * gr + 0.114 * b) > 150 ? 0xFF111111 : 0xFFFFFFFF;
  }

  /** Checkerboard to show transparency */
  public static void checker(GuiGraphics g, int x, int y, int w, int h, int size) {
    g.fill(x, y, x + w, y + h, CHECKER_A);
    for (int yy = 0; yy < h; yy += size) {
      for (int xx = (yy / size) % 2 == 0 ? 0 : size; xx < w; xx += size * 2) {
        g.fill(x + xx, y + yy, Math.min(x + w, x + xx + size), Math.min(y + h, y + yy + size), CHECKER_B);
      }
    }
  }
}
