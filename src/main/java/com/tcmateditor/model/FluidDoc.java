package com.tcmateditor.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** A new fluid, registered at startup by an exported mod or KubeJS script. */
public class FluidDoc {
  public String id = "";
  public String color = "#FFFFFF";
  public String stillTexture = "minecraft:block/water_still";
  public String flowingTexture = "minecraft:block/water_flow";
  /** Fluid type temperature in kelvin, distinct from Tinkers' recipe temperature. */
  public int temperature = 300;
  public int density = 1000;
  public int viscosity = 1000;
  public int lightLevel = 0;
  public boolean bucket = true;
  public boolean block = true;
  /** Disabled for older documents which reference an existing external texture. */
  public boolean generatedTexture = false;
  public String textureStyle = "water";
  public int textureSeed = 0;
  public int textureScale = 4;
  public int opacity = 255;
  /** Initialized from the saved color when loading an older document. */
  public TextureDoc texture;
  public Map<String,LangText> lang = new LinkedHashMap<>();

  public String namespace() { return id.substring(0, id.indexOf(':')); }
  public String path() { return id.substring(id.indexOf(':') + 1); }
  public String flowingId() { return namespace() + ":flowing_" + path(); }
  public String bucketId() { return id + "_bucket"; }
  public String translationKey() { return "fluid." + namespace() + "." + path().replace('/', '.'); }

  public String displayName(String language) {
    String name = Languages.resolve(lang, language, text -> text.name);
    if (!name.isBlank()) return name;
    StringBuilder result = new StringBuilder();
    for (String word : path().split("[_./-]")) {
      if (word.isEmpty()) continue;
      if (!result.isEmpty()) result.append(' ');
      result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return result.toString();
  }

  public int colorInt() {
    return generatedTexture && texture != null ? texture.mainColor() : baseColorInt();
  }

  public int baseColorInt() {
    try { return 0xFF000000 | Integer.parseInt(color.substring(1), 16); }
    catch (RuntimeException e) { return 0xFFFFFFFF; }
  }

  public int renderTintColor() { return generatedTexture ? 0xFFFFFFFF : baseColorInt(); }
  public String stillTextureId() { return generatedTexture ? namespace() + ":block/fluid/" + path() + "_still" : stillTexture; }
  public String flowingTextureId() { return generatedTexture ? namespace() + ":block/fluid/" + path() + "_flow" : flowingTexture; }

  public static TextureDoc createTexture(int color) {
    TextureDoc texture = TextureDoc.createDefault(color);
    texture.baseStyle = "";
    texture.animation.type = "flow";
    texture.animation.frames = 16;
    texture.animation.frametime = 2;
    return texture;
  }

  public void validate() {
    if (id == null) id = "";
    id = id.trim();
    if (color == null || !color.matches("#[0-9a-fA-F]{6}")) color = "#FFFFFF";
    if (stillTexture == null || !Workspace.isValidId(stillTexture)) stillTexture = "minecraft:block/water_still";
    if (flowingTexture == null || !Workspace.isValidId(flowingTexture)) flowingTexture = "minecraft:block/water_flow";
    temperature = Math.clamp(temperature, 1, 100000);
    density = Math.clamp(density, 1, 100000);
    viscosity = Math.clamp(viscosity, 1, 100000);
    lightLevel = Math.clamp(lightLevel, 0, 15);
    if (textureStyle == null || !java.util.List.of("water", "molten", "slime", "oil", "honey", "cloud", "crystal").contains(textureStyle)) textureStyle = "water";
    textureSeed = Math.clamp(textureSeed, 0, 9999);
    textureScale = Math.clamp(textureScale, 1, 8);
    opacity = Math.clamp(opacity, 0, 255);
    if (texture == null) texture = createTexture(baseColorInt());
    texture.validate();
    if (lang == null) lang = new LinkedHashMap<>();
    lang.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
  }

  public FluidDoc copy() {
    FluidDoc copy = Workspace.GSON.fromJson(Workspace.GSON.toJson(this), FluidDoc.class);
    copy.validate();
    return copy;
  }
}
