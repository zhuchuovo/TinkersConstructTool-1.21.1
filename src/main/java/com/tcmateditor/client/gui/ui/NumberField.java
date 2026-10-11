package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/** Numeric input. Mouse wheel adjusts the value (Shift = x10). */
public class NumberField extends TextField {
  private final boolean integer;
  private final double min, max, step;
  private DoubleConsumer onValue = v -> {};
  /** Optional suffix drawn inside the field, like % */
  private String suffix = "";

  public NumberField(double value, boolean integer, double min, double max, double step, int width) {
    super(format(value, integer), width);
    this.integer = integer;
    this.min = min;
    this.max = max;
    this.step = step;
    filter(s -> s.isEmpty() || s.equals("-") || s.matches(integer ? "-?\\d{0,9}" : "-?\\d{0,9}(\\.\\d{0,4})?"));
    validator(s -> {
      Double v = parse(s);
      return v != null && v >= min && v <= max;
    });
    onChange(s -> {
      Double v = parse(s);
      if (v != null && v >= min && v <= max) {
        onValue.accept(v);
      }
    });
    tooltip(Component.translatable("tcmateditor.hint.number", format(min, integer), format(max, integer)));
  }

  public NumberField onValue(DoubleConsumer onValue) {
    this.onValue = onValue;
    return this;
  }

  public NumberField suffix(String suffix) {
    this.suffix = suffix;
    return this;
  }

  private static Double parse(String text) {
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static String format(double value, boolean integer) {
    if (integer) {
      return Long.toString(Math.round(value));
    }
    String text = String.format(Locale.ROOT, "%.4f", value);
    // trim trailing zeros
    text = text.replaceAll("0+$", "");
    if (text.endsWith(".")) {
      text = text.substring(0, text.length() - 1);
    }
    return text.equals("-0") ? "0" : text;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    super.render(g, mx, my, partialTick);
    if (!suffix.isEmpty()) {
      int sw = Theme.font().width(suffix);
      Theme.text(g, suffix, x + w - sw - 4, y + (h - 8) / 2, Theme.TEXT_FAINT);
    }
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    Double current = parse(getValue());
    double value = current == null ? 0 : current;
    double amount = step * (Screen.hasShiftDown() ? 10 : 1) * Math.signum(sy);
    value = Math.max(min, Math.min(max, value + amount));
    if (!integer) {
      value = Math.round(value * 10000) / 10000.0;
    }
    String text = format(value, integer);
    setValue(text);
    onValue.accept(value);
    return true;
  }
}
