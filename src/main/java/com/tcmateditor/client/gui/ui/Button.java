package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Flat button */
public class Button extends UIElement {
  public enum Style { DEFAULT, PRIMARY, DANGER, GHOST, TAB, CHIP }

  private final Supplier<Component> text;
  private final Runnable onClick;
  public Style style = Style.DEFAULT;
  private BooleanSupplier enabled = () -> true;
  private BooleanSupplier selected = () -> false;
  /** Optional color dot drawn before the text */
  public int dotColor = 0;
  /** Right click handler */
  public Runnable onRightClick;

  public Button(Component text, Runnable onClick) {
    this(() -> text, onClick);
  }

  public Button(Supplier<Component> text, Runnable onClick) {
    this.text = text;
    this.onClick = onClick;
    this.h = 16;
    this.w = Theme.font().width(text.get()) + 12;
  }

  public Button style(Style style) {
    this.style = style;
    return this;
  }

  public Button enabled(BooleanSupplier enabled) {
    this.enabled = enabled;
    return this;
  }

  public Button selected(BooleanSupplier selected) {
    this.selected = selected;
    return this;
  }

  public Button dot(int color) {
    this.dotColor = color;
    if (w > 0) {
      w += 8;
    }
    return this;
  }

  /** Fits the width to the text */
  public Button fit() {
    this.w = Theme.font().width(text.get()) + 12 + (dotColor != 0 ? 8 : 0);
    return this;
  }

  public boolean isEnabled() {
    return enabled.getAsBoolean();
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    boolean hover = isHovered(mx, my) && isEnabled();
    boolean sel = selected.getAsBoolean();
    int fill, border, color;
    switch (style) {
      case PRIMARY -> {
        fill = hover ? Theme.ACCENT_HOVER : Theme.ACCENT;
        border = fill;
        color = 0xFFFFFFFF;
      }
      case DANGER -> {
        fill = hover ? Theme.DANGER : Theme.DANGER_DIM;
        border = Theme.DANGER;
        color = hover ? 0xFFFFFFFF : Theme.ON_DANGER_DIM;
      }
      case GHOST -> {
        fill = hover ? Theme.CARD_HOVER : 0;
        border = fill;
        color = sel ? Theme.TEXT : Theme.TEXT_DIM;
      }
      case TAB -> {
        fill = sel ? Theme.CARD : (hover ? Theme.CARD_HOVER : 0);
        border = fill;
        color = sel ? Theme.TEXT : Theme.TEXT_DIM;
      }
      case CHIP -> {
        fill = sel ? Theme.ACCENT_DIM : (hover ? Theme.CARD_HOVER : Theme.CARD);
        border = sel ? Theme.ACCENT : Theme.BORDER;
        color = sel ? Theme.TEXT : Theme.TEXT_DIM;
      }
      default -> {
        fill = hover ? Theme.CARD_HOVER : Theme.CARD;
        border = hover ? Theme.BORDER_HOVER : Theme.BORDER;
        color = Theme.TEXT;
      }
    }
    if (!isEnabled()) {
      color = Theme.TEXT_FAINT;
    }
    if (fill != 0) {
      if (border != fill) {
        Theme.outlined(g, x, y, w, h, fill, border);
      } else {
        Theme.box(g, x, y, w, h, fill);
      }
    }
    if (style == Style.TAB && sel) {
      g.fill(x + 3, y + h - 2, x + w - 3, y + h, Theme.ACCENT);
    }
    Component value = text.get();
    int textW = Theme.font().width(value) + (dotColor != 0 ? 8 : 0);
    int tx = x + Math.max(4, (w - textW) / 2);
    int ty = y + (h - 8) / 2;
    if (dotColor != 0) {
      g.fill(tx, ty + 1, tx + 5, ty + 6, dotColor);
      tx += 8;
    }
    Theme.ellipsis(g, value, tx, ty, w - (tx - x) - 3, color);
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (!isEnabled()) {
      return true;
    }
    if (button == 0) {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f));
      onClick.run();
      return true;
    }
    if (button == 1 && onRightClick != null) {
      onRightClick.run();
      return true;
    }
    return false;
  }
}
