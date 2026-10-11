package com.tcmateditor.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tcmateditor.model.AnvilSlots;
import com.tcmateditor.model.EntryDoc;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.MaterialDoc.ItemRef;
import com.tcmateditor.model.MaterialDoc.TraitRef;
import com.tcmateditor.model.UpgradeDoc;
import com.tcmateditor.model.Workspace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Converts editor documents into plain Tinkers' Construct data pack files.
 * Nothing generated here references this mod, so the output works on its own.
 */
public final class DataGenerator {
  private DataGenerator() {}

  /** Generates all data files, keyed by pack relative path */
  public static Map<String,String> generate(Workspace workspace) {
    Map<String,String> files = new TreeMap<>();
    for (MaterialDoc doc : workspace.materials) {
      if (Workspace.isValidId(doc.id)) {
        generateMaterial(doc, files);
      }
    }
    for (EntryDoc doc : workspace.entries) {
      if (Workspace.isValidId(doc.id)) {
        files.put("data/" + doc.namespace() + "/tinkering/modifiers/" + doc.path() + ".json", Workspace.GSON.toJson(modifierJson(doc)));
      }
    }
    for (UpgradeDoc doc : workspace.upgrades) {
      if (Workspace.isValidId(doc.id) && Workspace.isValidId(fullId(doc.modifier)) && doc.hasInputs()) {
        generateUpgrade(doc, files);
      }
    }
    return files;
  }

  /* Materials */

  private static void generateMaterial(MaterialDoc doc, Map<String,String> files) {
    String ns = doc.namespace();
    String path = doc.path();
    String base = "data/" + ns + "/tinkering/materials/";

    JsonObject definition = new JsonObject();
    definition.addProperty("craftable", doc.craftable);
    definition.addProperty("hidden", doc.hidden);
    definition.addProperty("sortOrder", doc.sortOrder);
    definition.addProperty("tier", doc.tier);
    files.put(base + "definition/" + path + ".json", Workspace.GSON.toJson(definition));

    JsonObject statsRoot = new JsonObject();
    JsonObject stats = new JsonObject();
    for (Map.Entry<String,JsonObject> entry : doc.stats.entrySet()) {
      stats.add(entry.getKey(), entry.getValue().deepCopy());
    }
    statsRoot.add("stats", stats);
    files.put(base + "stats/" + path + ".json", Workspace.GSON.toJson(statsRoot));

    JsonObject traits = traitsJson(doc);
    if (traits != null) {
      files.put(base + "traits/" + path + ".json", Workspace.GSON.toJson(traits));
    }

    // part builder recipes
    String recipeBase = "data/" + ns + "/recipe/tcmateditor/" + path + "/";
    int index = 0;
    for (ItemRef item : doc.items) {
      JsonObject ingredient = ingredientJson(item.ingredient);
      if (ingredient == null) {
        continue;
      }
      JsonObject recipe = new JsonObject();
      recipe.addProperty("type", "tconstruct:material");
      recipe.add("ingredient", ingredient);
      recipe.addProperty("material", doc.id);
      recipe.addProperty("needed", Math.max(1, item.needed));
      recipe.addProperty("value", Math.max(1, item.value));
      files.put(recipeBase + "item_" + index++ + ".json", Workspace.GSON.toJson(recipe));
    }

    // casting and melting
    MaterialDoc.FluidRef fluid = doc.fluid;
    if (fluid.enabled && isValidRef(fluid.fluid)) {
      int amount = Math.max(1, fluid.amount);
      int temperature = Math.max(1, fluid.temperature);
      JsonObject casting = new JsonObject();
      casting.addProperty("type", "tconstruct:material_fluid");
      casting.add("fluid", fluidJson(fluid.fluid, amount));
      casting.addProperty("output", doc.id);
      casting.addProperty("temperature", temperature);
      files.put(recipeBase + "casting.json", Workspace.GSON.toJson(casting));

      JsonObject melting = new JsonObject();
      melting.addProperty("type", "tconstruct:material_melting");
      melting.addProperty("input", doc.id);
      melting.add("result", fluidJson(fluid.fluid, amount));
      melting.addProperty("temperature", temperature);
      files.put(recipeBase + "melting.json", Workspace.GSON.toJson(melting));
    }
  }

  private static JsonObject traitsJson(MaterialDoc doc) {
    JsonArray defaults = traitList(doc.defaultTraits);
    JsonObject perStat = new JsonObject();
    for (Map.Entry<String,List<TraitRef>> entry : doc.statTraits.entrySet()) {
      // an empty list is meaningful: it removes the default traits from that stat type
      perStat.add(entry.getKey(), traitList(entry.getValue()));
    }
    if (defaults.isEmpty() && perStat.isEmpty()) {
      return null;
    }
    JsonObject json = new JsonObject();
    if (!defaults.isEmpty()) {
      json.add("default", defaults);
    }
    if (!perStat.isEmpty()) {
      json.add("perStat", perStat);
    }
    return json;
  }

