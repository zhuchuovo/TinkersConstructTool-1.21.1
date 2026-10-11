package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Floating panel drawn above everything; clicking outside closes it */
public class Popup extends UIGroup {
  /** If true, the rest of the screen is dimmed */
  public boolean modal = false;

  public Popup() {
    background = 0;
  }

  /** Places the popup near an anchor, keeping it on screen */
  public void place(int anchorX, int anchorY, int screenW, int screenH) {
    int px = Math.max(4, Math.min(anchorX, screenW - w - 4));
    int py = anchorY;
    if (py + h > screenH - 4) {
      py = Math.max(4, screenH - h - 4);
    }
    layout(px - lx, py - ly);
  }

  public void center(int screenW, int screenH) {
    lx = 0;
    ly = 0;
    layout((screenW - w) / 2, (screenH - h) / 2);
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    if (modal) {
      g.fill(0, 0, 10000, 10000, 0x90000000);
    }
    // shadow
    g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x60000000);
    Theme.outlined(g, x, y, w, h, Theme.PANEL, Theme.BORDER_HOVER);
    renderChildren(g, mx, my, partialTick);
  }

  public void close() {
    UI.closePopup(this);
  }

  /** Called when closed */
  public void onClose() {}
}
