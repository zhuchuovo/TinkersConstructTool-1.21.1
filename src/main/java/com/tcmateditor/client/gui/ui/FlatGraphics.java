package com.tcmateditor.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;

/**
 * Graphics that draws text without the vanilla drop shadow, for widgets that hardcode the shadow.
 * Shares the buffer source of the screen, so call {@link #flush()} before drawing continues on the parent.
 */
public class FlatGraphics extends GuiGraphics {
  public FlatGraphics(GuiGraphics parent) {
    super(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
    pose().last().pose().set(parent.pose().last().pose());
  }

  @Override
  public int drawString(Font font, @Nullable String text, int x, int y, int color) {
    return drawString(font, text, x, y, color, false);
  }

  @Override
  public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
    return drawString(font, text, x, y, color, false);
  }

  @Override
  public int drawString(Font font, Component text, int x, int y, int color) {
    return drawString(font, text, x, y, color, false);
  }
}
