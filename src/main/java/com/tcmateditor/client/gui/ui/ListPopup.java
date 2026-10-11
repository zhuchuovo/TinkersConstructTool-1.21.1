package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntConsumer;

/** Simple option list shown under a dropdown */
public class ListPopup extends Popup {
  private static final int ROW = 14;

  public ListPopup(List<Component> options, List<Integer> colors, int selected, int width, IntConsumer onSelect) {
    int maxRows = 12;
    this.w = width;
    ScrollView view = add(new ScrollView());
    view.at(2, 2);
    view.size(width - 4, Math.min(options.size(), maxRows) * ROW);
    this.h = view.h + 4;
    for (int i = 0; i < options.size(); i++) {
      int index = i;
      Component text = options.get(i);
      int color = colors != null && i < colors.size() ? colors.get(i) : 0;
      view.add(new UIElement() {
        @Override
        public void render(GuiGraphics g, int mx, int my, float partialTick) {
          boolean hover = isHovered(mx, my);
          if (hover || index == selected) {
            g.fill(x, y, x + w, y + h, hover ? Theme.CARD_HOVER : Theme.ACCENT_DIM);
          }
          int tx = x + 5;
          if (color != 0) {
            g.fill(tx, y + 4, tx + 6, y + 10, color);
            tx += 10;
          }
          Theme.ellipsis(g, text, tx, y + 3, w - (tx - x) - 4, Theme.TEXT);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
          close();
          onSelect.accept(index);
          return true;
        }
      }.at(0, i * ROW).size(width - 8, ROW));
    }
    view.contentHeight = options.size() * ROW;
    if (selected > maxRows / 2) {
      view.setScroll((selected - maxRows / 2) * ROW);
    }
  }

  /** Opens below the given element */
  public ListPopup openBelow(UIElement anchor) {
    Minecraft mc = Minecraft.getInstance();
    UI.openPopup(this);
    place(anchor.x, anchor.y + anchor.h + 1, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    if (y < anchor.y + anchor.h) {
      // not enough room below, open above instead
      place(anchor.x, Math.max(4, anchor.y - h - 1), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }
    return this;
  }
}
