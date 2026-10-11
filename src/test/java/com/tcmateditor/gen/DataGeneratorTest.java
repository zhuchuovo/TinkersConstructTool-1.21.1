package com.tcmateditor.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.model.EntryDoc;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.UpgradeDoc;
import com.tcmateditor.model.Workspace;
import com.tcmateditor.recipe.PositionalModifierRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.impl.ComposableModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generated data must load in Tinkers' Construct. These tests run inside a loaded game (FML JUnit),
 * so the JSON is checked with Tinkers' own modifier loader and the vanilla recipe codecs.
 */
class DataGeneratorTest {
  private static final String NS = "testmats";
  private Workspace workspace;
  private final List<Problem> problems = new ArrayList<>();
  private Map<String,String> files;

  @BeforeEach
  void setUp() {
    workspace = new Workspace();
    problems.clear();

    EntryDoc sharp = new EntryDoc();
    sharp.id = NS + ":sharp";
    sharp.lines.add(line("stat", "tconstruct:attack_damage", "add", 0, 2));
    EntryDoc.Line speed = line("attribute", "minecraft:generic.attack_speed", "add_multiplied_total", 0, -0.1f);
    speed.slots = new ArrayList<>(List.of("mainhand"));
    sharp.lines.add(speed);
    sharp.lines.add(line("slot", "souls", "add", 0, 1));
    EntryDoc.Line poison = line("effect", "minecraft:poison", "weapon", 1, 0);
    poison.seconds = 5;
    poison.chance = 50;
    poison.chancePerLevel = 10;
    sharp.lines.add(poison);
    sharp.lines.add(line("effect", "minecraft:wither", "counter", 1, 0));
    sharp.lines.add(line("effect", "minecraft:speed", "self", 1, 0));
    sharp.lines.add(line("effect", "minecraft:strength", "armor_attack", 0, 1));
    // invalid lines are left out and reported
    sharp.lines.add(line("stat", "tconstruct:not_a_stat", "add", 1, 0));
    sharp.lines.add(line("effect", "minecraft:not_an_effect", "weapon", 1, 0));
    workspace.entries.add(sharp);

    MaterialDoc ruby = new MaterialDoc();
    ruby.id = NS + ":ruby";
    ruby.stats.put("tconstruct:head", JsonParser.parseString("{\"durability\": 300, \"mining_speed\": 6, \"mining_tier\": \"minecraft:iron\", \"melee_attack\": 2}").getAsJsonObject());
    ruby.defaultTraits.add(new MaterialDoc.TraitRef(NS + ":sharp", 2));
    ruby.statTraits.put("tconstruct:armor", new ArrayList<>());
    ruby.slots.put("upgrades", 1);
    ruby.slots.put("abilities", -1);
    ruby.items.add(new MaterialDoc.ItemRef("minecraft:redstone_block", 1, 1));
    ruby.items.add(new MaterialDoc.ItemRef("minecraft:not_an_item", 1, 1));
    ruby.fluid.enabled = true;
    ruby.fluid.fluid = "minecraft:diamond";
    workspace.materials.add(ruby);

    MaterialDoc molten = new MaterialDoc();
    molten.id = NS + ":molten";
    molten.items.add(new MaterialDoc.ItemRef("#c:ingots/copper", 1, 1));
    molten.fluid.enabled = true;
    molten.fluid.fluid = "minecraft:lava";
    molten.fluid.amount = 90;
    workspace.materials.add(molten);

    UpgradeDoc strict = new UpgradeDoc();
    strict.id = NS + ":sharp";
    strict.modifier = NS + ":sharp";
    strict.slot(0).ingredient = "minecraft:diamond";
    strict.slot(2).ingredient = "minecraft:redstone";
    strict.slot(2).count = 2;
    strict.allowTools = new ArrayList<>(List.of("#tconstruct:modifiable/melee"));
    strict.denyTools = new ArrayList<>(List.of("tconstruct:dagger"));
    strict.maxLevel = 3;
    workspace.upgrades.add(strict);

    UpgradeDoc loose = new UpgradeDoc();
    loose.id = NS + ":loose";
    loose.modifier = NS + ":sharp";
    loose.strictSlots = false;
    loose.slot(1).ingredient = "#c:gems";
    workspace.upgrades.add(loose);

    UpgradeDoc broken = new UpgradeDoc();
    broken.id = NS + ":broken";
    broken.modifier = NS + ":sharp";
    broken.slot(0).ingredient = "minecraft:not_an_item";
    workspace.upgrades.add(broken);

    files = DataGenerator.generate(workspace, problems);
  }

