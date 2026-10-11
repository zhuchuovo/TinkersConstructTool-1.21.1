package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.texture.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Color square, opens the color picker when clicked */
public class ColorSwatch extends UIElement {
  private final IntSupplier getter;
  private final IntConsumer setter;
  private boolean showHex = false;

  public ColorSwatch(IntSupplier getter, IntConsumer setter) {
    this.getter = getter;
    this.setter = setter;
    this.w = 16;
    this.h = 16;
    tooltip(() -> Component.literal(Colors.hex(getter.getAsInt())));
  }

  /** Shows the hex code next to the color */
  public ColorSwatch withHex() {
    this.showHex = true;
    this.w = 64;
    return this;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    boolean hover = isHovered(mx, my);
    int color = getter.getAsInt() | 0xFF000000;
    Theme.outlined(g, x, y, w, h, Theme.INPUT, hover ? Theme.BORDER_HOVER : Theme.BORDER);
    int size = h - 4;
    g.fill(x + 2, y + 2, x + 2 + size, y + 2 + size, color);
    if (showHex) {
      Theme.text(g, Colors.hex(color), x + size + 6, y + (h - 8) / 2, Theme.TEXT);
    }
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (button == 0) {
      new ColorPickerPopup(getter.getAsInt(), setter).openAt(x, y + h + 2);
      return true;
    }
    return false;
  }
}
