package com.tcmateditor.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.MaterialDoc.ItemRef;
import com.tcmateditor.model.MaterialDoc.TraitRef;
import com.tcmateditor.model.Workspace;

import java.util.List;
import java.util.Map;

/** Material definition, stats, traits, the slot trait and part builder / casting / melting recipes */
public final class MaterialGenerator {
  private MaterialGenerator() {}

  public static void generate(MaterialDoc doc, Workspace workspace, Map<String,String> files, List<Problem> problems) {
    generate(doc, workspace, files, problems, false);
  }

  public static void generate(MaterialDoc doc, Workspace workspace, Map<String,String> files, List<Problem> problems, boolean export) {
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

    // slot changes live in a hidden trait owned by the material
    String slotsTrait = null;
    if (doc.hasSlots()) {
      if (workspace.findEntry(doc.slotsModifierId()) != null) {
        problems.add(new Problem(doc.id, "slots_id_taken", doc.slotsModifierId()));
      } else {
        slotsTrait = doc.slotsModifierId();
        files.put("data/" + ns + "/tinkering/modifiers/" + path + "_slots.json", Workspace.GSON.toJson(slotsJson(doc)));
      }
    }

    JsonObject traits = traitsJson(doc, workspace, problems, slotsTrait);
    if (traits != null) {
      files.put(base + "traits/" + path + ".json", Workspace.GSON.toJson(traits));
    }

    // part builder recipes
    String recipeBase = "data/" + ns + "/recipe/tcmateditor/" + path + "/";
    int index = 0;
    for (ItemRef item : doc.items) {
      if (item.ingredient == null || item.ingredient.isBlank()) {
        continue;
      }
      JsonObject ingredient = GenUtil.isKnownItem(item.ingredient) ? GenUtil.ingredientJson(item.ingredient) : null;
      if (ingredient == null) {
        problems.add(new Problem(doc.id, "unknown_item", item.ingredient));
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
    boolean knownFluid = export ? GenUtil.isKnownFluid(fluid.fluid, workspace) : GenUtil.isKnownFluid(fluid.fluid);
    if (fluid.enabled && fluid.fluid != null && !fluid.fluid.isBlank() && !knownFluid) {
      problems.add(new Problem(doc.id, "unknown_fluid", fluid.fluid));
    } else if (fluid.enabled && GenUtil.isValidRef(fluid.fluid)) {
      int amount = Math.max(1, fluid.amount);
      int temperature = Math.max(1, fluid.temperature);
      JsonObject casting = new JsonObject();
      casting.addProperty("type", "tconstruct:material_fluid");
      casting.add("fluid", GenUtil.fluidJson(fluid.fluid, amount));
      casting.addProperty("output", doc.id);
      casting.addProperty("temperature", temperature);
      files.put(recipeBase + "casting.json", Workspace.GSON.toJson(casting));

      JsonObject melting = new JsonObject();
      melting.addProperty("type", "tconstruct:material_melting");
      melting.addProperty("input", doc.id);
      melting.add("result", GenUtil.fluidJson(fluid.fluid, amount));
      melting.addProperty("temperature", temperature);
      files.put(recipeBase + "melting.json", Workspace.GSON.toJson(melting));
    }
  }

  private static JsonObject traitsJson(MaterialDoc doc, Workspace workspace, List<Problem> problems, String slotsTrait) {
    JsonArray defaults = traitList(doc, doc.defaultTraits, workspace, problems);
    JsonObject perStat = new JsonObject();
    for (Map.Entry<String,List<TraitRef>> entry : doc.statTraits.entrySet()) {
      // an empty list is meaningful: it removes the default traits from that stat type
      perStat.add(entry.getKey(), traitList(doc, entry.getValue(), workspace, problems));
    }
    if (slotsTrait != null) {
      // per stat lists replace the default list, so the slot trait goes into every list
      JsonObject trait = new JsonObject();
      trait.addProperty("name", slotsTrait);
      trait.addProperty("level", 1);
      defaults.add(trait);
      for (Map.Entry<String,JsonElement> entry : perStat.entrySet()) {
        entry.getValue().getAsJsonArray().add(trait.deepCopy());
      }
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

  private static JsonArray traitList(MaterialDoc doc, List<TraitRef> traits, Workspace workspace, List<Problem> problems) {
    JsonArray array = new JsonArray();
    for (TraitRef trait : traits) {
      if (trait.id != null && Workspace.isValidId(trait.id) && !GenUtil.isKnownModifier(trait.id, workspace)) {
        problems.add(new Problem(doc.id, "unknown_modifier", trait.id));
      } else if (trait.id != null && Workspace.isValidId(trait.id)) {
        JsonObject json = new JsonObject();
        json.addProperty("name", trait.id);
        json.addProperty("level", Math.max(1, trait.level));
        array.add(json);
      }
    }
    return array;
  }

  /** Modifier that adds the material's slots once per tool, no matter how many parts use the material */
  private static JsonObject slotsJson(MaterialDoc doc) {
    JsonObject json = new JsonObject();
    json.addProperty("level_display", "tconstruct:no_levels");
    // like Tinkers' own slot modifiers, only listed in the tinker station
    json.addProperty("tooltip_display", "tinker_station");
    JsonArray modules = new JsonArray();
    for (String type : EntryGenerator.MODIFIER_SLOT_TYPES) {
      Integer amount = doc.slots.get(type);
      if (amount != null && amount != 0) {
        modules.add(EntryGenerator.slotModule(type, amount, 0));
      }
    }
    json.add("modules", modules);
    return json;
  }
}
