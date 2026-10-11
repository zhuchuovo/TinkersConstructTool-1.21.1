package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.model.TextureDoc;
import com.tcmateditor.model.TextureDoc.Stop;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.Supplier;

/**
 * Gradient bar for the grey to color palette.
 * Drag a stop to move it, click a stop to select it, click the bar to add a stop, right click a stop to remove it.
 */
public class PaletteBar extends UIElement {
  private static final int BAR_H = 14;
  private final Supplier<TextureDoc> doc;
  private final Runnable onChange;
  private final Runnable onSelect;
  private Stop selected;
  private Stop dragging;

  public PaletteBar(Supplier<TextureDoc> doc, int width, Runnable onChange, Runnable onSelect) {
    this.doc = doc;
    this.onChange = onChange;
    this.onSelect = onSelect;
    this.w = width;
    this.h = BAR_H + 10;
  }

  public Stop selected() {
    List<Stop> palette = doc.get().palette;
    if (selected == null || !palette.contains(selected)) {
      selected = palette.isEmpty() ? null : palette.get(palette.size() > 5 ? 5 : palette.size() - 1);
    }
    return selected;
  }

  public void select(Stop stop) {
    selected = stop;
  }

  private int greyToX(int grey) {
    return x + 3 + Math.round(grey / 255f * (w - 7));
  }

  private int xToGrey(double mx) {
    return (int) Math.max(0, Math.min(255, Math.round((mx - x - 3) / (w - 7) * 255)));
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    TextureDoc texture = doc.get();
    Theme.outlined(g, x, y, w, BAR_H, Theme.INPUT, Theme.BORDER);
    for (int px = 0; px < w - 6; px++) {
      int grey = Math.round(px / (float) (w - 7) * 255);
      g.fill(x + 3 + px, y + 2, x + 4 + px, y + BAR_H - 2, texture.samplePalette(grey) | 0xFF000000);
    }
    // greyscale reference under the bar
    for (int px = 0; px < w - 6; px++) {
      int grey = Math.round(px / (float) (w - 7) * 255);
      g.fill(x + 3 + px, y + BAR_H, x + 4 + px, y + BAR_H + 2, 0xFF000000 | grey << 16 | grey << 8 | grey);
    }
    Stop sel = selected();
    for (Stop stop : texture.palette) {
      int sx = greyToX(stop.grey);
      boolean isSel = stop == sel;
      boolean hover = Math.abs(mx - sx) <= 3 && my >= y + BAR_H && my < y + h;
      int top = y + BAR_H + 2;
      // marker line on the bar
      g.fill(sx, y + 1, sx + 1, y + BAR_H - 1, isSel ? 0xFFFFFFFF : 0x80FFFFFF);
      // handle
      int border = isSel ? Theme.TEXT : hover ? Theme.BORDER_HOVER : Theme.TEXT_FAINT;
      g.fill(sx - 3, top, sx + 4, top + 7, border);
      g.fill(sx - 2, top + 1, sx + 3, top + 6, stop.color | 0xFF000000);
    }
  }

  private Stop stopAt(double mx, double my) {
    if (my < y + BAR_H) {
      return null;
    }
    Stop best = null;
    double bestDistance = 4;
    for (Stop stop : doc.get().palette) {
      double distance = Math.abs(mx - greyToX(stop.grey));
      if (distance <= bestDistance) {
        best = stop;
        bestDistance = distance;
      }
    }
    return best;
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    TextureDoc texture = doc.get();
    Stop stop = stopAt(mx, my);
    if (button == 1) {
      if (stop != null && texture.palette.size() > 2) {
        texture.palette.remove(stop);
        onChange.run();
        onSelect.run();
      }
      return true;
    }
    if (button != 0) {
      return false;
    }
    if (stop == null) {
      // add a new stop with the current color at that point
      int grey = xToGrey(mx);
      for (Stop existing : texture.palette) {
        if (existing.grey == grey) {
          selected = existing;
          onSelect.run();
          return true;
        }
      }
      stop = new Stop(grey, texture.samplePalette(grey) | 0xFF000000);
      texture.palette.add(stop);
      texture.sortPalette();
      onChange.run();
    }
    selected = stop;
    dragging = stop;
    UI.capture(this);
    onSelect.run();
    return true;
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    if (dragging == null) {
      return false;
    }
    TextureDoc texture = doc.get();
    int grey = xToGrey(mx);
    // stops may not share a grey value
    for (Stop other : texture.palette) {
      if (other != dragging && other.grey == grey) {
        return true;
      }
    }
    if (grey != dragging.grey) {
      dragging.grey = grey;
      texture.sortPalette();
      onChange.run();
      onSelect.run();
    }
    return true;
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    dragging = null;
    return true;
  }
}
