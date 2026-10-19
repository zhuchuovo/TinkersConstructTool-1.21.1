package com.tcmateditor.client.gui;

import com.tcmateditor.client.texture.FluidTextureEngine;
import com.tcmateditor.client.texture.PartTextureLibrary;
import com.tcmateditor.client.texture.Sprite;
import com.tcmateditor.model.FluidDoc;
import com.tcmateditor.model.Workspace;

/** Keeps generated texture previews out of the per-frame rendering path. */
public final class FluidPreviewCache {
  private static String key = "";
  private static Sprite still, flowing;
  private FluidPreviewCache() {}

  public static void invalidate() { key = ""; }

  public static Sprite get(FluidDoc doc, boolean flow) {
    String next = Workspace.GSON.toJson(doc);
    if (!next.equals(key)) {
      still = FluidTextureEngine.render(doc, false, PartTextureLibrary::pattern);
      flowing = FluidTextureEngine.render(doc, true, PartTextureLibrary::pattern);
      key = next;
    }
    return flow ? flowing : still;
  }
}
