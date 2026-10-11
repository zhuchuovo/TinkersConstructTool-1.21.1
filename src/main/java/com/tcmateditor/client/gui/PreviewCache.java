package com.tcmateditor.client.gui;

import com.tcmateditor.client.texture.PartTextureLibrary;
import com.tcmateditor.client.texture.PartTextureLibrary.PartInfo;
import com.tcmateditor.client.texture.Sprite;
import com.tcmateditor.client.texture.TextureEngine;
import com.tcmateditor.model.TextureDoc;
import com.tcmateditor.model.Workspace;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Live texture previews for the texture page, regenerated when the texture settings change */
public final class PreviewCache {
  private PreviewCache() {}

  public record Preview(String key, Component name, List<ResourceLocation> layers) {}

  private static ResourceLocation tc(String path) {
    return ResourceLocation.fromNamespaceAndPath("tconstruct", path);
  }

  public static final List<Preview> PREVIEWS = List.of(
    new Preview("pickaxe", Names.tr("preview.pickaxe"), List.of(tc("item/tool/pickaxe/handle"), tc("item/tool/pickaxe/head"), tc("item/tool/pickaxe/binding"))),
    new Preview("sword", Names.tr("preview.sword"), List.of(tc("item/tool/sword/handle"), tc("item/tool/sword/blade"), tc("item/tool/sword/guard"))),
    new Preview("chestplate", Names.tr("preview.chestplate"), List.of(tc("item/tool/armor/plate/chestplate/plating"), tc("item/tool/armor/plate/chestplate/maille"))),
    new Preview("handle", Names.statType("tconstruct:handle"), List.of(tc("item/tool/parts/tool_handle"))),
    new Preview("binding", Names.statType("tconstruct:binding"), List.of(tc("item/tool/parts/tool_binding"))),
    new Preview("plate", Names.tr("preview.large_plate"), List.of(tc("item/tool/parts/large_plate"))),
    new Preview("repair_kit", Names.statType("tconstruct:repair_kit"), List.of(tc("item/tool/parts/repair_kit"))));

  private static String lastKey = null;
  private static final Map<String,Sprite> SPRITES = new HashMap<>();

  /** Gets the preview sprite for the texture, regenerating all previews if the texture changed */
  @Nullable
  public static Sprite get(TextureDoc texture, Preview preview) {
    String key = Workspace.GSON.toJson(texture);
    if (!key.equals(lastKey)) {
      lastKey = key;
      SPRITES.clear();
      for (Preview p : PREVIEWS) {
        Sprite sprite = render(texture, p);
        if (sprite != null) {
          SPRITES.put(p.key, sprite);
        }
      }
    }
    return SPRITES.get(preview.key);
  }

  public static void invalidate() {
    lastKey = null;
  }

  @Nullable
  private static Sprite render(TextureDoc texture, Preview preview) {
    Sprite result = null;
    for (ResourceLocation layer : preview.layers) {
      Sprite base = PartTextureLibrary.base(layer, texture.baseStyle);
      if (base == null) {
        continue;
      }
      PartInfo info = PartTextureLibrary.find(layer);
      boolean animated = (info == null || info.allowAnimated()) && texture.animation.isActive();
      Sprite rendered;
      try {
        rendered = TextureEngine.render(texture, base, animated, PartTextureLibrary::pattern);
      } catch (RuntimeException e) {
        continue;
      }
      if (result == null) {
        result = Sprite.blank(rendered.width(), rendered.height());
      }
      if (rendered.height() != result.height() && rendered.isAnimated() != result.isAnimated()) {
        // mixing animated and static layers, repeat the static layer on every frame
        rendered = repeatFrames(rendered.firstFrame(), result.height() / Math.max(1, result.width()));
      }
      result.blendOver(rendered);
    }
    return result;
  }

  private static Sprite repeatFrames(Sprite frame, int frames) {
    Sprite out = Sprite.blank(frame.width(), frame.height() * Math.max(1, frames));
    for (int f = 0; f < Math.max(1, frames); f++) {
      System.arraycopy(frame.argb(), 0, out.argb(), f * frame.argb().length, frame.argb().length);
    }
    return out;
  }
}
