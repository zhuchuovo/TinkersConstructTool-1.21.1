package com.tcmateditor.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tcmateditor.TCMatEditor;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Describes the editable fields of every material stat type */
public final class StatSchema {
  private StatSchema() {}

  public enum Kind { INT, FLOAT, PERCENT, TIER, TEXT }

  /**
   * One stat field
   * @param def  Default used when the stat type is enabled
   */
  public record Field(String key, Kind kind, double def, double min, double max, double step) {}

  public record StatInfo(String id, String group, List<Field> fields) {}

  public static final List<String> GROUPS = List.of("tools", "ranged", "armor", "slimesuit", "ammo", "other");
  public static final List<String> TIERS = List.of("minecraft:wood", "minecraft:stone", "minecraft:gold", "minecraft:iron", "minecraft:diamond", "minecraft:netherite");

  private static final Map<String,StatInfo> KNOWN = new LinkedHashMap<>();

  private static Field i(String key, double def, double min, double max, double step) {
    return new Field(key, Kind.INT, def, min, max, step);
  }

  private static Field f(String key, double def, double min, double max, double step) {
    return new Field(key, Kind.FLOAT, def, min, max, step);
  }

  /** Percentage field, stored as a fraction in JSON */
  private static Field p(String key) {
    return new Field(key, Kind.PERCENT, 0, -100, 1000, 5);
  }

  private static void known(String path, String group, Field... fields) {
    String id = "tconstruct:" + path;
    KNOWN.put(id, new StatInfo(id, group, List.of(fields)));
  }

  static {
    known("head", "tools", i("durability", 250, 1, 1000000, 10), f("mining_speed", 6, 0, 1000, 0.5), new Field("mining_tier", Kind.TIER, 0, 0, 0, 0), f("melee_attack", 2, 0, 1000, 0.5));
    known("handle", "tools", p("durability"), p("mining_speed"), p("melee_speed"), p("melee_damage"));
    known("binding", "tools");
    known("limb", "ranged", i("durability", 250, 1, 1000000, 10), f("draw_speed", 0, -10, 10, 0.05), f("velocity", 0, -10, 10, 0.05), f("accuracy", 0, -10, 10, 0.05));
    known("grip", "ranged", p("durability"), f("accuracy", 0, -10, 10, 0.05), f("melee_damage", 0, 0, 1000, 0.5));
    known("bowstring", "ranged");
    known("plating_helmet", "armor", i("durability", 165, 1, 1000000, 5), f("armor", 2, 0, 100, 0.5), f("toughness", 0, 0, 100, 0.5), f("knockback_resistance", 0, 0, 10, 0.05));
    known("plating_chestplate", "armor", i("durability", 240, 1, 1000000, 5), f("armor", 5, 0, 100, 0.5), f("toughness", 0, 0, 100, 0.5), f("knockback_resistance", 0, 0, 10, 0.05));
    known("plating_leggings", "armor", i("durability", 225, 1, 1000000, 5), f("armor", 4, 0, 100, 0.5), f("toughness", 0, 0, 100, 0.5), f("knockback_resistance", 0, 0, 10, 0.05));
    known("plating_boots", "armor", i("durability", 195, 1, 1000000, 5), f("armor", 2, 0, 100, 0.5), f("toughness", 0, 0, 100, 0.5), f("knockback_resistance", 0, 0, 10, 0.05));
    known("plating_shield", "armor", i("durability", 270, 1, 1000000, 5), f("toughness", 0, 0, 100, 0.5), f("knockback_resistance", 0, 0, 10, 0.05));
    known("maille", "armor");
    known("shield_core", "armor");
    known("cuirass", "armor");
    known("skull", "slimesuit", i("durability", 165, 1, 1000000, 5), i("armor", 0, 0, 100, 1));
    known("slime", "slimesuit", i("durability", 150, 1, 1000000, 5), i("overslime_capacity", 50, 0, 1000000, 5));
    known("ribcage", "slimesuit", i("repair_amount", 100, 1, 1000000, 5));
    known("shell", "slimesuit", i("repair_amount", 100, 1, 1000000, 5));
    known("laces", "slimesuit", i("repair_amount", 100, 1, 1000000, 5));
    known("arrow_head", "ammo");
    known("arrow_shaft", "ammo");
    known("fletching", "ammo");
    known("repair_kit", "other");
  }

  /** All stat types: known ones first, then any other registered type (addons) */
  public static List<StatInfo> all() {
    List<StatInfo> list = new ArrayList<>(KNOWN.values());
    try {
      for (ResourceLocation id : MaterialRegistry.getInstance().getAllStatTypeIds()) {
        if (!KNOWN.containsKey(id.toString())) {
          list.add(discover(id.toString()));
        }
      }
    } catch (RuntimeException e) {
      TCMatEditor.LOG.debug("Unable to list stat types", e);
    }
    return list;
  }

  public static StatInfo get(String id) {
    StatInfo info = KNOWN.get(id);
    return info != null ? info : discover(id);
  }

  /** Builds fields from the serialized default stats of an unknown type */
  private static StatInfo discover(String id) {
    List<Field> fields = new ArrayList<>();
    JsonObject defaults = defaultJson(id);
    for (Map.Entry<String,JsonElement> entry : defaults.entrySet()) {
      JsonElement value = entry.getValue();
      if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
        double number = value.getAsDouble();
        boolean integer = number == Math.rint(number) && !value.getAsString().contains(".");
        fields.add(new Field(entry.getKey(), integer ? Kind.INT : Kind.FLOAT, number, -1000000, 1000000, integer ? 1 : 0.1));
      } else if (value.isJsonPrimitive()) {
        fields.add(new Field(entry.getKey(), Kind.TEXT, 0, 0, 0, 0));
      }
    }
    return new StatInfo(id, "other", fields);
  }

  @SuppressWarnings("unchecked")
  private static <T extends IMaterialStats> JsonObject defaultJson(String id) {
    JsonObject json = new JsonObject();
    try {
      MaterialStatsId statsId = MaterialStatsId.PARSER.tryParse(id);
      if (statsId != null) {
        MaterialStatType<T> type = MaterialRegistry.getInstance().getStatType(statsId);
        if (type != null) {
          type.getLoadable().serialize(type.getDefaultStats(), json);
        }
      }
    } catch (RuntimeException e) {
      TCMatEditor.LOG.debug("Unable to read defaults of {}", id, e);
    }
    return json;
  }

  /** JSON used when a stat type gets enabled */
  public static JsonObject defaults(StatInfo info) {
    JsonObject json = new JsonObject();
    JsonObject registryDefaults = KNOWN.containsKey(info.id) ? new JsonObject() : defaultJson(info.id);
    for (Field field : info.fields) {
      switch (field.kind) {
        case INT -> json.addProperty(field.key, (int) Math.round(field.def));
        case FLOAT, PERCENT -> json.addProperty(field.key, (float) field.def);
        case TIER -> json.addProperty(field.key, "minecraft:iron");
        case TEXT -> {
          if (registryDefaults.has(field.key)) {
            json.add(field.key, registryDefaults.get(field.key));
          }
        }
      }
    }
    return json;
  }
}
