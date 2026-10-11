package com.tcmateditor.client.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.tcmateditor.EditorPaths;
import com.tcmateditor.TCMatEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads the part sprite list ({@code tinkering/generator_part_textures.json}) and greyscale sprites from the loaded resource packs.
 * This is the same information Tinkers' own part texture generator uses, so addon parts are supported automatically.
 */
public final class PartTextureLibrary {
  private static final String GENERATOR_FILE = "tinkering/generator_part_textures.json";
  /** Built in patterns offered by the texture editor */
  public static final List<String> BUILT_IN_PATTERNS = List.of(
    "tconstruct:generator/fiery", "tconstruct:generator/knightslime", "tconstruct:generator/queens_slime", "tconstruct:generator/cactus", "tconstruct:generator/ardite_rust",
    "minecraft:block/stone", "minecraft:block/cobblestone", "minecraft:block/obsidian", "minecraft:block/crying_obsidian", "minecraft:block/netherrack", "minecraft:block/magma",
    "minecraft:block/prismarine", "minecraft:block/amethyst_block", "minecraft:block/oak_planks", "minecraft:block/dark_oak_log", "minecraft:block/end_stone",
    "minecraft:block/sculk", "minecraft:block/blackstone", "minecraft:block/gilded_blackstone", "minecraft:block/ancient_debris_side", "minecraft:block/glowstone",
    "minecraft:block/sea_lantern", "minecraft:block/blue_ice", "minecraft:block/mud", "minecraft:block/tuff");

  public record PartInfo(ResourceLocation path, Set<String> statTypes, boolean allowAnimated) {}

  private static List<PartInfo> parts;
  private static final Map<ResourceLocation,Optional<Sprite>> SPRITES = new HashMap<>();
  private static final Map<String,Optional<Sprite>> PATTERNS = new HashMap<>();

  private PartTextureLibrary() {}

  /** Clears caches, called on resource reload */
  public static synchronized void clear() {
    parts = null;
    SPRITES.clear();
    PATTERNS.clear();
  }

  private static ResourceManager manager() {
    return Minecraft.getInstance().getResourceManager();
  }

  /** All part sprites that support material textures */
  public static synchronized List<PartInfo> parts() {
    if (parts == null) {
      parts = loadParts(manager());
    }
    return parts;
  }

