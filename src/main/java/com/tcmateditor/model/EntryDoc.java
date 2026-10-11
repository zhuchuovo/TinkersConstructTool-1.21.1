package com.tcmateditor.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom entry ("词条"), saved as a data driven Tinkers' modifier.
 * Each line becomes one modifier module, so an entry can raise damage and lower attack speed at the same time.
 */
public class EntryDoc {
  /** Full modifier ID */
  public String id = "";
  /** Text color as #RRGGBB */
  public String color = "#FFAA33";
  /** default, single_level, no_levels, pluses */
  public String levelDisplay = "default";
  public List<Line> lines = new ArrayList<>();
  public LinkedHashMap<String,LangText> lang = new LinkedHashMap<>();

  public static class Line {
    /** stat or attribute */
    public String kind = "stat";
    /** Tool stat ID or attribute ID */
    public String target = "tconstruct:attack_damage";
    /** Stat: add, percent, multiply_base, multiply_conditional, multiply_all. Attribute: add_value, add_multiplied_base, add_multiplied_total */
    public String op = "add";
    /** Value at every level */
    public float flat = 0f;
    /** Additional value per modifier level */
    public float perLevel = 1f;
    /** Equipment slots for attributes */
    public List<String> slots = new ArrayList<>(List.of("mainhand"));

    public Line copy() {
      Line line = new Line();
      line.kind = kind;
      line.target = target;
      line.op = op;
      line.flat = flat;
      line.perLevel = perLevel;
      line.slots = new ArrayList<>(slots);
      return line;
    }

    public boolean isAttribute() {
      return "attribute".equals(kind);
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

  public String displayName(String code) {
    String name = Languages.resolve(lang, code, t -> t.name);
    return name.isBlank() ? path() : name;
  }

  public int colorInt() {
    if (color != null && color.startsWith("#") && color.length() == 7) {
      try {
        return 0xFF000000 | Integer.parseInt(color.substring(1), 16);
      } catch (NumberFormatException ignored) {}
    }
    return 0xFFFFAA33;
  }

  public EntryDoc copy() {
    EntryDoc doc = new EntryDoc();
    doc.id = id;
    doc.color = color;
    doc.levelDisplay = levelDisplay;
    lines.forEach(l -> doc.lines.add(l.copy()));
    for (Map.Entry<String,LangText> entry : lang.entrySet()) {
      doc.lang.put(entry.getKey(), entry.getValue().copy());
    }
    return doc;
  }

  public void validate() {
    if (id == null) id = "";
    if (color == null) color = "#FFAA33";
    if (levelDisplay == null) levelDisplay = "default";
    if (lines == null) lines = new ArrayList<>();
    lines.removeIf(l -> l == null);
    for (Line line : lines) {
      if (line.slots == null) line.slots = new ArrayList<>();
      if (line.kind == null) line.kind = "stat";
      if (line.target == null) line.target = "";
      if (line.op == null) line.op = line.isAttribute() ? "add_value" : "add";
    }
    if (lang == null) lang = new LinkedHashMap<>();
    lang.values().removeIf(v -> v == null);
  }
}
