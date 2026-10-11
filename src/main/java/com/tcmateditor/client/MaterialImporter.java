package com.tcmateditor.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.tcmateditor.TCMatEditor;
import com.tcmateditor.client.texture.Colors;
import com.tcmateditor.model.LangText;
import com.tcmateditor.model.Languages;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.MaterialDoc.ItemRef;
import com.tcmateditor.model.MaterialDoc.TraitRef;
import com.tcmateditor.model.TextureDoc;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Creates editor documents from materials that are already loaded in game, used as templates */
public final class MaterialImporter {
  private MaterialImporter() {}

  /** Trait groups Tinkers' falls back to before the default traits */
  public static final List<String> TRAIT_GROUPS = List.of(
    MaterialRegistry.MELEE_HARVEST.toString(), MaterialRegistry.RANGED.toString(), MaterialRegistry.ARMOR.toString(), MaterialRegistry.AMMO.toString());

  /**
   * Imports a loaded material
   * @param source  Material to copy
   * @param newId   ID for the new document
   */
  public static MaterialDoc importMaterial(IMaterial source, String newId) {
    IMaterialRegistry registry = MaterialRegistry.getInstance();
    MaterialId id = source.getIdentifier();
    MaterialDoc doc = new MaterialDoc();
    doc.id = newId;
    doc.tier = source.getTier();
    doc.sortOrder = source.getSortOrder();
    doc.craftable = source.isCraftable();
    doc.hidden = source.isHidden();

    // stats
    for (IMaterialStats stats : registry.getAllStats(id)) {
      try {
        doc.stats.put(stats.getIdentifier().toString(), serializeStats(stats));
      } catch (RuntimeException e) {
        TCMatEditor.LOG.warn("Unable to copy stats {} of {}", stats.getIdentifier(), id, e);
      }
    }

    // traits
    for (ModifierEntry entry : registry.getDefaultTraits(id)) {
      doc.defaultTraits.add(new TraitRef(entry.getId().toString(), entry.getLevel()));
    }
    Set<String> statIds = new LinkedHashSet<>(TRAIT_GROUPS);
    for (ResourceLocation statId : registry.getAllStatTypeIds()) {
      statIds.add(statId.toString());
    }
    for (String statId : statIds) {
      MaterialStatsId stat = MaterialStatsId.PARSER.tryParse(statId);
      if (stat != null && registry.hasUniqueTraits(id, stat)) {
        List<TraitRef> list = new ArrayList<>();
        for (ModifierEntry entry : registry.getTraits(id, stat)) {
          list.add(new TraitRef(entry.getId().toString(), entry.getLevel()));
        }
        doc.statTraits.put(statId, list);
      }
    }

    collapseTraits(doc);

    // items from the synced recipe list
    try {
      if (Minecraft.getInstance().level != null) {
        for (RecipeHolder<MaterialRecipe> holder : Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(TinkerRecipeTypes.MATERIAL.get())) {
          MaterialRecipe recipe = holder.value();
          if (recipe.getMaterial().getId().equals(id) && !recipe.getMaterial().getVariant().hasVariant()) {
            for (String ref : ingredientRefs(recipe.getIngredient())) {
              doc.items.add(new ItemRef(ref, recipe.getValue(), recipe.getNeeded()));
            }
          }
        }
      }
    } catch (RuntimeException e) {
      TCMatEditor.LOG.warn("Unable to copy recipes of {}", id, e);
    }

    // texture
    MaterialVariantId variant = MaterialVariantId.create(id, "");
    MaterialRenderInfo info = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(variant).orElse(null);
    int color = info != null ? info.vertexColor() | 0xFF000000 : 0xFFB0B0B0;
    if (color == 0xFFFFFFFF) {
      color = 0xFFC8C8C8;
    }
    TextureDoc texture = TextureDoc.createDefault(color);
    if (info != null) {
      texture.baseStyle = info.fallbacks().length > 0 ? info.fallbacks()[0] : "";
      texture.luminosity = info.luminosity();
    }
    List<TextureDoc.Stop> palette = readPalette(id);
    if (palette != null && palette.size() >= 2) {
      texture.palette = palette;
    }
    texture.validate();
    doc.texture = texture;

    // texts
    String base = "material." + id.getNamespace() + "." + id.getPath();
    for (String code : Languages.CODES) {
      Map<String,String> lang = readLang(id.getNamespace(), code);
      LangText text = new LangText(lang.get(base), lang.get(base + ".flavor"), lang.get(base + ".encyclopedia"));
      if (!text.isEmpty()) {
        doc.lang.put(code, text);
      }
    }
    doc.validate();
    return doc;
  }

