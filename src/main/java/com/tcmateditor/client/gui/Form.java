package com.tcmateditor.client.gui;

import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.ScrollView;
import com.tcmateditor.client.gui.ui.UIElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Vertical layout helper for page content */
public class Form {
  public final ScrollView view;
  public final int pad = 8;
  public int labelWidth = 86;
  public int y = 6;

  public Form(ScrollView view) {
    this.view = view;
    this.labelWidth = Math.min(96, Math.max(60, (view.w - 20) / 4));
  }

  /** Usable width inside the padding */
  public int width() {
    return view.w - pad * 2 - 4;
  }

  /** Width available for a control placed after a row label */
  public int controlWidth() {
    return width() - labelWidth;
  }

  public void gap(int amount) {
    y += amount;
  }

  /** Section title with a divider */
  public void header(Component text) {
    y += 4;
    int lineY = y + 11;
    view.add(new UIElement() {
      @Override
      public void render(GuiGraphics g, int mx, int my, float partialTick) {
        Theme.text(g, text, x, y, Theme.TEXT);
        g.fill(x, y + 11, x + w, y + 12, Theme.BORDER);
      }
    }.at(pad, y).size(width(), 12));
    y = lineY + 5;
  }

  /** Short dim explanation */
  public Label hint(Component text) {
    Label label = new Label(text, Theme.TEXT_FAINT).wrap(width());
    view.add(label).at(pad, y);
    y += label.h + 5;
    return label;
  }

  /** Label on the left, control on the right */
  public <T extends UIElement> T row(Component label, T control) {
    int rowH = Math.max(12, control.h);
    int labelY = rowH > 24 ? y + 4 : y + (rowH - 8) / 2;
    view.add(new Label(label, Theme.TEXT_DIM)).at(pad, labelY).size(labelWidth - 6, 9);
    view.add(control).at(pad + labelWidth, y + (rowH - control.h) / 2);
    y += rowH + 4;
    return control;
  }

  /** Element across the whole width */
  public <T extends UIElement> T full(T element) {
    view.add(element).at(pad, y);
    y += element.h + 4;
    return element;
  }

  /** Places an element at an x offset on the current line without advancing */
  public <T extends UIElement> T place(T element, int offsetX) {
    view.add(element).at(pad + offsetX, y);
    return element;
  }

  /** Advances by a line height */
  public void next(int height) {
    y += height + 4;
  }

  public void finish() {
    view.contentHeight = y + 10;
  }
}
