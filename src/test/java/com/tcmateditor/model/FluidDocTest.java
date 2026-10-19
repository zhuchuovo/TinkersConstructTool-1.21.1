package com.tcmateditor.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FluidDocTest {
  @TempDir Path folder;

  @Test
  void workspacePersistsRenamesAndDeletesFluids() throws Exception {
    Workspace workspace = new Workspace();
    FluidDoc doc = new FluidDoc();
    doc.id = "custommats:molten_ruby";
    doc.color = "#CC2233";
    doc.temperature = 1300;
    doc.lang.put("en_us", new LangText("Molten Ruby", "", ""));
    workspace.fluids.add(doc);
    workspace.save(folder);
    Workspace loaded = Workspace.load(folder);
    assertEquals(1, loaded.fluids.size());
    FluidDoc read = loaded.fluids.getFirst();
    assertEquals(1300, read.temperature);
    assertEquals(0xFFCC2233, read.colorInt());
    assertEquals("Molten Ruby", read.displayName("ja_jp"));
    read.id = "custommats:renamed";
    loaded.save(folder);
    assertFalse(Files.exists(folder.resolve("fluids/custommats@molten_ruby.json")));
    assertTrue(Files.exists(folder.resolve("fluids/custommats@renamed.json")));
    loaded.fluids.clear();
    loaded.save(folder);
    assertTrue(Workspace.load(folder).fluids.isEmpty());
    assertFalse(Files.exists(folder.resolve("fluids/custommats@renamed.json")));
  }

  @Test
  void copiesAndOldDocumentsKeepSafeDefaults() {
    FluidDoc doc = Workspace.GSON.fromJson("{\"id\":\"custommats:ruby\"}", FluidDoc.class);
    doc.validate();
    assertTrue(doc.bucket);
    assertTrue(doc.block);
    assertEquals(300, doc.temperature);
    assertFalse(doc.generatedTexture, "existing documents keep their external texture references");
    doc.lang.put("zh_cn", new LangText("红宝石", "", ""));
    FluidDoc copy = doc.copy();
    copy.lang.get("zh_cn").name = "复制";
    assertEquals("红宝石", doc.displayName("zh_cn"));
    copy.lightLevel = 100;
    copy.viscosity = -1;
    copy.color = "invalid";
    copy.validate();
    assertEquals(15, copy.lightLevel);
    assertEquals(1, copy.viscosity);
    assertEquals("#FFFFFF", copy.color);
    FluidDoc legacy = Workspace.GSON.fromJson("{\"id\":\"custommats:old\",\"color\":\"#CC2233\"}", FluidDoc.class);
    legacy.validate();
    assertEquals(0xFFCC2233, legacy.texture.mainColor(), "old fluid colors seed the new generator palette");
  }

  @Test
  void generatedSettingsSurviveSavingAndCopiesDoNotSharePalettes() throws Exception {
    Workspace workspace = new Workspace();
    FluidDoc doc = new FluidDoc();
    doc.id = "custommats:slime";
    doc.generatedTexture = true;
    doc.textureStyle = "slime";
    doc.textureSeed = 73;
    doc.opacity = 120;
    doc.texture = FluidDoc.createTexture(0xFF22CC33);
    workspace.fluids.add(doc);
    workspace.save(folder);
    FluidDoc saved = Workspace.load(folder).fluids.getFirst();
    assertTrue(saved.generatedTexture);
    assertEquals("slime", saved.textureStyle);
    assertEquals(73, saved.textureSeed);
    assertEquals(120, saved.opacity);
    assertEquals(0xFF22CC33, saved.colorInt());
    saved.copy().texture.palette.getFirst().color = 0xFFFFFFFF;
    assertEquals(0xFF000000, saved.texture.palette.getFirst().color);
  }
}
