package com.tcmateditor.client.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.tcmateditor.TCMatEditor;
import com.tcmateditor.client.texture.Colors;
import com.tcmateditor.client.texture.PartTextureLibrary;
import com.tcmateditor.client.texture.PartTextureLibrary.PartInfo;
import com.tcmateditor.client.texture.Sprite;
import com.tcmateditor.client.texture.TextureEngine;
import com.tcmateditor.client.texture.FluidTextureEngine;
import com.tcmateditor.gen.EntryGenerator;
import com.tcmateditor.gen.GenUtil;
import com.tcmateditor.model.EntryDoc;
import com.tcmateditor.model.FluidDoc;
import com.tcmateditor.model.LangText;
import com.tcmateditor.model.Languages;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.Workspace;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Generates resource pack files: material render info, part textures, language files and name colors.
 * All output uses Tinkers' standard formats, so it works without this mod.
 */
public final class AssetGenerator {
  private AssetGenerator() {}

  /** Generates every asset file keyed by pack relative path */
  public static Map<String,byte[]> generate(Workspace workspace) {
    return generate(workspace, false);
  }

  public static Map<String,byte[]> generateForExport(Workspace workspace) {
    return generate(workspace, true);
  }

  private static Map<String,byte[]> generate(Workspace workspace, boolean export) {
    Map<String,byte[]> files = new TreeMap<>();
    // namespace -> language -> key -> text
    Map<String,Map<String,Map<String,String>>> lang = new TreeMap<>();
    // namespace -> colors.json
    Map<String,JsonObject> colors = new TreeMap<>();

    for (MaterialDoc doc : workspace.materials) {
      if (!isValidMaterialId(doc.id)) {
        continue;
      }
      String ns = doc.namespace();
      String path = doc.path();
      files.put("assets/" + ns + "/tinkering/materials/" + path + ".json", json(renderInfo(doc)));
      generateTextures(doc, files);

      String key = "material." + ns + "." + path;
      addLang(lang, ns, key, doc.lang, t -> t.name, prettyName(path));
      addLang(lang, ns, key + ".flavor", doc.lang, t -> t.flavor, null);
      addLang(lang, ns, key + ".encyclopedia", doc.lang, t -> t.desc, null);
      colors.computeIfAbsent(ns, n -> new JsonObject());
      JsonObject materialColors = colors.get(ns).has("material." + ns) ? colors.get(ns).getAsJsonObject("material." + ns) : new JsonObject();
      materialColors.addProperty(path, Colors.hex(doc.nameColor()));
      colors.get(ns).add("material." + ns, materialColors);

      // hidden slot trait, shown in the tinker station
      if (doc.hasSlots()) {
        String slotsKey = "modifier." + ns + "." + path + "_slots";
        for (String code : Languages.CODES) {
          String[] text = SLOT_TEXT.get(code);
          String name = Languages.resolve(doc.lang, code, t -> t.name);
          Map<String,String> texts = lang.computeIfAbsent(ns, n -> new TreeMap<>()).computeIfAbsent(code, c -> new LinkedHashMap<>());
          texts.put(slotsKey, text[0].formatted(name.isBlank() ? prettyName(path) : name));
          texts.put(slotsKey + ".description", slotsDescription(doc, text));
        }
        JsonObject modifierColors = colors.get(ns).has("modifier." + ns) ? colors.get(ns).getAsJsonObject("modifier." + ns) : new JsonObject();
        modifierColors.addProperty(path + "_slots", Colors.hex(doc.nameColor()));
        colors.get(ns).add("modifier." + ns, modifierColors);
      }
    }

    for (EntryDoc doc : workspace.entries) {
      if (!Workspace.isValidId(doc.id)) {
        continue;
      }
      String ns = doc.namespace();
      String path = doc.path();
      String key = "modifier." + ns + "." + path;
      addLang(lang, ns, key, doc.lang, t -> t.name, prettyName(path));
      addLang(lang, ns, key + ".flavor", doc.lang, t -> t.flavor, null);
      addLang(lang, ns, key + ".description", doc.lang, t -> t.desc, null);
      colors.computeIfAbsent(ns, n -> new JsonObject());
      JsonObject modifierColors = colors.get(ns).has("modifier." + ns) ? colors.get(ns).getAsJsonObject("modifier." + ns) : new JsonObject();
      modifierColors.addProperty(path, Colors.hex(doc.colorInt()));
      colors.get(ns).add("modifier." + ns, modifierColors);
    }

    for (FluidDoc doc : workspace.fluids) {
      if (!Workspace.isValidId(doc.id)) continue;
      String ns = doc.namespace();
      String path = doc.path();
      addLang(lang, ns, doc.translationKey(), doc.lang, text -> text.name, doc.displayName(Languages.EN));
      addLang(lang, ns, "block." + ns + "." + path.replace('/', '.'), doc.lang, text -> text.name, doc.displayName(Languages.EN));
      for (String code : Languages.CODES) {
        String name = doc.displayName(code);
        String bucketName = switch (code) {
          case "zh_cn", "zh_tw" -> name + "桶";
          case "ja_jp" -> name + "入りバケツ";
          default -> name + " Bucket";
        };
        lang.computeIfAbsent(ns, n -> new TreeMap<>()).computeIfAbsent(code, c -> new LinkedHashMap<>())
          .put("item." + ns + "." + path.replace('/', '.') + "_bucket", bucketName);
      }
      if (doc.generatedTexture) generateFluidTextures(doc, files);
      // Live resource reload cannot load models for fluids that have not been registered yet.
      if (!export && !GenUtil.isKnownFluid(doc.id)) continue;
      generateFluidModels(doc, files);
    }

    for (Map.Entry<String,Map<String,Map<String,String>>> nsEntry : lang.entrySet()) {
      for (Map.Entry<String,Map<String,String>> langEntry : nsEntry.getValue().entrySet()) {
        JsonObject json = new JsonObject();
        langEntry.getValue().forEach(json::addProperty);
        files.put("assets/" + nsEntry.getKey() + "/lang/" + langEntry.getKey() + ".json", json(json));
      }
    }
    for (Map.Entry<String,JsonObject> entry : colors.entrySet()) {
      files.put("assets/" + entry.getKey() + "/mantle/colors.json", json(entry.getValue()));
    }
    return files;
  }