  private static EntryDoc.Line line(String kind, String target, String op, float flat, float perLevel) {
    EntryDoc.Line line = new EntryDoc.Line();
    line.kind = kind;
    line.target = target;
    line.op = op;
    line.flat = flat;
    line.perLevel = perLevel;
    return line;
  }

  private JsonObject file(String path) {
    String text = files.get(path);
    assertNotNull(text, "missing generated file " + path + ", have " + files.keySet());
    return JsonParser.parseString(text).getAsJsonObject();
  }

  private boolean hasProblem(String key, String detail) {
    return problems.stream().anyMatch(p -> p.key().equals(key) && p.detail().equals(detail));
  }

  /** Parses a modifier with Tinkers' own loader, the same one the modifier manager uses */
  private static Modifier parseModifier(String id, JsonObject json) {
    return ComposableModifier.LOADER.deserialize(json, ModifierManager.contextBuilder(ResourceLocation.parse(id)).build());
  }

  /** Checks NeoForge conditions, parses with the vanilla recipe codec and encodes for the recipe sync packet */
  private static Recipe<?> parseRecipe(String id, JsonObject json, boolean expectLoaded) {
    json = json.deepCopy();
    JsonElement conditions = json.remove("neoforge:conditions");
    if (conditions != null) {
      List<ICondition> list = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, conditions).getOrThrow(JsonParseException::new);
      assertEquals(expectLoaded, list.stream().allMatch(c -> c.test(ICondition.IContext.EMPTY)), "conditions of " + id);
    }
    RegistryAccess access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    Recipe<?> recipe = Recipe.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), json).getOrThrow(JsonParseException::new);
    RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
    try {
      RecipeHolder.STREAM_CODEC.encode(buffer, new RecipeHolder<>(ResourceLocation.parse(id), recipe));
    } finally {
      buffer.release();
    }
    return recipe;
  }

  /* Materials */

  @Test
  void materialFiles() {
    String base = "data/" + NS + "/tinkering/materials/";
    assertEquals(1, file(base + "definition/ruby.json").get("tier").getAsInt(), "default tier");
    assertEquals(300, file(base + "stats/ruby.json").getAsJsonObject("stats").getAsJsonObject("tconstruct:head").get("durability").getAsInt());
    // the unknown item is skipped, the valid one keeps index 0
    assertTrue(files.containsKey("data/" + NS + "/recipe/tcmateditor/ruby/item_0.json"));
    assertFalse(files.containsKey("data/" + NS + "/recipe/tcmateditor/ruby/item_1.json"));
    assertTrue(hasProblem("unknown_item", "minecraft:not_an_item"));
    // an item ID in the fluid field must not produce broken casting recipes
    assertFalse(files.containsKey("data/" + NS + "/recipe/tcmateditor/ruby/casting.json"));
    assertTrue(hasProblem("unknown_fluid", "minecraft:diamond"));
  }

  @Test
  void materialRecipesLoad() {
    String base = "data/" + NS + "/recipe/tcmateditor/";
    parseRecipe(NS + ":ruby_item", file(base + "ruby/item_0.json"), true);
    parseRecipe(NS + ":molten_item", file(base + "molten/item_0.json"), true);
    parseRecipe(NS + ":molten_casting", file(base + "molten/casting.json"), true);
    parseRecipe(NS + ":molten_melting", file(base + "molten/melting.json"), true);
  }

  @Test
  void materialSlotsBecomeHiddenTrait() {
    JsonObject slots = file("data/" + NS + "/tinkering/modifiers/ruby_slots.json");
    assertEquals("tinker_station", slots.get("tooltip_display").getAsString());
    JsonArray modules = slots.getAsJsonArray("modules");
    assertEquals(2, modules.size());
    for (JsonElement module : modules) {
      assertEquals("tconstruct:modifier_slot", module.getAsJsonObject().get("type").getAsString());
      // flat amounts, so the slots count once no matter how many parts use the material
      assertFalse(module.getAsJsonObject().has("each_level"));
    }
    assertNotNull(parseModifier(NS + ":ruby_slots", slots));

    JsonObject traits = file("data/" + NS + "/tinkering/materials/traits/ruby.json");
    List<String> defaults = names(traits.getAsJsonArray("default"));
    assertEquals(List.of(NS + ":sharp", NS + ":ruby_slots"), defaults);
    // a per stat list replaces the defaults, so it needs the slot trait too
    assertEquals(List.of(NS + ":ruby_slots"), names(traits.getAsJsonObject("perStat").getAsJsonArray("tconstruct:armor")));
  }

  @Test
  void slotTraitIdClashIsReported() {
    EntryDoc clash = new EntryDoc();
    clash.id = NS + ":ruby_slots";
    workspace.entries.add(clash);
    problems.clear();
    files = DataGenerator.generate(workspace, problems);
    assertTrue(hasProblem("slots_id_taken", NS + ":ruby_slots"));
    assertFalse(names(file("data/" + NS + "/tinkering/materials/traits/ruby.json").getAsJsonArray("default")).contains(NS + ":ruby_slots"));
  }

  private static List<String> names(JsonArray traits) {
    List<String> names = new ArrayList<>();
    traits.forEach(trait -> names.add(trait.getAsJsonObject().get("name").getAsString()));
    return names;
  }

  /* Entries */

  @Test
  void entryLoadsInTinkers() {
    JsonObject json = file("data/" + NS + "/tinkering/modifiers/sharp.json");
    List<String> types = new ArrayList<>();
    json.getAsJsonArray("modules").forEach(module -> types.add(module.getAsJsonObject().get("type").getAsString()));
    assertEquals(List.of("tconstruct:stat_boost", "tconstruct:attribute", "tconstruct:modifier_slot", "tconstruct:weapon_mob_effect",
      "tconstruct:counter_mob_effect", "tconstruct:tool_usage_mob_effect", "tconstruct:armor_attack_mob_effect"), types);
    assertTrue(hasProblem("unknown_stat", "tconstruct:not_a_stat"));
    assertTrue(hasProblem("unknown_effect", "minecraft:not_an_effect"));
    // every module must be accepted by Tinkers', one bad module would drop the whole modifier
    Modifier modifier = parseModifier(NS + ":sharp", json);
    assertInstanceOf(ComposableModifier.class, modifier);
  }

  @Test
  void effectValuesUseTinkersUnits() {
    JsonObject poison = file("data/" + NS + "/tinkering/modifiers/sharp.json").getAsJsonArray("modules").get(3).getAsJsonObject();
    assertEquals("minecraft:poison", poison.get("effect").getAsString());
    assertEquals(1f, poison.getAsJsonObject("level").get("flat").getAsFloat());
    assertEquals(100f, poison.getAsJsonObject("time").get("flat").getAsFloat(), "seconds become ticks");
    assertEquals(0.5f, poison.getAsJsonObject("chance").get("flat").getAsFloat(), 1e-6, "percent becomes 0 to 1");
    assertEquals(0.1f, poison.getAsJsonObject("chance").get("each_level").getAsFloat(), 1e-6);
  }

  /* Upgrades */

  @Test
  void strictUpgradeHasFallback() {
    String base = "data/" + NS + "/recipe/tcmateditor/upgrade/";
    JsonObject strict = file(base + "sharp.json");
    assertEquals(UpgradeGenerator.POSITIONAL_TYPE, strict.get("type").getAsString());
    // slot 1 is anvil input 2, slot 3 is input 4
    assertEquals("[2,4]", strict.getAsJsonArray("positions").toString());
    assertEquals("neoforge:difference", strict.getAsJsonObject("tools").get("type").getAsString());
    assertEquals(2, strict.getAsJsonArray("inputs").get(1).getAsJsonObject().get("amount_needed").getAsInt());
    // with this mod loaded the strict recipe is used and the any order fallback is skipped
    assertInstanceOf(PositionalModifierRecipe.class, parseRecipe(NS + ":sharp", strict, true));
    JsonObject anyOrder = file(base + "sharp_any_order.json");
    assertEquals("tconstruct:modifier", anyOrder.get("type").getAsString());
    parseRecipe(NS + ":sharp_any_order", anyOrder, false);
  }

  @Test
  void looseUpgradeIsPlainTinkers() {
    String base = "data/" + NS + "/recipe/tcmateditor/upgrade/";
    JsonObject loose = file(base + "loose.json");
    assertEquals("tconstruct:modifier", loose.get("type").getAsString());
    assertFalse(loose.has("neoforge:conditions"));
    assertFalse(files.containsKey(base + "loose_any_order.json"));
    parseRecipe(NS + ":loose", loose, true);
  }

  @Test
  void upgradeWithUnknownInputIsSkipped() {
    assertFalse(files.keySet().stream().anyMatch(path -> path.contains("/upgrade/broken")));
    assertTrue(problems.stream().anyMatch(p -> p.owner().equals(NS + ":broken") && p.key().equals("unknown_item")));
  }
}
