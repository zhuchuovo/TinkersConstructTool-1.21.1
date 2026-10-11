package com.tcmateditor.client.gui.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Element containing other elements positioned relative to it */
public class UIGroup extends UIElement {
  public final List<UIElement> children = new ArrayList<>();
  /** Optional background color, 0 for none */
  public int background = 0;

  public <T extends UIElement> T add(T child) {
    children.add(child);
    return child;
  }

  public void clear() {
    children.clear();
  }

  /** Origin used for children */
  protected int originX() {
    return x;
  }

  protected int originY() {
    return y;
  }

  @Override
  protected void onLayout() {
    layoutChildren();
  }

  public void layoutChildren() {
    int ox = originX(), oy = originY();
    for (UIElement child : children) {
      child.layout(ox, oy);
    }
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    if (background != 0) {
      g.fill(x, y, x + w, y + h, background);
    }
    renderChildren(g, mx, my, partialTick);
  }

  protected void renderChildren(GuiGraphics g, int mx, int my, float partialTick) {
    for (UIElement child : children) {
      if (child.visible) {
        child.render(g, mx, my, partialTick);
      }
    }
  }

  /** Whether the point can reach children, used by scrolling groups to clip */
  protected boolean acceptsPoint(double mx, double my) {
    return true;
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (!acceptsPoint(mx, my)) {
      return false;
    }
    for (int i = children.size() - 1; i >= 0; i--) {
      UIElement child = children.get(i);
      if (child.visible && child.isHovered(mx, my) && child.mouseClicked(mx, my, button)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    if (!acceptsPoint(mx, my)) {
      return false;
    }
    for (int i = children.size() - 1; i >= 0; i--) {
      UIElement child = children.get(i);
      if (child.visible && child.isHovered(mx, my) && child.mouseScrolled(mx, my, sx, sy)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void tick() {
    for (UIElement child : children) {
      child.tick();
    }
  }

  @Nullable
  @Override
  public Component findTooltip(double mx, double my) {
    if (!isHovered(mx, my) || !acceptsPoint(mx, my)) {
      return null;
    }
    for (int i = children.size() - 1; i >= 0; i--) {
      UIElement child = children.get(i);
      if (child.visible && child.isHovered(mx, my)) {
        Component found = child.findTooltip(mx, my);
        if (found != null) {
          return found;
        }
      }
    }
    return tooltip != null ? tooltip.get() : null;
  }
}
