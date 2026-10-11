package com.tcmateditor.client.gui.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Base of the editor widget kit.
 * Positions: {@link #lx}/{@link #ly} are relative to the parent content origin, {@link #x}/{@link #y} are absolute after layout.
 */
public abstract class UIElement {
  public int x, y, w, h;
  public int lx, ly;
  public boolean visible = true;
  @Nullable
  protected Supplier<Component> tooltip;

  @SuppressWarnings("unchecked")
  public <T extends UIElement> T at(int lx, int ly) {
    this.lx = lx;
    this.ly = ly;
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends UIElement> T size(int w, int h) {
    this.w = w;
    this.h = h;
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends UIElement> T tooltip(@Nullable Supplier<Component> tooltip) {
    this.tooltip = tooltip;
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends UIElement> T tooltip(Component tooltip) {
    this.tooltip = () -> tooltip;
    return (T) this;
  }

  /** Positions this element relative to an origin */
  public void layout(int originX, int originY) {
    x = originX + lx;
    y = originY + ly;
    onLayout();
  }

  protected void onLayout() {}

  public boolean isHovered(double mx, double my) {
    return visible && mx >= x && my >= y && mx < x + w && my < y + h;
  }

  public abstract void render(GuiGraphics g, int mx, int my, float partialTick);

  public boolean mouseClicked(double mx, double my, int button) { return false; }
  public boolean mouseReleased(double mx, double my, int button) { return false; }
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return false; }
  public boolean mouseScrolled(double mx, double my, double sx, double sy) { return false; }
  public boolean keyPressed(int key, int scan, int modifiers) { return false; }
  public boolean charTyped(char c, int modifiers) { return false; }
  public void setFocused(boolean focused) {}
  public void tick() {}

  /** Tooltip under the mouse, deepest element first */
  @Nullable
  public Component findTooltip(double mx, double my) {
    return isHovered(mx, my) && tooltip != null ? tooltip.get() : null;
  }
}
