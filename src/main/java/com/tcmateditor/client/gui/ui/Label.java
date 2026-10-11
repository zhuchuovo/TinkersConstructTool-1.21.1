package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

/** Text, optionally wrapped. When wrapping the height is computed from the width. */
public class Label extends UIElement {
  private final Supplier<Component> text;
  public int color;
  private boolean wrap = false;
  private boolean centered = false;
  private Supplier<Integer> colorSupplier;

  public Label(Component text, int color) {
    this(() -> text, color);
  }

  public Label(Supplier<Component> text, int color) {
    this.text = text;
    this.color = color;
    this.h = 9;
  }

  /** Wraps text to the given width and sets the height */
  public Label wrap(int width) {
    this.wrap = true;
    this.w = width;
    this.h = Math.max(9, Theme.font().split(text.get(), width).size() * 10);
    return this;
  }

  public Label centered(int width) {
    this.centered = true;
    this.w = width;
    return this;
  }

  public Label color(Supplier<Integer> color) {
    this.colorSupplier = color;
    return this;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    int c = colorSupplier != null ? colorSupplier.get() : color;
    Component value = text.get();
    if (wrap) {
      Font font = Theme.font();
      List<FormattedCharSequence> lines = font.split(value, w);
      int yy = y;
      for (FormattedCharSequence line : lines) {
        g.drawString(font, line, x, yy, c, false);
        yy += 10;
      }
    } else if (centered) {
      Theme.centered(g, value, x + w / 2, y, c);
    } else if (w > 0) {
      Theme.ellipsis(g, value, x, y, w, c);
    } else {
      Theme.text(g, value, x, y, c);
    }
  }
}
