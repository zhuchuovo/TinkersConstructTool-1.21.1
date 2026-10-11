package com.tcmateditor.client.gui.ui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tcmateditor.TCMatEditor;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.texture.Colors;
import com.tcmateditor.client.texture.Sprite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Draws a generated sprite through a dynamic texture, animating vertical frame strips */
public class SpritePreview extends UIElement {
  /** Texture slots reused between rebuilds */
  private static final Map<String,Slot> SLOTS = new HashMap<>();

  private static class Slot {
    final ResourceLocation location;
    DynamicTexture texture;
    int width, height;
    Sprite uploaded;

    Slot(String key) {
      this.location = TCMatEditor.id("preview/" + key.toLowerCase().replaceAll("[^a-z0-9_/.-]", "_"));
    }
  }

  private final String key;
  private final Supplier<Sprite> sprite;
  /** Ticks per frame for animated strips */
  private final Supplier<Integer> frametime;
  public boolean background = true;

  public SpritePreview(String key, Supplier<Sprite> sprite, Supplier<Integer> frametime, int size) {
    this.key = key;
    this.sprite = sprite;
    this.frametime = frametime;
    this.w = size;
    this.h = size;
  }

  /** Releases all preview textures */
  public static void releaseAll() {
    for (Slot slot : SLOTS.values()) {
      if (slot.texture != null) {
        Minecraft.getInstance().getTextureManager().release(slot.location);
      }
    }
    SLOTS.clear();
  }

  @Nullable
  private Slot upload(Sprite s) {
    Slot slot = SLOTS.computeIfAbsent(key, Slot::new);
    if (slot.uploaded == s && slot.texture != null) {
      return slot;
    }
    if (slot.texture == null || slot.width != s.width() || slot.height != s.height()) {
      if (slot.texture != null) {
        Minecraft.getInstance().getTextureManager().release(slot.location);
      }
      slot.texture = new DynamicTexture(s.width(), s.height(), false);
      slot.width = s.width();
      slot.height = s.height();
      Minecraft.getInstance().getTextureManager().register(slot.location, slot.texture);
    }
    NativeImage pixels = slot.texture.getPixels();
    if (pixels == null) {
      return null;
    }
    for (int y = 0; y < s.height(); y++) {
      for (int x = 0; x < s.width(); x++) {
        pixels.setPixelRGBA(x, y, Colors.toAbgr(s.get(x, y)));
      }
    }
    slot.texture.upload();
    slot.uploaded = s;
    return slot;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partialTick) {
    if (background) {
      Theme.checker(g, x, y, w, h, Math.max(2, w / 8));
    }
    Sprite s = sprite.get();
    if (s == null) {
      return;
    }
    Slot slot = upload(s);
    if (slot == null) {
      return;
    }
    int frameSize = s.width();
    int frames = s.isAnimated() ? s.height() / frameSize : 1;
    int frameHeight = frames > 1 ? frameSize : s.height();
    int frame = 0;
    if (frames > 1) {
      long ticks = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : System.currentTimeMillis() / 50;
      frame = (int) ((ticks / Math.max(1, frametime.get())) % frames);
    }
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    g.blit(slot.location, x, y, w, h, 0, frame * frameHeight, s.width(), frameHeight, s.width(), s.height());
    RenderSystem.disableBlend();
  }
}