  private static JsonArray traitList(List<TraitRef> traits) {
    JsonArray array = new JsonArray();
    for (TraitRef trait : traits) {
      if (trait.id != null && Workspace.isValidId(trait.id)) {
        JsonObject json = new JsonObject();
        json.addProperty("name", trait.id);
        json.addProperty("level", Math.max(1, trait.level));
        array.add(json);
      }
    }
    return array;
  }

  /** Checks if a string is an ID or #tag */
  public static boolean isValidRef(String ref) {
    if (ref == null || ref.isBlank()) {
      return false;
    }
    String id = ref.startsWith("#") ? ref.substring(1) : ref;
    if (!id.contains(":")) {
      id = "minecraft:" + id;
    }
    return Workspace.isValidId(id);
  }

  private static String fullId(String id) {
    return id.contains(":") ? id : "minecraft:" + id;
  }

  /** Converts an item reference to an ingredient JSON */
  public static JsonObject ingredientJson(String ref) {
    if (!isValidRef(ref)) {
      return null;
    }
    ref = ref.trim().toLowerCase(Locale.ROOT);
    JsonObject json = new JsonObject();
    if (ref.startsWith("#")) {
      json.addProperty("tag", fullId(ref.substring(1)));
    } else {
      json.addProperty("item", fullId(ref));
    }
    return json;
  }

  private static JsonObject fluidJson(String ref, int amount) {
    ref = ref.trim().toLowerCase(Locale.ROOT);
    JsonObject json = new JsonObject();
    json.addProperty("amount", amount);
    if (ref.startsWith("#")) {
      json.addProperty("tag", fullId(ref.substring(1)));
    } else {
      json.addProperty("fluid", fullId(ref));
    }
    return json;
  }

  /* Entries (modifiers) */

  public static final List<String> STAT_OPS = List.of("add", "percent", "multiply_base", "multiply_conditional", "multiply_all");
  public static final List<String> ATTRIBUTE_OPS = List.of("add_value", "add_multiplied_base", "add_multiplied_total");
  public static final List<String> SLOTS = List.of("mainhand", "offhand", "head", "chest", "legs", "feet");
  public static final List<String> LEVEL_DISPLAYS = List.of("default", "single_level", "no_levels", "pluses");

  public static JsonObject modifierJson(EntryDoc doc) {
    JsonObject json = new JsonObject();
    String display = LEVEL_DISPLAYS.contains(doc.levelDisplay) ? doc.levelDisplay : "default";
    json.addProperty("level_display", "tconstruct:" + display);
    json.addProperty("tooltip_display", "always");
    JsonArray modules = new JsonArray();
    int index = 0;
    for (EntryDoc.Line line : doc.lines) {
      JsonObject module = moduleJson(doc, line, index++);
      if (module != null) {
        modules.add(module);
      }
    }
    json.add("modules", modules);
    return json;
  }

  private static JsonObject moduleJson(EntryDoc doc, EntryDoc.Line line, int index) {
    if (line.target == null || !Workspace.isValidId(fullId(line.target))) {
      return null;
    }
    JsonObject module = new JsonObject();
    if (line.isAttribute()) {
      List<String> slots = new ArrayList<>();
      for (String slot : line.slots) {
        if (SLOTS.contains(slot) && !slots.contains(slot)) {
          slots.add(slot);
        }
      }
      if (slots.isEmpty()) {
        slots.add("mainhand");
      }
      module.addProperty("type", "tconstruct:attribute");
      module.addProperty("unique", doc.namespace() + ".modifier." + doc.path().replace('/', '.') + ".line" + index);
      module.addProperty("attribute", fullId(line.target));
      module.addProperty("operation", ATTRIBUTE_OPS.contains(line.op) ? line.op : "add_value");
      module.addProperty("flat", line.flat);
      module.addProperty("each_level", line.perLevel);
      JsonArray slotArray = new JsonArray();
      slots.forEach(slotArray::add);
      module.add("slots", slotArray);
      if (!"add_value".equals(line.op)) {
        module.addProperty("tooltip_style", "percent");
      }
    } else {
      module.addProperty("type", "tconstruct:stat_boost");
      module.addProperty("stat", fullId(line.target));
      module.addProperty("operation", STAT_OPS.contains(line.op) ? line.op : "add");
      module.addProperty("flat", line.flat);
      module.addProperty("each_level", line.perLevel);
    }
    return module;
  }

  /* Upgrades (modifier recipes) */

  public static final List<String> SLOT_TYPES = List.of("upgrades", "abilities", "defense", "souls", "none");
  /** Recipe type that checks exact anvil slots, only present with this mod */
  public static final String POSITIONAL_TYPE = "tcmateditor:positional_modifier";

