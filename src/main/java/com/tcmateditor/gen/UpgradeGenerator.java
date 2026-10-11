package com.tcmateditor.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.model.AnvilSlots;
import com.tcmateditor.model.UpgradeDoc;
import com.tcmateditor.model.Workspace;

import java.util.List;
import java.util.Map;

/** Upgrades as Tinker Station / Tinkers' Anvil modifier recipes */
public final class UpgradeGenerator {
  private UpgradeGenerator() {}

  /** Modifier slot types an upgrade can cost */
  public static final List<String> SLOT_TYPES = List.of("upgrades", "abilities", "defense", "souls", "none");
  /** Recipe type that checks exact anvil slots, only present with this mod */
  public static final String POSITIONAL_TYPE = "tcmateditor:positional_modifier";

  public static void generate(UpgradeDoc doc, Workspace workspace, Map<String,String> files, List<Problem> problems) {
    String modifier = GenUtil.fullId(doc.modifier);
    if (!Workspace.isValidId(doc.id) || !Workspace.isValidId(modifier) || !doc.hasInputs()) {
      return;
    }
    if (!GenUtil.isKnownModifier(modifier, workspace)) {
      problems.add(new Problem(doc.id, "unknown_modifier", modifier));
      return;
    }
    JsonArray inputs = new JsonArray();
    JsonArray positions = new JsonArray();
    for (int slot = 0; slot < UpgradeDoc.SLOTS; slot++) {
      UpgradeDoc.SlotInput input = doc.slot(slot);
      if (input.isEmpty()) {
        continue;
      }
      JsonObject ingredient = GenUtil.isKnownItem(input.ingredient) ? GenUtil.ingredientJson(input.ingredient) : null;
      if (ingredient == null) {
        // leaving out one input would change the recipe, so skip all of it
        problems.add(new Problem(doc.id, "unknown_item", input.ingredient));
        return;
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
    String base = "data/" + doc.namespace() + "/recipes/tcmateditor/upgrade/" + doc.path();
    JsonElement tools = toolsJson(doc, problems);
    if (doc.strictSlots) {
      // exact slots with this mod, any order without it
      JsonObject strict = upgradeJson(doc, POSITIONAL_TYPE, inputs, tools, modLoaded(true));
      strict.add("positions", positions);
      files.put(base + ".json", Workspace.GSON.toJson(strict));
      files.put(base + "_any_order.json", Workspace.GSON.toJson(upgradeJson(doc, "tconstruct:modifier", inputs, tools, modLoaded(false))));
    } else {
      files.put(base + ".json", Workspace.GSON.toJson(upgradeJson(doc, "tconstruct:modifier", inputs, tools, null)));
    }
  }

  private static JsonObject upgradeJson(UpgradeDoc doc, String type, JsonArray inputs, JsonElement tools, JsonArray conditions) {
    JsonObject json = new JsonObject();
    json.addProperty("type", type);
    if (conditions != null) {
      json.add("conditions", conditions);
    }
    json.add("inputs", inputs.deepCopy());
    json.add("tools", tools.deepCopy());
    json.addProperty("result", GenUtil.fullId(doc.modifier));
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

  /** NeoForge 1.20.1 retains Forge condition IDs. */
  private static JsonArray modLoaded(boolean loaded) {
    JsonObject condition = new JsonObject();
    condition.addProperty("type", "forge:mod_loaded");
    condition.addProperty("modid", "tcmateditor");
    if (!loaded) {
      JsonObject not = new JsonObject();
      not.addProperty("type", "forge:not");
      not.add("value", condition);
      condition = not;
    }
    JsonArray array = new JsonArray();
    array.add(condition);
    return array;
  }

  /** Allowed tools minus denied tools, using the 1.20.1 Forge difference ingredient. */
  public static JsonElement toolsJson(UpgradeDoc doc, List<Problem> problems) {
    JsonElement allowed = ingredientList(doc, doc.allowTools, problems);
    if (allowed == null) {
      JsonObject all = new JsonObject();
      all.addProperty("tag", "tconstruct:modifiable");
      allowed = all;
    }
    JsonElement denied = ingredientList(doc, doc.denyTools, problems);
    if (denied == null) {
      return allowed;
    }
    JsonObject difference = new JsonObject();
    difference.addProperty("type", "forge:difference");
    difference.add("base", allowed);
    difference.add("subtracted", denied);
    return difference;
  }

  private static JsonElement ingredientList(UpgradeDoc doc, List<String> refs, List<Problem> problems) {
    JsonArray array = new JsonArray();
    for (String ref : refs) {
      if (ref == null || ref.isBlank()) {
        continue;
      }
      if (!GenUtil.isKnownItem(ref)) {
        problems.add(new Problem(doc.id, "unknown_item", ref));
        continue;
      }
      JsonObject json = GenUtil.ingredientJson(ref);
      if (json != null) {
        array.add(json);
      }
    }
    if (array.isEmpty()) {
      return null;
    }
    return array.size() == 1 ? array.get(0) : array;
  }
}
