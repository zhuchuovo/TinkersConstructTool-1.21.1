package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.model.AnvilSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Round "?" that shows the anvil slot numbering diagram while hovered */
public class HelpBadge extends UIElement {
  /** Shows the panel without hovering, used by the self test screenshots */
  public static boolean forceOpen = false;

  public HelpBadge() {
    this.w = 12;
    this.h = 12;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    boolean hover = isHovered(mx, my);
    int color = hover ? Theme.ACCENT : Theme.BORDER_HOVER;
    // rounded badge
    g.fill(x + 3, y, x + w - 3, y + h, color);
    g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    g.fill(x, y + 3, x + w, y + h - 3, color);
    Theme.centered(g, Component.literal("?"), x + w / 2 + 1, y + 2, 0xFFFFFFFF);
    if (hover || forceOpen) {
      int px = hover ? mx : x + w;
      int py = hover ? my : y;
      UI.overlay = graphics -> drawPanel(graphics, px, py);
    }
  }

  private static void drawPanel(GuiGraphics g, int mx, int my) {
    Font font = Theme.font();
    int[] diagram = AnvilSlotsEditor.diagramSize();
    int textX = diagram[0] + 18;
    int listWidth = 0;
    for (int i = 0; i < 5; i++) {
      listWidth = Math.max(listWidth, font.width(AnvilSlotsEditor.slotName(i)));
    }
    int width = textX + listWidth + 12;
    List<FormattedCharSequence> note = font.split(Names.tr("help.anvil_note"), width - 16);
    int height = 24 + Math.max(diagram[1], 5 * 12) + 6 + note.size() * 10 + 6;
    int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
    int px = Math.min(mx + 12, screenW - width - 4);
    int py = Math.max(4, Math.min(my - 10, screenH - height - 4));
    g.fill(px + 2, py + 2, px + width + 2, py + height + 2, 0x60000000);
    Theme.outlined(g, px, py, width, height, Theme.PANEL, Theme.BORDER_HOVER);
    Theme.text(g, Names.tr("help.anvil_title"), px + 8, py + 8, Theme.TEXT);
    AnvilSlotsEditor.drawDiagram(g, px + 10, py + 24);
    for (int i = 0; i < 5; i++) {
      int ly = py + 26 + i * 12;
      Component name = AnvilSlotsEditor.slotName(i);
      Theme.text(g, name, px + 10 + textX, ly, AnvilSlots.onStation(i) ? Theme.TEXT : Theme.TEXT_DIM);
    }
    int ny = py + 24 + Math.max(diagram[1], 5 * 12) + 6;
    for (FormattedCharSequence line : note) {
      g.drawString(font, line, px + 8, ny, Theme.TEXT_FAINT, false);
      ny += 10;
    }
  }
}
