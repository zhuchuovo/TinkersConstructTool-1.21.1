package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Switch with an optional label to its right */
public class Toggle extends UIElement {
  private final BooleanSupplier getter;
  private final Consumer<Boolean> setter;
  private final Component label;
  public int labelColor = Theme.TEXT;

  public Toggle(Component label, BooleanSupplier getter, Consumer<Boolean> setter) {
    this.label = label;
    this.getter = getter;
    this.setter = setter;
    this.h = 12;
    this.w = 20 + (label.getString().isEmpty() ? 0 : 4 + Theme.font().width(label));
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    boolean on = getter.getAsBoolean();
    boolean hover = isHovered(mx, my);
    int ty = y + (h - 10) / 2;
    int track = on ? (hover ? Theme.ACCENT_HOVER : Theme.ACCENT) : (hover ? Theme.TOGGLE_OFF_HOVER : Theme.TOGGLE_OFF);
    Theme.box(g, x, ty, 18, 10, track);
    int knobX = on ? x + 10 : x + 2;
    Theme.box(g, knobX, ty + 2, 6, 6, 0xFFFFFFFF);
    if (!label.getString().isEmpty()) {
      Theme.ellipsis(g, label, x + 22, y + (h - 8) / 2, w - 22, labelColor);
    }
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (button == 0) {
      setter.accept(!getter.getAsBoolean());
      return true;
    }
    return false;
  }
}