  /** Stat type to the trait group Tinkers' falls back to */
  private static final Map<String,String> STAT_GROUP = new HashMap<>();
  static {
    for (String stat : List.of("head", "handle", "binding")) STAT_GROUP.put("tconstruct:" + stat, "tconstruct:melee_harvest");
    for (String stat : List.of("limb", "grip", "bowstring")) STAT_GROUP.put("tconstruct:" + stat, "tconstruct:ranged");
    for (String stat : List.of("plating_helmet", "plating_chestplate", "plating_leggings", "plating_boots", "plating_shield", "cuirass", "maille", "shield_core", "slime", "skull", "shell", "laces")) {
      STAT_GROUP.put("tconstruct:" + stat, "tconstruct:armor");
    }
    for (String stat : List.of("arrow_head", "arrow_shaft", "fletching")) STAT_GROUP.put("tconstruct:" + stat, "tconstruct:ammo");
  }

  /** Tinkers' copies group traits into every stat type when loading, remove lists that only repeat their fallback */
  private static void collapseTraits(MaterialDoc doc) {
    for (String stat : new ArrayList<>(doc.statTraits.keySet())) {
      if (TRAIT_GROUPS.contains(stat)) {
        continue;
      }
      String group = STAT_GROUP.get(stat);
      List<TraitRef> fallback = group != null && doc.statTraits.containsKey(group) ? doc.statTraits.get(group) : doc.defaultTraits;
      if (sameTraits(doc.statTraits.get(stat), fallback)) {
        doc.statTraits.remove(stat);
      }
    }
    for (String group : TRAIT_GROUPS) {
      if (doc.statTraits.containsKey(group) && sameTraits(doc.statTraits.get(group), doc.defaultTraits)) {
        doc.statTraits.remove(group);
      }
    }
  }

  private static boolean sameTraits(List<TraitRef> a, List<TraitRef> b) {
    if (a.size() != b.size()) {
      return false;
    }
    for (int i = 0; i < a.size(); i++) {
      if (!a.get(i).id.equals(b.get(i).id) || a.get(i).level != b.get(i).level) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  private static <T extends IMaterialStats> JsonObject serializeStats(T stats) {
    MaterialStatType<T> type = (MaterialStatType<T>) stats.getType();
    JsonObject json = new JsonObject();
    type.getLoadable().serialize(stats, json);
    return json;
  }

  /** Converts an ingredient to item IDs or #tags */
  public static List<String> ingredientRefs(Ingredient ingredient) {
    List<String> refs = new ArrayList<>();
    try {
      JsonElement json = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow();
      collectRefs(json, refs);
    } catch (RuntimeException ignored) {}
    if (refs.isEmpty()) {
      ItemStack[] items = ingredient.getItems();
      if (items.length > 0) {
        refs.add(BuiltInRegistries.ITEM.getKey(items[0].getItem()).toString());
      }
    }
    return refs;
  }

  private static void collectRefs(JsonElement json, List<String> refs) {
    if (json.isJsonArray()) {
      json.getAsJsonArray().forEach(e -> collectRefs(e, refs));
    } else if (json.isJsonObject()) {
      JsonObject object = json.getAsJsonObject();
      if (object.has("tag")) {
        refs.add("#" + object.get("tag").getAsString());
      } else if (object.has("item")) {
        refs.add(object.get("item").getAsString());
      }
    }
  }

  /** Reads the generator palette from a material render info JSON */
  @Nullable
  private static List<TextureDoc.Stop> readPalette(MaterialId id) {
    ResourceManager manager = Minecraft.getInstance().getResourceManager();
    ResourceLocation location = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), MaterialRenderInfoLoader.FOLDER + "/" + id.getPath() + ".json");
    for (int depth = 0; depth < 4; depth++) {
      JsonObject json = readJson(manager, location);
      if (json == null) {
        return null;
      }
      JsonArray palette = findPalette(json.get("generator"));
      if (palette != null) {
        List<TextureDoc.Stop> stops = new ArrayList<>();
        for (JsonElement element : palette) {
          if (!element.isJsonObject()) continue;
          JsonObject stop = element.getAsJsonObject();
          if (!stop.has("color") || !stop.has("grey")) continue;
          Integer color = parseColor(GsonHelper.getAsString(stop, "color"));
          if (color != null) {
            stops.add(new TextureDoc.Stop(GsonHelper.getAsInt(stop, "grey"), color));
          }
        }
        return stops;
      }
      // follow parent redirects
      if (json.has("parent")) {
        ResourceLocation parent = ResourceLocation.tryParse(GsonHelper.getAsString(json, "parent"));
        if (parent == null) return null;
        location = parent.withPath(MaterialRenderInfoLoader.FOLDER + "/" + parent.getPath() + ".json");
      } else {
        return null;
      }
    }
    return null;
  }

