package com.tcmateditor.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.model.EntryDoc;
import com.tcmateditor.model.Workspace;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.tools.stat.ToolStatId;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Custom entries as data driven Tinkers' modifiers, each line becomes one modifier module */
public final class EntryGenerator {
  private EntryGenerator() {}

  /** Tool stat operations */
  public static final List<String> STAT_OPS = List.of("add", "percent", "multiply_base", "multiply_conditional", "multiply_all");
  /** Attribute operations */
  public static final List<String> ATTRIBUTE_OPS = List.of("add_value", "add_multiplied_base", "add_multiplied_total");
  /** Equipment slots attributes can apply in */
  public static final List<String> SLOTS = List.of("mainhand", "offhand", "head", "chest", "legs", "feet");
  public static final List<String> LEVEL_DISPLAYS = List.of("default", "single_level", "no_levels", "pluses");
  /** Slot types a modifier can add or remove */
  public static final List<String> MODIFIER_SLOT_TYPES = List.of("upgrades", "abilities", "defense", "souls");
  /**
   * When an effect line applies, each is one of Tinkers' mob effect modules:
   * weapon = melee or projectile hit, effect on the target. counter = armor is attacked, effect on the attacker.
   * armor_attack = attacking while wearing the armor, effect on the target. self = using the tool, effect on yourself.
   */
  public static final List<String> EFFECT_TRIGGERS = List.of("weapon", "counter", "armor_attack", "self");
  private static final Map<String,String> EFFECT_MODULES = Map.of(
    "weapon", "tconstruct:weapon_mob_effect",
    "counter", "tconstruct:counter_mob_effect",
    "armor_attack", "tconstruct:armor_attack_mob_effect",
    "self", "tconstruct:tool_usage_mob_effect");

  public static void generate(EntryDoc doc, Map<String,String> files, List<Problem> problems) {
    files.put("data/" + doc.namespace() + "/tinkering/modifiers/" + doc.path() + ".json", Workspace.GSON.toJson(modifierJson(doc, problems)));
  }

  public static JsonObject modifierJson(EntryDoc doc, List<Problem> problems) {
    JsonObject json = new JsonObject();
    String display = LEVEL_DISPLAYS.contains(doc.levelDisplay) ? doc.levelDisplay : "default";
    json.addProperty("level_display", "tconstruct:" + display);
    json.addProperty("show_in_tooltips", "always");
    JsonArray modules = new JsonArray();
    int index = 0;
    for (EntryDoc.Line line : doc.lines) {
      JsonObject module = moduleJson(doc, line, index++, problems);
      if (module != null) {
        modules.add(module);
      }
    }
    json.add("modules", modules);
    return json;
  }

  private static JsonObject moduleJson(EntryDoc doc, EntryDoc.Line line, int index, List<Problem> problems) {
    if (line.isEffect()) {
      return effectModule(doc, line, problems);
    }
    if (line.isSlot()) {
      int flat = Math.round(line.flat);
      int eachLevel = Math.round(line.perLevel);
      if (!MODIFIER_SLOT_TYPES.contains(line.target) || (flat == 0 && eachLevel == 0)) {
        return null;
      }
      return slotModule(line.target, flat, eachLevel);
    }
    if (line.target == null || !Workspace.isValidId(GenUtil.fullId(line.target))) {
      return null;
    }
    // an unknown stat or attribute makes the whole modifier fail to load
    ResourceLocation target = new ResourceLocation(GenUtil.fullId(line.target));
    boolean known = line.isAttribute() ? BuiltInRegistries.ATTRIBUTE.containsKey(target) : ToolStats.getToolStat(new ToolStatId(target)) != null;
    if (!known) {
      problems.add(new Problem(doc.id, line.isAttribute() ? "unknown_attribute" : "unknown_stat", target.toString()));
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
      module.addProperty("attribute", target.toString());
      String operation = attributeOperation(line.op);
      module.addProperty("operation", operation);
      module.addProperty("flat", line.flat);
      module.addProperty("each_level", line.perLevel);
      JsonArray slotArray = new JsonArray();
      slots.forEach(slotArray::add);
      module.add("slots", slotArray);
      if (!"addition".equals(operation)) {
        module.addProperty("tooltip_style", "percent");
      }
    } else {
      module.addProperty("type", "tconstruct:stat_boost");
      module.addProperty("stat", target.toString());
      module.addProperty("operation", STAT_OPS.contains(line.op) ? line.op : "add");
      module.addProperty("flat", line.flat);
      module.addProperty("each_level", line.perLevel);
    }
    return module;
  }

  /** The editor uses the same labels across versions; 1.20.1 attributes use these enum names. */
  private static String attributeOperation(String operation) {
    return switch (operation) {
      case "add_multiplied_base", "multiply_base" -> "multiply_base";
      case "add_multiplied_total", "multiply_total" -> "multiply_total";
      default -> "addition";
    };
  }

  /** Tinkers' modifier slot module: flat + eachLevel * level slots of the given type */
  static JsonObject slotModule(String type, int flat, int eachLevel) {
    JsonObject module = new JsonObject();
    module.addProperty("type", "tconstruct:modifier_slot");
    module.addProperty("name", type);
    if (flat != 0) {
      module.addProperty("flat", flat);
    }
    if (eachLevel != 0) {
      module.addProperty("each_level", eachLevel);
    }
    return module;
  }

  /** Tinkers' mob effect module: level and time scale with the modifier level, chance is 0 to 1 */
  private static JsonObject effectModule(EntryDoc doc, EntryDoc.Line line, List<Problem> problems) {
    ResourceLocation effect = line.target == null ? null : ResourceLocation.tryParse(GenUtil.fullId(line.target.trim()));
    if (effect == null || !BuiltInRegistries.MOB_EFFECT.containsKey(effect)) {
      problems.add(new Problem(doc.id, "unknown_effect", String.valueOf(line.target)));
      return null;
    }
    JsonObject module = new JsonObject();
    module.addProperty("type", EFFECT_MODULES.getOrDefault(line.op, EFFECT_MODULES.get("weapon")));
    module.addProperty("effect", effect.toString());
    // effect level 1 is level I
    module.add("level", leveling("per_level", line.flat, line.perLevel));
    module.add("time", leveling("per_level", line.seconds * 20, line.secondsPerLevel * 20));
    module.add("chance", leveling("each_level", line.chance / 100f, line.chancePerLevel / 100f));
    return module;
  }

  /** {"flat": a, perLevelKey: b}, leaving out zeros */
  private static JsonObject leveling(String perLevelKey, float flat, float perLevel) {
    JsonObject json = new JsonObject();
    if (flat != 0 || perLevel == 0) {
      json.addProperty("flat", flat);
    }
    if (perLevel != 0) {
      json.addProperty(perLevelKey, perLevel);
    }
    return json;
  }
}
