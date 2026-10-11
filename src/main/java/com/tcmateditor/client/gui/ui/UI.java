package com.tcmateditor.client.gui.ui;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Focus, mouse capture and popup state of the current editor screen */
public final class UI {
  private UI() {}

  @Nullable
  private static UIElement focused;
  @Nullable
  private static UIElement captured;
  private static boolean focusRequested;
  public static final List<Popup> POPUPS = new ArrayList<>();
  /** Drawn on top of everything this frame, set by hovered elements such as help badges */
  @Nullable
  public static java.util.function.Consumer<net.minecraft.client.gui.GuiGraphics> overlay;
  /** Clip rectangle of the scroll view currently rendering, for widgets that draw with their own graphics */
  @Nullable
  public static net.minecraft.client.gui.navigation.ScreenRectangle clip;
  /** Set when something needs the screen to rebuild its pages */
  private static boolean rebuildRequested;

  public static void reset() {
    focused = null;
    captured = null;
    POPUPS.clear();
    rebuildRequested = false;
  }

  /** Gives keyboard focus to an element, call from mouseClicked */
  public static void focus(@Nullable UIElement element) {
    if (focused != element) {
      if (focused != null) {
        focused.setFocused(false);
      }
      focused = element;
      if (element != null) {
        element.setFocused(true);
      }
    }
    focusRequested = true;
  }

  @Nullable
  public static UIElement focused() {
    return focused;
  }

  public static boolean isFocused(UIElement element) {
    return focused == element;
  }

  /** Captures the mouse so the element gets drag and release events */
  public static void capture(UIElement element) {
    captured = element;
  }

  @Nullable
  public static UIElement captured() {
    return captured;
  }

  public static void release() {
    captured = null;
  }

  public static void beginClick() {
    focusRequested = false;
  }

  /** Clears focus if nothing requested it during the click */
  public static void endClick() {
    if (!focusRequested) {
      focus(null);
      focusRequested = false;
    }
  }

  public static void openPopup(Popup popup) {
    POPUPS.add(popup);
  }

  public static void closePopup(Popup popup) {
    POPUPS.remove(popup);
    popup.onClose();
  }

  public static void closeAllPopups() {
    for (Popup popup : new ArrayList<>(POPUPS)) {
      closePopup(popup);
    }
  }

  public static void requestRebuild() {
    rebuildRequested = true;
  }

  public static boolean consumeRebuild() {
    boolean value = rebuildRequested;
    rebuildRequested = false;
    return value;
  }
}
