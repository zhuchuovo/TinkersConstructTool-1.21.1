package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

/** Horizontal slider with the value shown on the right */
public class Slider extends UIElement {
  private final DoubleSupplier getter;
  private final DoubleConsumer setter;
  private final double min, max;
  private final boolean integer;
  private DoubleFunction<String> format;
  private boolean dragging;

  public Slider(double min, double max, boolean integer, DoubleSupplier getter, DoubleConsumer setter, int width) {
    this.min = min;
    this.max = max;
    this.integer = integer;
    this.getter = getter;
    this.setter = setter;
    this.w = width;
    this.h = 14;
    this.format = v -> integer ? Long.toString(Math.round(v)) : Math.round(v * 100) + "%";
  }

  public Slider format(DoubleFunction<String> format) {
    this.format = format;
    return this;
  }

  private int trackWidth() {
    return w - 30;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    double value = getter.getAsDouble();
    double t = (value - min) / (max - min);
    int tw = trackWidth();
    int cy = y + h / 2;
    g.fill(x, cy - 1, x + tw, cy + 1, Theme.BORDER);
    int knob = x + (int) Math.round(t * (tw - 6));
    g.fill(x, cy - 1, knob + 3, cy + 1, Theme.ACCENT);
    boolean hover = dragging || isHovered(mx, my);
    Theme.box(g, knob, cy - 4, 6, 8, hover ? Theme.KNOB_HOVER : Theme.KNOB);
    Theme.text(g, format.apply(value), x + tw + 5, y + (h - 8) / 2, Theme.TEXT_DIM);
  }

  private void update(double mx) {
    double t = (mx - x - 3) / (double) (trackWidth() - 6);
    t = Math.max(0, Math.min(1, t));
    double value = min + t * (max - min);
    if (integer) {
      value = Math.round(value);
    } else {
      value = Math.round(value * 100) / 100.0;
    }
    setter.accept(value);
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (button == 0 && mx < x + trackWidth() + 2) {
      dragging = true;
      UI.capture(this);
      update(mx);
      return true;
    }
    return false;
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    if (dragging) {
      update(mx);
      return true;
    }
    return false;
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    dragging = false;
    return true;
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    double step = integer ? 1 : 0.05;
    double value = Math.max(min, Math.min(max, getter.getAsDouble() + step * Math.signum(sy)));
    setter.accept(integer ? Math.round(value) : Math.round(value * 100) / 100.0);
    return true;
  }
}