  /** Searches recursively for the first "palette" array */
  @Nullable
  private static JsonArray findPalette(@Nullable JsonElement element) {
    if (element == null || !element.isJsonObject()) {
      return null;
    }
    JsonObject object = element.getAsJsonObject();
    if (object.has("palette") && object.get("palette").isJsonArray()) {
      return object.getAsJsonArray("palette");
    }
    for (Map.Entry<String,JsonElement> entry : object.entrySet()) {
      JsonArray found = findPalette(entry.getValue());
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  /** Parses Tinkers' AARRGGBB or RRGGBB colors */
  @Nullable
  private static Integer parseColor(String text) {
    String s = text.startsWith("#") ? text.substring(1) : text;
    try {
      if (s.length() == 8) {
        return (int) Long.parseLong(s, 16) | 0xFF000000;
      }
      if (s.length() == 6) {
        return Integer.parseInt(s, 16) | 0xFF000000;
      }
    } catch (NumberFormatException ignored) {}
    return null;
  }

  @Nullable
  private static JsonObject readJson(ResourceManager manager, ResourceLocation location) {
    return manager.getResource(location).map(resource -> {
      try (BufferedReader reader = resource.openAsReader()) {
        return GsonHelper.parse(reader);
      } catch (Exception e) {
        return null;
      }
    }).orElse(null);
  }

  /** Reads a merged language file of a namespace */
  public static Map<String,String> readLang(String namespace, String code) {
    Map<String,String> map = new HashMap<>();
    ResourceManager manager = Minecraft.getInstance().getResourceManager();
    List<Resource> stack = manager.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace, "lang/" + code + ".json"));
    for (Resource resource : stack) {
      try (BufferedReader reader = resource.openAsReader()) {
        JsonObject json = GsonHelper.parse(reader);
        for (Map.Entry<String,JsonElement> entry : json.entrySet()) {
          if (entry.getValue().isJsonPrimitive()) {
            map.put(entry.getKey(), entry.getValue().getAsString());
          }
        }
      } catch (Exception e) {
        TCMatEditor.LOG.debug("Failed to read language {} {}", namespace, code, e);
      }
    }
    return map;
  }

  /** Helper for showing a color preview of loaded materials */
  public static int materialColor(MaterialId id) {
    return MaterialRenderInfoLoader.INSTANCE.getRenderInfo(MaterialVariantId.create(id, "")).map(i -> i.vertexColor() | 0xFF000000).orElse(Colors.argb(255, 176, 176, 176));
  }
}
