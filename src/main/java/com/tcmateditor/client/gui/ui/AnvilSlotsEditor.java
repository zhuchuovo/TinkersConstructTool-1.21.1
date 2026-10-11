package com.tcmateditor.client.gui.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tcmateditor.client.gui.Icons;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.model.AnvilSlots;
import com.tcmateditor.model.UpgradeDoc;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The five Tinkers' Anvil input slots arranged like in game, numbered 1-5 clockwise from the lapis slot.
 * Left click selects a slot, right click clears it.
 */
public class AnvilSlotsEditor extends UIElement {
  private static final int SLOT = 20;
  private static final float SPREAD = 1.3f;

  private final Supplier<UpgradeDoc> doc;
  private final IntSupplier selected;
  private final IntConsumer onSelect;
  private final Runnable onClear;

  public AnvilSlotsEditor(Supplier<UpgradeDoc> doc, IntSupplier selected, IntConsumer onSelect, Runnable onClear) {
    this.doc = doc;
    this.selected = selected;
    this.onSelect = onSelect;
    this.onClear = onClear;
    this.w = size(SLOT, SPREAD)[0];
    this.h = size(SLOT, SPREAD)[1];
  }

  /** Offsets of the slots relative to the top left, index 5 is the tool slot */
  private static int[][] offsets(float spread) {
    int[][] result = new int[6][2];
    for (int i = 0; i < 5; i++) {
      result[i][0] = Math.round((AnvilSlots.POSITION[i][0] - 11) * spread);
      result[i][1] = Math.round((AnvilSlots.POSITION[i][1] - 19) * spread);
    }
    result[5][0] = Math.round((AnvilSlots.TOOL_POSITION[0] - 11) * spread);
    result[5][1] = Math.round((AnvilSlots.TOOL_POSITION[1] - 19) * spread);
    return result;
  }

  private static int[] size(int slot, float spread) {
    int[][] offsets = offsets(spread);
    int maxX = 0, maxY = 0;
    for (int[] offset : offsets) {
      maxX = Math.max(maxX, offset[0]);
      maxY = Math.max(maxY, offset[1]);
    }
    return new int[] { maxX + slot, maxY + slot };
  }

  public static ResourceLocation icon(String name) {
    return ResourceLocation.fromNamespaceAndPath("tconstruct", "textures/gui/tinker_pattern/" + name + ".png");
  }

  /** Name of the slot, e.g. "1 · Lapis (top)" */
  public static MutableComponent slotName(int slot) {
    return Component.literal((slot + 1) + " · ").append(Names.tr("slot_icon." + AnvilSlots.ICONS[slot]));
  }

  /** Faded slot icon like the empty slots in the anvil */
  private static void ghost(GuiGraphics g, ResourceLocation texture, int x, int y) {
    RenderSystem.enableBlend();
    g.setColor(1f, 1f, 1f, Theme.isLight() ? 0.55f : 0.6f);
    g.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    g.setColor(1f, 1f, 1f, 1f);
    RenderSystem.disableBlend();
  }

  private static void slotFrame(GuiGraphics g, int x, int y, int size, int border) {
    Theme.outlined(g, x, y, size, size, Theme.INPUT, border);
  }

  private static void badge(GuiGraphics g, int x, int y, int number, boolean highlight) {
    g.fill(x - 3, y - 3, x + 5, y + 6, highlight ? Theme.ACCENT : Theme.ACCENT_DIM);
    Theme.text(g, Integer.toString(number), x - 1, y - 2, highlight ? 0xFFFFFFFF : Theme.TEXT);
  }

  private int slotAt(double mx, double my) {
    int[][] offsets = offsets(SPREAD);
    for (int i = 0; i < 5; i++) {
      int sx = x + offsets[i][0], sy = y + offsets[i][1];
      if (mx >= sx && my >= sy && mx < sx + SLOT && my < sy + SLOT) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    UpgradeDoc upgrade = doc.get();
    int[][] offsets = offsets(SPREAD);
    int hovered = isHovered(mx, my) ? slotAt(mx, my) : -1;
    // tool slot in the middle
    int tx = x + offsets[5][0], ty = y + offsets[5][1];
    slotFrame(g, tx, ty, SLOT, Theme.BORDER);
    ghost(g, icon("pickaxe"), tx + 2, ty + 2);
    for (int i = 0; i < 5; i++) {
      int sx = x + offsets[i][0], sy = y + offsets[i][1];
      boolean sel = selected.getAsInt() == i;
      int border = sel ? Theme.ACCENT : hovered == i ? Theme.BORDER_HOVER : Theme.BORDER;
      slotFrame(g, sx, sy, SLOT, border);
      if (sel) {
        g.renderOutline(sx - 1, sy - 1, SLOT + 2, SLOT + 2, Theme.ACCENT);
      }
      UpgradeDoc.SlotInput input = upgrade.slot(i);
      ItemStack stack = input.isEmpty() ? ItemStack.EMPTY : Icons.forRef(input.ingredient);
      if (!stack.isEmpty()) {
        g.renderItem(stack, sx + 2, sy + 2);
        if (input.count > 1) {
          g.pose().pushPose();
          g.pose().translate(0, 0, 200);
          String count = Integer.toString(input.count);
          g.drawString(Theme.font(), count, sx + SLOT - 2 - Theme.font().width(count), sy + SLOT - 9, 0xFFFFFFFF, true);
          g.pose().popPose();
        }
      } else if (!input.isEmpty()) {
        // unknown item or empty tag
        Theme.centered(g, Component.literal("?"), sx + SLOT / 2, sy + 6, Theme.DANGER);
      } else {
        ghost(g, icon(AnvilSlots.ICONS[i]), sx + 2, sy + 2);
      }
      g.pose().pushPose();
      g.pose().translate(0, 0, 250);
      badge(g, sx, sy, i + 1, sel);
      g.pose().popPose();
    }
  }

  @Nullable
  @Override
  public Component findTooltip(double mx, double my) {
    if (!isHovered(mx, my)) {
      return null;
    }
    int slot = slotAt(mx, my);
    if (slot < 0) {
      return null;
    }
    UpgradeDoc.SlotInput input = doc.get().slot(slot);
    MutableComponent text = slotName(slot);
    if (!input.isEmpty()) {
      text.append("\n").append(Component.literal(input.ingredient + (input.count > 1 ? " ×" + input.count : "")).withColor(Theme.TEXT_DIM));
    }
    if (!AnvilSlots.onStation(slot)) {
      text.append("\n").append(Names.tr("hint.anvil_only").withColor(Theme.WARNING));
    }
    return text;
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    int slot = slotAt(mx, my);
    if (slot < 0) {
      return false;
    }
    onSelect.accept(slot);
    if (button == 1) {
      onClear.run();
    }
    return true;
  }

  /** Small numbered diagram used by the help badge */
  public static void drawDiagram(GuiGraphics g, int x, int y) {
    float spread = 1.15f;
    int slot = 18;
    int[][] offsets = offsets(spread);
    slotFrame(g, x + offsets[5][0], y + offsets[5][1], slot, Theme.BORDER);
    ghost(g, icon("pickaxe"), x + offsets[5][0] + 1, y + offsets[5][1] + 1);
    for (int i = 0; i < 5; i++) {
      int sx = x + offsets[i][0], sy = y + offsets[i][1];
      slotFrame(g, sx, sy, slot, Theme.BORDER_HOVER);
      ghost(g, icon(AnvilSlots.ICONS[i]), sx + 1, sy + 1);
      badge(g, sx, sy, i + 1, true);
    }
  }

  public static int[] diagramSize() {
    return size(18, 1.15f);
  }
}
