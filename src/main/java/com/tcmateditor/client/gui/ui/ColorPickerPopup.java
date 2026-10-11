package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.texture.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/** HSV color picker: saturation/value square, hue bar, hex input and presets. Changes apply live. */
public class ColorPickerPopup extends Popup {
  private static final int SQUARE = 96;
  private static final int[] PRESETS = {
    0xFFFFFFFF, 0xFFC8C8C8, 0xFF7F7F7F, 0xFF3A3A3A, 0xFF111111, 0xFFE33B3B, 0xFFFF8A2B, 0xFFFFD43B,
    0xFF8BE04E, 0xFF2FBF71, 0xFF3BD6D6, 0xFF3B8BFF, 0xFF6F4BFF, 0xFFC04BFF, 0xFFFF5FB4, 0xFF8A5A3B };

  private float hue, sat, val;
  private final IntConsumer onChange;
  private final TextField hex;
  private int dragging = 0;

  public ColorPickerPopup(int color, IntConsumer onChange) {
    this.onChange = onChange;
    float[] hsv = Colors.toHsv(color);
    hue = hsv[0];
    sat = hsv[1];
    val = hsv[2];
    w = SQUARE + 30;
    h = SQUARE + 62;
    hex = add(new TextField(Colors.hex(color), 64));
    hex.at(6, SQUARE + 10);
    hex.maxLength(7);
    hex.validator(s -> Colors.parseHex(s) != null);
    hex.onChange(s -> {
      Integer parsed = Colors.parseHex(s);
      if (parsed != null) {
        float[] values = Colors.toHsv(parsed);
        hue = values[0];
        sat = values[1];
        val = values[2];
        onChange.accept(parsed);
      }
    });
    add(new Button(Component.translatable("tcmateditor.button.done"), this::close).style(Button.Style.PRIMARY)).at(w - 46, SQUARE + 10).size(40, 16);
    for (int i = 0; i < PRESETS.length; i++) {
      int preset = PRESETS[i];
      add(new UIElement() {
        @Override
        public void render(GuiGraphics g, int mx, int my, float partialTick) {
          g.fill(x, y, x + w, y + h, isHovered(mx, my) ? Theme.TEXT : Theme.BORDER);
          g.fill(x + 1, y + 1, x + w - 1, y + h - 1, preset);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
          setColor(preset);
          return true;
        }
      }.at(6 + (i % 8) * 14, SQUARE + 32 + (i / 8) * 13).size(12, 11));
    }
  }

  private int current() {
    return Colors.fromHsv(hue, sat, val, 255);
  }

  private void setColor(int color) {
    float[] values = Colors.toHsv(color);
    hue = values[0];
    sat = values[1];
    val = values[2];
    hex.setValue(Colors.hex(color));
    onChange.accept(color);
  }

  public void openAt(int anchorX, int anchorY) {
    Minecraft mc = Minecraft.getInstance();
    UI.openPopup(this);
    place(anchorX, anchorY, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    super.render(g, mx, my, partialTick);
    int sx = x + 6, sy = y + 6;
    // saturation/value square, drawn in columns
    for (int i = 0; i < SQUARE; i++) {
      float s = i / (float) (SQUARE - 1);
      g.fillGradient(sx + i, sy, sx + i + 1, sy + SQUARE, Colors.fromHsv(hue, s, 1, 255), 0xFF000000);
    }
    int cx = sx + Math.round(sat * (SQUARE - 1));
    int cy = sy + Math.round((1 - val) * (SQUARE - 1));
    g.renderOutline(cx - 2, cy - 2, 5, 5, val > 0.6f ? 0xFF000000 : 0xFFFFFFFF);
    // hue bar
    int hx = sx + SQUARE + 6;
    for (int i = 0; i < SQUARE; i++) {
      g.fill(hx, sy + i, hx + 10, sy + i + 1, Colors.fromHsv(i / (float) SQUARE, 1, 1, 255));
    }
    int hy = sy + Math.round(hue * (SQUARE - 1));
    g.fill(hx - 1, hy - 1, hx + 11, hy + 2, 0xFFFFFFFF);
    g.fill(hx, hy, hx + 10, hy + 1, Colors.fromHsv(hue, 1, 1, 255));
    // current color
    g.fill(x + 74, y + SQUARE + 10, x + 80, y + SQUARE + 26, current());
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    int sx = x + 6, sy = y + 6;
    if (mx >= sx && mx < sx + SQUARE && my >= sy && my < sy + SQUARE) {
      dragging = 1;
      UI.capture(this);
      updateDrag(mx, my);
      return true;
    }
    int hx = sx + SQUARE + 6;
    if (mx >= hx - 2 && mx < hx + 12 && my >= sy && my < sy + SQUARE) {
      dragging = 2;
      UI.capture(this);
      updateDrag(mx, my);
      return true;
    }
    return super.mouseClicked(mx, my, button);
  }

  private void updateDrag(double mx, double my) {
    int sx = x + 6, sy = y + 6;
    if (dragging == 1) {
      sat = (float) Math.max(0, Math.min(1, (mx - sx) / (SQUARE - 1)));
      val = (float) Math.max(0, Math.min(1, 1 - (my - sy) / (SQUARE - 1)));
    } else if (dragging == 2) {
      hue = (float) Math.max(0, Math.min(0.999, (my - sy) / SQUARE));
    }
    int color = current();
    hex.setValue(Colors.hex(color));
    onChange.accept(color);
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    if (dragging != 0) {
      updateDrag(mx, my);
      return true;
    }
    return false;
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    dragging = 0;
    return true;
  }
}
