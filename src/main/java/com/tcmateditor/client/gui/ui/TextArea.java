package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Multi line text input in the flat style */
public class TextArea extends UIElement {
  private final FlatBox box;

  private static class FlatBox extends MultiLineEditBox {
    FlatBox(int width, int height, Component placeholder) {
      super(Theme.font(), 0, 0, width, height, placeholder, Component.empty());
    }

    @Override
    protected void renderBorder(GuiGraphics g, int x, int y, int width, int height) {
      // drawn by TextArea#render so the light theme color modulation does not tint it
    }

    @Override
    protected void renderDecorations(GuiGraphics g) {
      // slim scrollbar inside the box instead of the vanilla one outside
      int max = getMaxScrollAmount();
      if (max > 0) {
        int barH = Math.max(12, height * height / (height + max));
        int barY = getY() + (int) ((height - barH) * (scrollAmount() / max));
        g.fill(getX() + width - 3, barY, getX() + width - 1, barY + barH, Theme.BORDER_HOVER);
      }
    }

    boolean canScroll() {
      return getMaxScrollAmount() > 0;
    }

    void setPos(int x, int y) {
      setX(x);
      setY(y);
    }
  }

  public TextArea(String value, int width, int height, Component placeholder) {
    this.w = width;
    this.h = height;
    box = new FlatBox(width, height, placeholder);
    box.setCharacterLimit(4000);
    box.setValue(value == null ? "" : value);
  }

  public TextArea onChange(Consumer<String> listener) {
    box.setValueListener(listener);
    return this;
  }

  public String getValue() {
    return box.getValue();
  }

  @Override
  protected void onLayout() {
    box.setPos(x, y);
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    Theme.outlined(g, x, y, w, h, Theme.INPUT, box.isFocused() ? Theme.ACCENT : isHovered(mx, my) ? Theme.BORDER_HOVER : Theme.BORDER);
    // vanilla hardcodes a text shadow here, draw through graphics without shadows
    net.minecraft.client.gui.navigation.ScreenRectangle clip = UI.clip;
    FlatGraphics flat = new FlatGraphics(g);
    if (clip != null) {
      flat.enableScissor(clip.left(), clip.top(), clip.right(), clip.bottom());
    }
    if (Theme.isLight()) {
      // vanilla draws this text light grey; darken it for the light background (placeholder stays lighter)
      float shade = box.getValue().isEmpty() && !box.isFocused() ? 0.6f : 0.13f;
      flat.setColor(shade, shade, shade, 1f);
      box.render(flat, mx, my, partialTick);
      flat.setColor(1f, 1f, 1f, 1f);
    } else {
      box.render(flat, mx, my, partialTick);
    }
    flat.flush();
    if (clip != null) {
      flat.disableScissor();
      // restore the scroll view clip on the real graphics
      g.enableScissor(clip.left(), clip.top(), clip.right(), clip.bottom());
      g.disableScissor();
    }
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    UI.focus(this);
    UI.capture(this);
    box.mouseClicked(mx, my, button);
    return true;
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    return box.mouseDragged(mx, my, button, dx, dy);
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    return box.mouseReleased(mx, my, button);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    return box.canScroll() && box.mouseScrolled(mx, my, sy);
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    return box.keyPressed(key, scan, modifiers);
  }

  @Override
  public boolean charTyped(char c, int modifiers) {
    return box.charTyped(c, modifiers);
  }

  @Override
  public void setFocused(boolean focused) {
    box.setFocused(focused);
  }
}
