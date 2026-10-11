package com.tcmateditor.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Editor documents, IDs and languages */
class ModelTest {
  @Test
  void anvilSlotsRoundTrip() {
    // slot 1 is the lapis slot (input 2), then clockwise
    assertEquals(List.of(2, 3, 4, 0, 1), java.util.Arrays.stream(AnvilSlots.INPUT_INDEX).boxed().toList());
    // JEI shows the anvil as left, top, right, bottom right, bottom left: slots 5, 1, 2, 3, 4
    int[] jei = { 1, 2, 3, 4, 0 };
    for (int slot = 0; slot < UpgradeDoc.SLOTS; slot++) {
      assertEquals(jei[slot], AnvilSlots.displaySlot(AnvilSlots.INPUT_INDEX[slot]), "JEI position of slot " + (slot + 1));
    }
    // the tinker station only has inputs 0 to 2: slots 1, 4 and 5
    assertTrue(AnvilSlots.onStation(0));
    assertFalse(AnvilSlots.onStation(1));
    assertFalse(AnvilSlots.onStation(2));
    assertTrue(AnvilSlots.onStation(3));
    assertTrue(AnvilSlots.onStation(4));
  }

  @Test
  void languagesPickAndFallBack() {
    assertEquals("zh_tw", Languages.bestFor("zh_hk"));
    assertEquals("zh_cn", Languages.bestFor("zh_cn"));
    assertEquals("en_us", Languages.bestFor("ko_kr"));
    for (String code : Languages.CODES) {
      assertEquals(code, Languages.fallbackOrder(code).get(0));
      assertEquals(4, Languages.fallbackOrder(code).size());
    }
    Map<String,LangText> texts = new LinkedHashMap<>();
    texts.put("zh_cn", new LangText("红宝石", "", ""));
    texts.put("en_us", new LangText("Ruby", "", ""));
    assertEquals("红宝石", Languages.resolve(texts, "zh_tw", t -> t.name), "traditional falls back to simplified");
    assertEquals("Ruby", Languages.resolve(texts, "ja_jp", t -> t.name), "japanese falls back to english");
    assertEquals("", Languages.resolve(texts, "en_us", t -> t.flavor));
  }

  @Test
  void idValidation() {
    assertTrue(Workspace.isValidId("mymats:ruby"));
    assertTrue(Workspace.isValidId("mymats:gems/ruby"));
    assertFalse(Workspace.isValidId("ruby"));
    assertFalse(Workspace.isValidId("MyMats:ruby"));
    assertFalse(Workspace.isValidId("mymats:../ruby"));
    assertFalse(Workspace.isValidId("mymats:ruby/"));
    assertEquals("red_gem_2", Workspace.sanitizePath("Red Gem 2!"));
  }

  @Test
  void materialSlotsCopyAndValidate() {
    MaterialDoc doc = new MaterialDoc();
    doc.id = "mymats:ruby";
    assertFalse(doc.hasSlots());
    doc.slots.put("upgrades", 1);
    doc.slots.put("abilities", 0);
    doc.validate();
    assertEquals(Map.of("upgrades", 1), doc.slots, "zero slot changes are dropped");
    assertTrue(doc.hasSlots());
    assertEquals("mymats:ruby_slots", doc.slotsModifierId());

    MaterialDoc copy = doc.copy();
    assertNotSame(doc.slots, copy.slots);
    copy.slots.put("defense", 2);
    assertFalse(doc.slots.containsKey("defense"), "copies do not share slots");
  }

  @Test
  void materialSurvivesJson() {
    MaterialDoc doc = new MaterialDoc();
    doc.id = "mymats:ruby";
    doc.tier = 3;
    doc.slots.put("defense", -1);
    doc.defaultTraits.add(new MaterialDoc.TraitRef("mymats:sharp", 2));
    doc.items.add(new MaterialDoc.ItemRef("#c:gems/ruby", 1, 1));
    doc.lang.put("en_us", new LangText("Ruby", "Shiny", "A gem"));

    MaterialDoc read = Workspace.GSON.fromJson(Workspace.GSON.toJson(doc), MaterialDoc.class);
    read.validate();
    assertEquals(doc.id, read.id);
    assertEquals(3, read.tier);
    assertEquals(Map.of("defense", -1), read.slots);
    assertEquals("mymats:sharp", read.defaultTraits.get(0).id);
    assertEquals(2, read.defaultTraits.get(0).level);
    assertEquals("#c:gems/ruby", read.items.get(0).ingredient);
    assertEquals("Ruby", read.lang.get("en_us").name);
  }

  @Test
  void entryLinesValidateAndCopy() {
    EntryDoc doc = new EntryDoc();
    doc.id = "mymats:venom";
    EntryDoc.Line line = new EntryDoc.Line();
    line.kind = "effect";
    line.op = "not_a_trigger";
    line.seconds = 8;
    line.chance = 40;
    doc.lines.add(line);
    doc.validate();
    assertEquals("weapon", line.op, "unknown effect triggers fall back to on hit");

    EntryDoc.Line copy = line.copy();
    assertTrue(copy.isEffect());
    assertEquals(8, copy.seconds);
    assertEquals(40, copy.chance);

    // documents saved before effects existed still get the defaults
    EntryDoc.Line old = Workspace.GSON.fromJson("{\"kind\":\"stat\",\"target\":\"tconstruct:attack_damage\"}", EntryDoc.Line.class);
    assertEquals(5, old.seconds);
    assertEquals(100, old.chance);
  }

  @Test
  void upgradeAlwaysHasFiveSlots() {
    UpgradeDoc doc = Workspace.GSON.fromJson("{\"id\":\"mymats:up\",\"inputs\":[{\"ingredient\":\"minecraft:diamond\"}]}", UpgradeDoc.class);
    assertEquals("minecraft:diamond", doc.slot(0).ingredient);
    assertTrue(doc.slot(4).isEmpty());
    assertEquals(UpgradeDoc.SLOTS, doc.inputs.size());
    assertTrue(doc.hasInputs());
  }
}