  /** Per language: name pattern, description prefix, upgrades, abilities, defense, souls, separator (same words as Tinkers') */
  private static final Map<String,String[]> SLOT_TEXT = Map.of(
    "zh_cn", new String[] { "%s槽位", "使用此材料的工具：", "升级槽", "能力槽", "防御槽", "灵魂槽", "，" },
    "zh_tw", new String[] { "%s槽位", "使用此材料的工具：", "升級槽", "能力槽", "防禦槽", "靈魂槽", "，" },
    "en_us", new String[] { "%s Slots", "Tools using this material: ", "Upgrades", "Abilities", "Defense Slots", "Soul Slots", ", " },
    "ja_jp", new String[] { "%sのスロット", "この素材を使うツール：", "アップグレード", "アビリティ", "防御スロット", "ソウルスロット", "、" });

  /** e.g. "Tools using this material: Upgrades +1, Abilities -1" */
  private static String slotsDescription(MaterialDoc doc, String[] text) {
    List<String> parts = new ArrayList<>();
    for (int i = 0; i < EntryGenerator.MODIFIER_SLOT_TYPES.size(); i++) {
      Integer amount = doc.slots.get(EntryGenerator.MODIFIER_SLOT_TYPES.get(i));
      if (amount != null && amount != 0) {
        parts.add(text[2 + i] + " " + String.format(Locale.ROOT, "%+d", amount));
      }
    }
    return text[1] + String.join(text[6], parts);
  }

  /** Material IDs may not contain '/' as Tinkers' treats the last folder as a variant */
  public static boolean isValidMaterialId(String id) {
    return Workspace.isValidId(id) && !id.substring(id.indexOf(':') + 1).contains("/");
  }

  private static byte[] json(JsonObject json) {
    return Workspace.GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
  }

