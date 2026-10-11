package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Searchable list popup used for picking items, modifiers, stats and materials */
public class SearchPopup extends Popup {
  private static final int ROW = 18;
  private static final int LIMIT = 400;

  /** One pickable entry */
  public record Entry(String id, Component label, Component sub, int color, ItemStack icon) {
    public Entry(String id, Component label, int color) {
      this(id, label, Component.literal(id), color, ItemStack.EMPTY);
    }

    String searchText() {
      return (label.getString() + " " + id).toLowerCase(Locale.ROOT);
    }
  }

  private final List<Entry> entries;
  private final Consumer<Entry> onPick;
  private final ScrollView list;
  private final TextField search;

  public SearchPopup(Component title, List<Entry> entries, Consumer<Entry> onPick) {
    this.entries = entries;
    this.onPick = onPick;
    Minecraft mc = Minecraft.getInstance();
    this.w = Math.min(240, mc.getWindow().getGuiScaledWidth() - 20);
    this.h = Math.min(220, mc.getWindow().getGuiScaledHeight() - 20);
    this.modal = true;
    add(new Label(title, Theme.TEXT)).at(8, 7);
    search = add(new TextField("", w - 16));
    search.at(8, 20);
    search.hint(Component.translatable("tcmateditor.hint.search"));
    search.onChange(s -> refresh());
    list = add(new ScrollView());
    list.at(4, 40);
    list.size(w - 8, h - 44);
    refresh();
  }

  public void open() {
    Minecraft mc = Minecraft.getInstance();
    UI.openPopup(this);
    center(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    UI.focus(search);
  }

  private void refresh() {
    String query = search.getValue().toLowerCase(Locale.ROOT).trim();
    list.clear();
    int count = 0;
    for (Entry entry : entries) {
      if (!query.isEmpty() && !entry.searchText().contains(query)) {
        continue;
      }
      if (count >= LIMIT) {
        break;
      }
      list.add(new Row(entry).at(0, count * ROW).size(list.w - 6, ROW));
      count++;
    }
    list.contentHeight = count * ROW;
    list.setScroll(0);
    list.layoutChildren();
  }

  private class Row extends UIElement {
    private final Entry entry;

    Row(Entry entry) {
      this.entry = entry;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
      if (isHovered(mx, my)) {
        Theme.box(g, x, y, w, h, Theme.CARD_HOVER);
      }
      int tx = x + 4;
      if (!entry.icon.isEmpty()) {
        g.pose().pushPose();
        g.pose().translate(tx, y + 1, 0);
        g.pose().scale(1f, 1f, 1f);
        g.renderItem(entry.icon, 0, 0);
        g.pose().popPose();
        tx += 20;
      } else if (entry.color != 0) {
        g.fill(tx, y + 5, tx + 8, y + 13, entry.color);
        tx += 12;
      }
      boolean hasSub = !entry.sub.getString().isEmpty() && !entry.sub.getString().equals(entry.label.getString());
      if (hasSub) {
        int labelW = Math.min(Theme.font().width(entry.label), (w - (tx - x)) * 2 / 3);
        Theme.ellipsis(g, entry.label, tx, y + 5, labelW, Theme.TEXT);
        Theme.ellipsis(g, entry.sub, tx + labelW + 6, y + 5, w - (tx - x) - labelW - 10, Theme.TEXT_FAINT);
      } else {
        Theme.ellipsis(g, entry.label, tx, y + 5, w - (tx - x) - 4, Theme.TEXT);
      }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
      close();
      onPick.accept(entry);
      return true;
    }
  }
}