  private static void generateUpgrade(UpgradeDoc doc, Map<String,String> files) {
    JsonArray inputs = new JsonArray();
    JsonArray positions = new JsonArray();
    for (int slot = 0; slot < UpgradeDoc.SLOTS; slot++) {
      UpgradeDoc.SlotInput input = doc.slot(slot);
      JsonObject ingredient = input.isEmpty() ? null : ingredientJson(input.ingredient);
      if (ingredient == null) {
        continue;
      }
      if (input.count > 1) {
        ingredient.addProperty("amount_needed", input.count);
      }
      inputs.add(ingredient);
      positions.add(AnvilSlots.INPUT_INDEX[slot]);
    }
    if (inputs.isEmpty()) {
      return;
    }
    String base = "data/" + doc.namespace() + "/recipe/tcmateditor/upgrade/" + doc.path();
    if (doc.strictSlots) {
      // exact slots with this mod, any order without it
      JsonObject strict = upgradeJson(doc, POSITIONAL_TYPE, inputs, modLoaded(true));
      strict.add("positions", positions);
      files.put(base + ".json", Workspace.GSON.toJson(strict));
      files.put(base + "_any_order.json", Workspace.GSON.toJson(upgradeJson(doc, "tconstruct:modifier", inputs, modLoaded(false))));
    } else {
      files.put(base + ".json", Workspace.GSON.toJson(upgradeJson(doc, "tconstruct:modifier", inputs, null)));
    }
  }

  private static JsonObject upgradeJson(UpgradeDoc doc, String type, JsonArray inputs, JsonArray conditions) {
    JsonObject json = new JsonObject();
    json.addProperty("type", type);
    if (conditions != null) {
      json.add("neoforge:conditions", conditions);
    }
    json.add("inputs", inputs.deepCopy());
    json.add("tools", toolsJson(doc));
    json.addProperty("result", fullId(doc.modifier));
    if (doc.maxLevel > 1) {
      JsonObject level = new JsonObject();
      level.addProperty("max", doc.maxLevel);
      json.add("level", level);
    } else {
      json.addProperty("level", 1);
    }
    if (!"none".equals(doc.slotType) && doc.slotCount > 0 && SLOT_TYPES.contains(doc.slotType)) {
      JsonObject slots = new JsonObject();
      slots.addProperty(doc.slotType, doc.slotCount);
      json.add("slots", slots);
    }
    json.addProperty("allow_crystal", doc.allowCrystal);
    return json;
  }

  /** NeoForge condition: this mod is (or is not) loaded */
  private static JsonArray modLoaded(boolean loaded) {
    JsonObject condition = new JsonObject();
    condition.addProperty("type", "neoforge:mod_loaded");
    condition.addProperty("modid", "tcmateditor");
    if (!loaded) {
      JsonObject not = new JsonObject();
      not.addProperty("type", "neoforge:not");
      not.add("value", condition);
      condition = not;
    }
    JsonArray array = new JsonArray();
    array.add(condition);
    return array;
  }

  /** Allowed tools minus denied tools, using NeoForge's difference ingredient when needed */
  public static JsonElement toolsJson(UpgradeDoc doc) {
    JsonElement allowed = ingredientList(doc.allowTools);
    if (allowed == null) {
      JsonObject all = new JsonObject();
      all.addProperty("tag", "tconstruct:modifiable");
      allowed = all;
    }
    JsonElement denied = ingredientList(doc.denyTools);
    if (denied == null) {
      return allowed;
    }
    JsonObject difference = new JsonObject();
    difference.addProperty("type", "neoforge:difference");
    difference.add("base", allowed);
    difference.add("subtracted", denied);
    return difference;
  }

  private static JsonElement ingredientList(List<String> refs) {
    JsonArray array = new JsonArray();
    for (String ref : refs) {
      JsonObject json = ingredientJson(ref);
      if (json != null) {
        array.add(json);
      }
    }
    if (array.isEmpty()) {
      return null;
    }
    return array.size() == 1 ? array.get(0) : array;
  }

  /* Helpers for the UI */

  /** Reads a numeric field from stats JSON */
  public static double getNumber(JsonObject json, String key, double fallback) {
    JsonElement element = json.get(key);
    if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
      return element.getAsDouble();
    }
    return fallback;
  }

  /** Reads a string field from stats JSON */
  public static String getString(JsonObject json, String key, String fallback) {
    JsonElement element = json.get(key);
    if (element != null && element.isJsonPrimitive()) {
      return element.getAsString();
    }
    return fallback;
  }

  /** Creates an ordered copy of a map for stable output */
  public static <V> Map<String,V> ordered(Map<String,V> map) {
    return new LinkedHashMap<>(new TreeMap<>(map));
  }
}