  /** Converts a path into a readable fallback name */
  public static String prettyName(String path) {
    StringBuilder builder = new StringBuilder();
    for (String word : path.split("[_./-]")) {
      if (!word.isEmpty()) {
        if (!builder.isEmpty()) builder.append(' ');
        builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
    }
    return builder.toString();
  }

  private static void addLang(Map<String,Map<String,Map<String,String>>> lang, String ns, String key, Map<String,LangText> texts, Function<LangText,String> getter, String fallback) {
    for (String code : Languages.CODES) {
      String value = Languages.resolve(texts, code, getter);
      if (value.isBlank()) {
        value = fallback;
      }
      if (value != null && !value.isBlank()) {
        lang.computeIfAbsent(ns, n -> new TreeMap<>()).computeIfAbsent(code, c -> new LinkedHashMap<>()).put(key, value);
      }
    }
  }

  /* Render info */

  private static JsonObject renderInfo(MaterialDoc doc) {
    JsonObject json = new JsonObject();
    json.addProperty("color", String.format(Locale.ROOT, "%08X", doc.texture.mainColor() | 0xFF000000));
    JsonArray fallbacks = new JsonArray();
    if (!doc.texture.baseStyle.isBlank()) {
      fallbacks.add(doc.texture.baseStyle);
    }
    json.add("fallbacks", fallbacks);
    if (doc.texture.luminosity > 0) {
      json.addProperty("luminosity", doc.texture.luminosity);
    }
    return json;
  }

  /* Textures */

  /** Stat types textures are generated for, including Tinkers' pseudo stat types for armor models */
  public static Set<String> textureStats(MaterialDoc doc) {
    Set<String> stats = new HashSet<>(doc.stats.keySet());
    stats.add("tconstruct:repair_kit");
    for (String stat : doc.stats.keySet()) {
      switch (stat) {
        case "tconstruct:plating_helmet", "tconstruct:plating_chestplate", "tconstruct:plating_leggings", "tconstruct:plating_boots" -> stats.add("tconstruct:armor_plating");
        case "tconstruct:maille" -> stats.add("tconstruct:armor_maille");
        case "tconstruct:cuirass" -> stats.add("tconstruct:armor_cuirass");
        default -> {}
      }
    }
    return stats;
  }

  /** Sprite suffix for a material, matches MaterialRenderInfo#getSuffix */
  public static String suffix(String materialId) {
    int i = materialId.indexOf(':');
    String ns = materialId.substring(0, i);
    String path = materialId.substring(i + 1);
    return "minecraft".equals(ns) ? path : ns + "_" + path;
  }

  private static void generateTextures(MaterialDoc doc, Map<String,byte[]> files) {
    Set<String> stats = textureStats(doc);
    String suffix = suffix(doc.id);
    Function<String,Sprite> patterns = PartTextureLibrary::pattern;
    for (PartInfo part : PartTextureLibrary.parts()) {
      boolean supported = part.statTypes().stream().anyMatch(stats::contains);
      if (!supported) {
        continue;
      }
      Sprite base = PartTextureLibrary.base(part.path(), doc.texture.baseStyle);
      if (base == null) {
        continue;
      }
      boolean animated = part.allowAnimated() && doc.texture.animation.isActive();
      Sprite result;
      try {
        result = TextureEngine.render(doc.texture, base, animated, patterns);
      } catch (RuntimeException e) {
        TCMatEditor.LOG.error("Failed to render {} for {}", part.path(), doc.id, e);
        continue;
      }
      ResourceLocation out = part.path().withSuffix("_" + suffix);
      String file = "assets/" + out.getNamespace() + "/textures/" + out.getPath() + ".png";
      try {
        files.put(file, png(result));
      } catch (IOException e) {
        TCMatEditor.LOG.error("Failed to encode {}", file, e);
        continue;
      }
      if (animated) {
        JsonObject animation = new JsonObject();
        animation.addProperty("frametime", doc.texture.animation.frametime);
        if (doc.texture.animation.interpolate) {
          animation.addProperty("interpolate", true);
        }
        JsonObject meta = new JsonObject();
        meta.add("animation", animation);
        files.put(file + ".mcmeta", json(meta));
      }
    }
  }

  public static byte[] png(Sprite sprite) throws IOException {
    try (NativeImage image = new NativeImage(sprite.width(), sprite.height(), false)) {
      for (int y = 0; y < sprite.height(); y++) {
        for (int x = 0; x < sprite.width(); x++) {
          image.setPixelRGBA(x, y, Colors.toAbgr(sprite.get(x, y)));
        }
      }
      return image.asByteArray();
    }
  }

  private static void generateFluidTextures(FluidDoc doc, Map<String,byte[]> files) {
    for (boolean flowing : new boolean[] {false, true}) {
      ResourceLocation id = ResourceLocation.parse(flowing ? doc.flowingTextureId() : doc.stillTextureId());
      String path = "assets/" + id.getNamespace() + "/textures/" + id.getPath() + ".png";
      Sprite sprite = FluidTextureEngine.render(doc, flowing, PartTextureLibrary::pattern);
      try {
        files.put(path, png(sprite));
      } catch (IOException e) {
        throw new IllegalStateException("Cannot encode fluid texture " + id, e);
      }
      if (sprite.isAnimated()) {
        JsonObject animation = new JsonObject();
        animation.addProperty("width", sprite.width());
        animation.addProperty("height", sprite.width());
        animation.addProperty("frametime", doc.texture.animation.frametime);
        animation.addProperty("interpolate", doc.texture.animation.interpolate);
        JsonObject meta = new JsonObject();
        meta.add("animation", animation);
        files.put(path + ".mcmeta", json(meta));
      }
      // KubeJS 7 renders these IDs, not the inputs passed to stillTexture/flowingTexture.
      // Supply them directly so resource packs and the live pack work with its startup script.
      String kubePath = kubeFluidTexturePath(doc, flowing);
      files.put(kubePath, files.get(path));
      if (files.containsKey(path + ".mcmeta")) {
        files.put(kubePath + ".mcmeta", files.get(path + ".mcmeta"));
      }
    }
  }

  public static String kubeFluidTexturePath(FluidDoc doc, boolean flowing) {
    return "assets/" + doc.namespace() + "/textures/block/generated/" + doc.path() + (flowing ? "_flow" : "_still") + ".png";
  }

  /** Models skipped before registration must be available on the first reload after a restart. */
  public static Map<String,byte[]> registeredFluidModels(Workspace workspace) {
    Map<String,byte[]> files = new TreeMap<>();
    for (FluidDoc doc : workspace.fluids) {
      if (Workspace.isValidId(doc.id) && GenUtil.isKnownFluid(doc.id)) {
        generateFluidModels(doc, files);
      }
    }
    return files;
  }

  private static void generateFluidModels(FluidDoc doc, Map<String,byte[]> files) {
    String ns = doc.namespace();
    String path = doc.path();
    if (doc.bucket) {
      JsonObject model = new JsonObject();
      model.addProperty("parent", "neoforge:item/bucket");
      model.addProperty("loader", "tconstruct:fluid_container");
      model.addProperty("fluid", doc.id);
      files.put("assets/" + ns + "/models/item/" + path + "_bucket.json", json(model));
    }
    if (doc.block) {
      JsonObject blockModel = new JsonObject();
      JsonObject textures = new JsonObject();
      textures.addProperty("particle", doc.stillTextureId());
      blockModel.add("textures", textures);
      files.put("assets/" + ns + "/models/block/" + path + ".json", json(blockModel));
      JsonObject state = new JsonObject();
      JsonObject variants = new JsonObject();
      JsonObject variant = new JsonObject();
      variant.addProperty("model", ns + ":block/" + path);
      variants.add("", variant);
      state.add("variants", variants);
      files.put("assets/" + ns + "/blockstates/" + path + ".json", json(state));
    }
  }

  /* Signature */

  /** Text describing everything assets depend on, used to skip resource reloads when nothing visual changed */
  public static String signature(Workspace workspace) {
    JsonObject json = new JsonObject();
    json.addProperty("format", 2);
    for (MaterialDoc doc : workspace.materials) {
      JsonObject material = new JsonObject();
      material.add("texture", Workspace.GSON.toJsonTree(doc.texture));
      material.add("lang", Workspace.GSON.toJsonTree(doc.lang));
      material.addProperty("color", doc.textColor);
      material.add("slots", Workspace.GSON.toJsonTree(doc.slots));
      material.add("stats", Workspace.GSON.toJsonTree(textureStats(doc).stream().sorted().toList()));
      json.add("m:" + doc.id, material);
    }
    for (EntryDoc doc : workspace.entries) {
      JsonObject entry = new JsonObject();
      entry.add("lang", Workspace.GSON.toJsonTree(doc.lang));
      entry.addProperty("color", doc.color);
      json.add("e:" + doc.id, entry);
    }
    for (FluidDoc doc : workspace.fluids) {
      json.add("f:" + doc.id, Workspace.GSON.toJsonTree(doc));
      json.addProperty("registered:" + doc.id, GenUtil.isKnownFluid(doc.id));
    }
    json.addProperty("parts", PartTextureLibrary.parts().size());
    return json.toString();
  }
}
