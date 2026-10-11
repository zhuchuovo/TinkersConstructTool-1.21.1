package com.tcmateditor.gen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tcmateditor.model.Workspace;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;

import java.util.Locale;

/** Item, fluid and modifier references shared by the generators and the editor UI */
public final class GenUtil {
  private GenUtil() {}

  /* References */

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

  /** Adds the minecraft namespace to IDs without one */
  public static String fullId(String id) {
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

  /** Converts a fluid reference to a sized fluid JSON */
  public static JsonObject fluidJson(String ref, int amount) {
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

  /* Checks against the running game, unknown IDs make the whole file fail to load */

  /** Checks if the reference is a registered item or any well formed tag */
  public static boolean isKnownItem(String ref) {
    return isKnown(ref, BuiltInRegistries.ITEM, BuiltInRegistries.ITEM.getDefaultKey());
  }

  /** Checks if the reference is a registered fluid or any well formed tag */
  public static boolean isKnownFluid(String ref) {
    return isKnown(ref, BuiltInRegistries.FLUID, BuiltInRegistries.FLUID.getDefaultKey());
  }

  private static boolean isKnown(String ref, Registry<?> registry, ResourceLocation empty) {
    if (!isValidRef(ref)) {
      return false;
    }
    ref = ref.trim().toLowerCase(Locale.ROOT);
    if (ref.startsWith("#")) {
      return true;
    }
    ResourceLocation id = ResourceLocation.tryParse(fullId(ref));
    return id != null && !id.equals(empty) && registry.containsKey(id);
  }

  /** Checks if a modifier comes from the workspace or the game, assumes it exists if modifiers are not loaded yet */
  public static boolean isKnownModifier(String id, Workspace workspace) {
    if (workspace.findEntry(id) != null) {
      return true;
    }
    ModifierId modifier = ModifierId.tryParse(id);
    return modifier != null && (!ModifierManager.INSTANCE.isDynamicModifiersLoaded() || ModifierManager.INSTANCE.contains(modifier));
  }

  /* Reading stats JSON in the UI */

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
}
