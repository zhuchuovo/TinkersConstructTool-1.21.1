package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Select box opening a list of options */
public class Dropdown<T> extends UIElement {
  private final List<T> options;
  private final Function<T,Component> label;
  private final Supplier<T> getter;
  private final Consumer<T> setter;
  private Function<T,Integer> color = t -> 0;

  public Dropdown(List<T> options, Function<T,Component> label, Supplier<T> getter, Consumer<T> setter, int width) {
    this.options = options;
    this.label = label;
    this.getter = getter;
    this.setter = setter;
    this.w = width;
    this.h = 16;
  }

  public Dropdown<T> color(Function<T,Integer> color) {
    this.color = color;
    return this;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    boolean hover = isHovered(mx, my);
    Theme.outlined(g, x, y, w, h, hover ? Theme.CARD_HOVER : Theme.CARD, hover ? Theme.BORDER_HOVER : Theme.BORDER);
    T current = getter.get();
    int tx = x + 5;
    int c = current == null ? 0 : color.apply(current);
    if (c != 0) {
      g.fill(tx, y + 5, tx + 6, y + 11, c);
      tx += 10;
    }
    Component text = current == null ? Component.literal("-") : label.apply(current);
    Theme.ellipsis(g, text, tx, y + 4, w - (tx - x) - 14, Theme.TEXT);
    Theme.text(g, "▾", x + w - 10, y + 4, Theme.TEXT_DIM);
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (button != 0) {
      return false;
    }
    List<Component> labels = new ArrayList<>();
    List<Integer> colors = new ArrayList<>();
    for (T option : options) {
      labels.add(label.apply(option));
      colors.add(color.apply(option));
    }
    int selected = options.indexOf(getter.get());
    new ListPopup(labels, colors, selected, Math.max(w, 90), index -> setter.accept(options.get(index))).openBelow(this);
    return true;
  }
}
