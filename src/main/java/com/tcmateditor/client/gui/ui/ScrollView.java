package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Vertically scrolling group that clips its children */
public class ScrollView extends UIGroup {
  public int contentHeight = 0;
  private double scroll = 0;
  private boolean draggingBar = false;
  private double dragStartY;
  private double dragStartScroll;

  public double getScroll() {
    return scroll;
  }

  public void setScroll(double value) {
    scroll = Math.max(0, Math.min(maxScroll(), value));
    layoutChildren();
  }

  public int maxScroll() {
    return Math.max(0, contentHeight - h);
  }

  @Override
  protected int originY() {
    return y - (int) Math.round(scroll);
  }

  @Override
  protected boolean acceptsPoint(double mx, double my) {
    return mx >= x && my >= y && mx < x + w && my < y + h;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    if (background != 0) {
      g.fill(x, y, x + w, y + h, background);
    }
    g.enableScissor(x, y, x + w, y + h);
    net.minecraft.client.gui.navigation.ScreenRectangle previousClip = UI.clip;
    UI.clip = new net.minecraft.client.gui.navigation.ScreenRectangle(x, y, w, h);
    boolean inside = acceptsPoint(mx, my);
    // move the mouse away when outside so clipped children don't show hover states
    int cmx = inside ? mx : -10000, cmy = inside ? my : -10000;
    for (UIElement child : children) {
      if (child.visible && child.y + child.h >= y && child.y <= y + h) {
        child.render(g, cmx, cmy, partialTick);
      }
    }
    UI.clip = previousClip;
    g.disableScissor();
    if (maxScroll() > 0) {
      int barH = Math.max(16, h * h / Math.max(1, contentHeight));
      int barY = y + (int) ((h - barH) * (scroll / maxScroll()));
      boolean hover = draggingBar || (mx >= x + w - 5 && mx < x + w && my >= y && my < y + h);
      g.fill(x + w - 3, barY, x + w - 1, barY + barH, hover ? Theme.BORDER_HOVER : Theme.BORDER);
    }
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (!acceptsPoint(mx, my)) {
      return false;
    }
    if (maxScroll() > 0 && button == 0 && mx >= x + w - 5) {
      draggingBar = true;
      dragStartY = my;
      dragStartScroll = scroll;
      UI.capture(this);
      return true;
    }
    return super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    if (draggingBar) {
      int barH = Math.max(16, h * h / Math.max(1, contentHeight));
      double ratio = maxScroll() / (double) Math.max(1, h - barH);
      setScroll(dragStartScroll + (my - dragStartY) * ratio);
      return true;
    }
    return false;
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    draggingBar = false;
    return true;
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    if (!acceptsPoint(mx, my)) {
      return false;
    }
    if (super.mouseScrolled(mx, my, sx, sy)) {
      return true;
    }
    if (maxScroll() > 0) {
      setScroll(scroll - sy * 18);
      return true;
    }
    return false;
  }
}
