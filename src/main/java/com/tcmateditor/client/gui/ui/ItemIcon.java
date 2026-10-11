package com.tcmateditor.client.gui.ui;

import com.tcmateditor.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Item stack in a small frame, optionally clickable */
public class ItemIcon extends UIElement {
  private final Supplier<ItemStack> stack;
  @Nullable
  private Runnable onClick;
  public boolean frame = true;

  public ItemIcon(Supplier<ItemStack> stack) {
    this.stack = stack;
    this.w = 18;
    this.h = 18;
    tooltip(() -> {
      ItemStack value = stack.get();
      return value.isEmpty() ? null : value.getHoverName();
    });
  }

  public ItemIcon onClick(Runnable onClick) {
    this.onClick = onClick;
    return this;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    if (frame) {
      boolean hover = onClick != null && isHovered(mx, my);
      Theme.outlined(g, x, y, w, h, Theme.INPUT, hover ? Theme.BORDER_HOVER : Theme.BORDER);
    }
    ItemStack value = stack.get();
    if (!value.isEmpty()) {
      g.renderItem(value, x + (w - 16) / 2, y + (h - 16) / 2);
    } else if (onClick != null) {
      Theme.centered(g, Component.literal("?"), x + w / 2, y + (h - 8) / 2, Theme.TEXT_FAINT);
    }
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (onClick != null && button == 0) {
      onClick.run();
      return true;
    }
    return false;
  }
}