  private static List<PartInfo> loadParts(ResourceManager manager) {
    Map<ResourceLocation,PartInfo> map = new LinkedHashMap<>();
    for (String namespace : manager.getNamespaces()) {
      ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, GENERATOR_FILE);
      List<Resource> stack = manager.getResourceStack(location);
      // top pack first, stop at "replace" like Tinkers' does
      for (int i = stack.size() - 1; i >= 0; i--) {
        try (BufferedReader reader = stack.get(i).openAsReader()) {
          JsonObject json = GsonHelper.parse(reader);
          for (JsonElement element : GsonHelper.getAsJsonArray(json, "parts", new JsonArray())) {
            JsonObject part = element.getAsJsonObject();
            ResourceLocation path = ResourceLocation.tryParse(GsonHelper.getAsString(part, "path"));
            if (path == null) {
              continue;
            }
            Set<String> stats = new LinkedHashSet<>();
            JsonElement statElement = part.get("stat_type");
            if (statElement != null && statElement.isJsonArray()) {
              statElement.getAsJsonArray().forEach(e -> stats.add(fullId(e.getAsString())));
            } else if (statElement != null) {
              stats.add(fullId(statElement.getAsString()));
            }
            boolean animated = GsonHelper.getAsBoolean(part, "allow_animated", true);
            map.merge(path, new PartInfo(path, stats, animated), (a, b) -> {
              Set<String> merged = new LinkedHashSet<>(a.statTypes);
              merged.addAll(b.statTypes);
              return new PartInfo(path, merged, a.allowAnimated && b.allowAnimated);
            });
          }
          if (GsonHelper.getAsBoolean(json, "replace", false)) {
            break;
          }
        } catch (IOException | RuntimeException e) {
          TCMatEditor.LOG.error("Failed to read {} from {}", location, stack.get(i).sourcePackId(), e);
        }
      }
    }
    return List.copyOf(map.values());
  }

  private static String fullId(String id) {
    return id.contains(":") ? id : "tconstruct:" + id;
  }

  /** Finds part info by path */
  @Nullable
  public static PartInfo find(ResourceLocation path) {
    for (PartInfo info : parts()) {
      if (info.path.equals(path)) {
        return info;
      }
    }
    return null;
  }

  /** Reads a texture from {@code textures/<path>.png}, cached */
  @Nullable
  public static synchronized Sprite sprite(ResourceLocation path) {
    return SPRITES.computeIfAbsent(path, p -> Optional.ofNullable(read(manager(), p.withPath("textures/" + p.getPath() + ".png")))).orElse(null);
  }

  /** Base sprite for a part, trying the style variant first like Tinkers' fallbacks */
  @Nullable
  public static Sprite base(ResourceLocation path, String style) {
    if (style != null && !style.isEmpty()) {
      Sprite styled = sprite(path.withSuffix("_" + style));
      if (styled != null) {
        return styled;
      }
    }
    return sprite(path);
  }

  @Nullable
  private static Sprite read(ResourceManager manager, ResourceLocation file) {
    Optional<Resource> resource = manager.getResource(file);
    if (resource.isEmpty()) {
      return null;
    }
    try (InputStream stream = resource.get().open()) {
      return fromStream(stream);
    } catch (IOException | RuntimeException e) {
      TCMatEditor.LOG.warn("Failed to read texture {}", file, e);
      return null;
    }
  }

  private static Sprite fromStream(InputStream stream) throws IOException {
    try (NativeImage image = NativeImage.read(stream)) {
      int w = image.getWidth(), h = image.getHeight();
      int[] pixels = new int[w * h];
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          pixels[y * w + x] = Colors.fromAbgr(image.getPixelRGBA(x, y));
        }
      }
      return new Sprite(w, h, pixels);
    }
  }

  /* Patterns */

  /** Pattern sprite by key: a texture location, or {@code file:name.png} from the patterns folder */
  @Nullable
  public static synchronized Sprite pattern(String key) {
    if (key == null || key.isBlank()) {
      return null;
    }
    return PATTERNS.computeIfAbsent(key, k -> {
      if (k.startsWith("file:")) {
        Path file = EditorPaths.patterns().resolve(k.substring(5)).normalize();
        if (!file.startsWith(EditorPaths.patterns().normalize()) || !Files.isRegularFile(file)) {
          return Optional.empty();
        }
        try (InputStream stream = Files.newInputStream(file)) {
          return Optional.of(fromStream(stream).firstFrame());
        } catch (IOException | RuntimeException e) {
          TCMatEditor.LOG.warn("Failed to read pattern {}", file, e);
          return Optional.empty();
        }
      }
      ResourceLocation location = ResourceLocation.tryParse(k);
      if (location == null) {
        return Optional.empty();
      }
      Sprite sprite = read(manager(), location.withPath("textures/" + location.getPath() + ".png"));
      return Optional.ofNullable(sprite == null ? null : sprite.firstFrame());
    }).orElse(null);
  }

  /** Lists pattern keys: user files first, then built in */
  public static List<String> listPatterns() {
    List<String> list = new ArrayList<>();
    Path folder = EditorPaths.patterns();
    try {
      Files.createDirectories(folder);
      try (Stream<Path> files = Files.list(folder)) {
        files.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
          .sorted()
          .forEach(p -> list.add("file:" + p.getFileName()));
      }
    } catch (IOException e) {
      TCMatEditor.LOG.warn("Failed to list patterns", e);
    }
    list.addAll(BUILT_IN_PATTERNS);
    return list;
  }
}
