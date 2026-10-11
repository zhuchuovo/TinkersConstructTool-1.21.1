package com.tcmateditor.model;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Editor representation of one Tinkers' material */
public class MaterialDoc {
  /** Full material ID, e.g. {@code mymats:ruby} */
  public String id = "";
  public int tier = 1;
  public int sortOrder = 100;
  public boolean craftable = true;
  public boolean hidden = false;
  /** Stat type ID to the JSON fields for that stat type. Only enabled stat types are present. */
  public LinkedHashMap<String,JsonObject> stats = new LinkedHashMap<>();
  /** Traits applied to every stat type without its own list */
  public List<TraitRef> defaultTraits = new ArrayList<>();
  /** Stat type or stat group ID to traits */
  public LinkedHashMap<String,List<TraitRef>> statTraits = new LinkedHashMap<>();
  /** Modifier slots added to tools using this material, slot type to amount (negative removes slots) */
  public LinkedHashMap<String,Integer> slots = new LinkedHashMap<>();
  /** Items that can be used in the part builder */
  public List<ItemRef> items = new ArrayList<>();
  /** Optional casting/melting fluid */
  public FluidRef fluid = new FluidRef();
  public TextureDoc texture = TextureDoc.createDefault(0xFFB0B0B0);
  /** Language code to texts */
  public LinkedHashMap<String,LangText> lang = new LinkedHashMap<>();
  /** Name color as #RRGGBB, empty to follow the texture palette */
  public String textColor = "";

  public static class TraitRef {
    public String id = "";
    public int level = 1;

    public TraitRef() {}

    public TraitRef(String id, int level) {
      this.id = id;
      this.level = level;
    }

    public TraitRef copy() {
      return new TraitRef(id, level);
    }
  }

  public static class ItemRef {
    /** Item ID, or {@code #tag} */
    public String ingredient = "";
    /** Material value given by the item */
    public int value = 1;
    /** Items needed for that value */
    public int needed = 1;

    public ItemRef() {}

    public ItemRef(String ingredient, int value, int needed) {
      this.ingredient = ingredient;
      this.value = value;
      this.needed = needed;
    }

    public ItemRef copy() {
      return new ItemRef(ingredient, value, needed);
    }
  }

  public static class FluidRef {
    public boolean enabled = false;
    /** Fluid ID, or {@code #tag} */
    public String fluid = "";
    /** Fluid amount per material value (an ingot is 90 mb) */
    public int amount = 90;
    public int temperature = 800;

    public FluidRef copy() {
      FluidRef ref = new FluidRef();
      ref.enabled = enabled;
      ref.fluid = fluid;
      ref.amount = amount;
      ref.temperature = temperature;
      return ref;
    }
  }

  public String namespace() {
    int i = id.indexOf(':');
    return i < 0 ? "minecraft" : id.substring(0, i);
  }

  public String path() {
    int i = id.indexOf(':');
    return i < 0 ? id : id.substring(i + 1);
  }

  public LangText lang(String code) {
    return lang.computeIfAbsent(code, c -> new LangText());
  }

  /** Name for lists, using the given language with fallbacks */
  public String displayName(String code) {
    String name = Languages.resolve(lang, code, t -> t.name);
    return name.isBlank() ? path() : name;
  }

  /** Color for the name */
  public int nameColor() {
    if (textColor != null && textColor.startsWith("#") && textColor.length() == 7) {
      try {
        return 0xFF000000 | Integer.parseInt(textColor.substring(1), 16);
      } catch (NumberFormatException ignored) {}
    }
    return texture.mainColor() | 0xFF000000;
  }

  /** True if the material changes any modifier slots */
  public boolean hasSlots() {
    return slots.values().stream().anyMatch(v -> v != null && v != 0);
  }

  /** ID of the hidden trait that carries the slot changes */
  public String slotsModifierId() {
    return id + "_slots";
  }

  public MaterialDoc copy() {
    MaterialDoc doc = new MaterialDoc();
    doc.id = id;
    doc.tier = tier;
    doc.sortOrder = sortOrder;
    doc.craftable = craftable;
    doc.hidden = hidden;
    for (Map.Entry<String,JsonObject> entry : stats.entrySet()) {
      doc.stats.put(entry.getKey(), entry.getValue().deepCopy());
    }
    defaultTraits.forEach(t -> doc.defaultTraits.add(t.copy()));
    for (Map.Entry<String,List<TraitRef>> entry : statTraits.entrySet()) {
      doc.statTraits.put(entry.getKey(), new ArrayList<>(entry.getValue().stream().map(TraitRef::copy).toList()));
    }
    doc.slots.putAll(slots);
    items.forEach(i -> doc.items.add(i.copy()));
    doc.fluid = fluid.copy();
    doc.texture = texture.copy();
    for (Map.Entry<String,LangText> entry : lang.entrySet()) {
      doc.lang.put(entry.getKey(), entry.getValue().copy());
    }
    doc.textColor = textColor;
    return doc;
  }

  /** Repairs nulls after loading from JSON */
  public void validate() {
    if (id == null) id = "";
    if (stats == null) stats = new LinkedHashMap<>();
    stats.values().removeIf(v -> v == null);
    if (defaultTraits == null) defaultTraits = new ArrayList<>();
    if (statTraits == null) statTraits = new LinkedHashMap<>();
    statTraits.values().removeIf(v -> v == null);
    if (slots == null) slots = new LinkedHashMap<>();
    slots.values().removeIf(v -> v == null || v == 0);
    if (items == null) items = new ArrayList<>();
    if (fluid == null) fluid = new FluidRef();
    if (texture == null) texture = TextureDoc.createDefault(0xFFB0B0B0);
    texture.validate();
    if (lang == null) lang = new LinkedHashMap<>();
    lang.values().removeIf(v -> v == null);
    if (textColor == null) textColor = "";
    tier = Math.max(0, tier);
  }
}
