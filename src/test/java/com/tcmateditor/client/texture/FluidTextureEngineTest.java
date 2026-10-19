package com.tcmateditor.client.texture;

import com.tcmateditor.model.FluidDoc;
import com.tcmateditor.model.TextureDoc;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class FluidTextureEngineTest {
  private static FluidDoc fluid() {
    FluidDoc doc = new FluidDoc();
    doc.id = "custommats:molten_ruby";
    doc.generatedTexture = true;
    doc.texture = FluidDoc.createTexture(0xFFCC2233);
    return doc;
  }

  @Test
  void stylesAndSeedsProduceDistinctRepeatableSurfaces() {
    FluidDoc doc = fluid();
    HashSet<Integer> textures = new HashSet<>();
    for (String style : FluidTextureEngine.STYLES) {
      doc.textureStyle = style;
      Sprite first = FluidTextureEngine.render(doc, false, null);
      assertArrayEquals(first.argb(), FluidTextureEngine.render(doc, false, null).argb());
      textures.add(Arrays.hashCode(first.argb()));
    }
    assertEquals(FluidTextureEngine.STYLES.size(), textures.size());
    Sprite first = FluidTextureEngine.base(doc);
    doc.textureSeed++;
    assertFalse(Arrays.equals(first.argb(), FluidTextureEngine.base(doc).argb()));
  }

  @Test
  void flowingFramesTileFourCopiesAndAnimationChangesFrames() {
    FluidDoc doc = fluid();
    Sprite still = FluidTextureEngine.render(doc, false, null);
    Sprite flowing = FluidTextureEngine.render(doc, true, null);
    assertEquals(16, still.width());
    assertEquals(16 * 16, still.height());
    assertEquals(32, flowing.width());
    assertEquals(32 * 16, flowing.height());
    for (int y = 0; y < flowing.height(); y++) {
      for (int x = 0; x < 16; x++) assertEquals(flowing.get(x, y), flowing.get(x + 16, y));
      if (y % 32 < 16) assertEquals(flowing.get(0, y), flowing.get(0, y + 16));
    }
    assertFalse(Arrays.equals(Arrays.copyOfRange(still.argb(), 0, 256), Arrays.copyOfRange(still.argb(), 256, 512)));
    doc.texture.animation.type = "none";
    assertEquals(16, FluidTextureEngine.render(doc, false, null).height());
    assertEquals(32, FluidTextureEngine.render(doc, true, null).height());
  }

  @Test
  void paletteEffectsAndOpacityReachExportedPixels() {
    FluidDoc doc = fluid();
    doc.texture.animation.type = "none";
    Sprite red = FluidTextureEngine.render(doc, false, null);
    doc.texture.palette = TextureDoc.paletteFromColor(0xFF2244CC);
    Sprite blue = FluidTextureEngine.render(doc, false, null);
    assertFalse(Arrays.equals(red.argb(), blue.argb()));
    TextureDoc.Effect effect = new TextureDoc.Effect();
    effect.type = "tint";
    effect.color = 0xFF00FF00;
    effect.strength = 1;
    doc.texture.effects.add(effect);
    doc.opacity = 100;
    for (int color : FluidTextureEngine.render(doc, false, null).argb()) assertEquals(0x6400FF00, color);
    assertEquals(0xFFFFFFFF, doc.renderTintColor(), "already colored pixels must not be tinted twice");
  }
}
