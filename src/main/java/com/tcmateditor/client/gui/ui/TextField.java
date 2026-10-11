package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** Single line text input built on the vanilla edit box, drawn in the flat style */
public class TextField extends UIElement {
  protected final EditBox box;
  private Predicate<String> validator = s -> true;
  private Consumer<String> onChange = s -> {};
  private boolean suppress = false;

  public TextField(String value, int width) {
    this.w = width;
    this.h = 16;
    box = new EditBox(Theme.font(), 0, 0, width - 8, 9, Component.empty());
    box.setBordered(false);
    box.setTextShadow(false);
    box.setMaxLength(1024);
    box.setTextColor(Theme.TEXT);
    box.setValue(value == null ? "" : value);
    box.moveCursorToStart(false);
    box.setResponder(text -> {
      if (!suppress) {
        onChange.accept(text);
      }
    });
  }

  public TextField onChange(Consumer<String> onChange) {
    this.onChange = onChange;
    return this;
  }

  public TextField validator(Predicate<String> validator) {
    this.validator = validator;
    return this;
  }

  public TextField filter(Predicate<String> filter) {
    box.setFilter(filter);
    return this;
  }

  public TextField hint(Component hint) {
    box.setHint(hint);
    return this;
  }

  public TextField maxLength(int length) {
    box.setMaxLength(length);
    return this;
  }

  public String getValue() {
    return box.getValue();
  }

  /** Sets the value without firing the change listener */
  public void setValue(String value) {
    if (!box.getValue().equals(value)) {
      suppress = true;
      box.setValue(value);
      box.moveCursorToStart(false);
      suppress = false;
    }
  }

  public boolean isValid() {
    return validator.test(box.getValue());
  }

  @Override
  protected void onLayout() {
    box.setX(x + 4);
    box.setY(y + (h - 8) / 2);
    box.setWidth(w - 8);
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    boolean focused = UI.isFocused(this);
    int border = !isValid() ? Theme.DANGER : focused ? Theme.ACCENT : isHovered(mx, my) ? Theme.BORDER_HOVER : Theme.BORDER;
    Theme.outlined(g, x, y, w, h, Theme.INPUT, border);
    g.enableScissor(x + 2, y, x + w - 2, y + h);
    box.render(g, mx, my, partialTick);
    g.disableScissor();
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    UI.focus(this);
    if (button == 1) {
      // right click clears
      box.setValue("");
      return true;
    }
    box.mouseClicked(Math.max(box.getX(), mx), box.getY() + 1, button);
    UI.capture(this);
    return true;
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    return box.mouseDragged(mx, box.getY() + 1, button, dx, dy);
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    return box.mouseReleased(mx, my, button);
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
