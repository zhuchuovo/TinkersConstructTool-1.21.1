package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.Pickers;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.gui.ui.Button;
import com.tcmateditor.client.gui.ui.ColorSwatch;
import com.tcmateditor.client.gui.ui.Dropdown;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.NumberField;
import com.tcmateditor.client.gui.ui.TextField;
import com.tcmateditor.client.gui.ui.UIElement;
import com.tcmateditor.client.texture.Colors;
import com.tcmateditor.gen.EntryGenerator;
import com.tcmateditor.model.EntryDoc;
import com.tcmateditor.model.EntryDoc.Line;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.Workspace;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Effects of a custom entry: each line changes one stat, attribute or modifier slot type, or applies a mob effect */
public class EntryEffectsPage implements Page {
  private static final List<String> KINDS = List.of("stat", "attribute", "slot", "effect");
  private final EntryDoc doc;

  public EntryEffectsPage(EntryDoc doc) {
    this.doc = doc;
  }

  @Override
  public void build(Form form) {
    form.hint(Names.tr("page.entry.hint"));

    TextField id = form.row(Names.tr("label.id"), new TextField(doc.id, form.controlWidth()));
    id.validator(text -> Workspace.isValidId(text) && (text.equals(doc.id) || EditorSession.workspace().findEntry(text) == null));
    id.onChange(text -> {
      if (id.isValid() && !text.equals(doc.id)) {
        // keep material references pointing at the renamed entry
        for (MaterialDoc material : EditorSession.workspace().materials) {
          material.defaultTraits.forEach(t -> { if (t.id.equals(doc.id)) t.id = text; });
          material.statTraits.values().forEach(list -> list.forEach(t -> { if (t.id.equals(doc.id)) t.id = text; }));
        }
        for (com.tcmateditor.model.UpgradeDoc upgrade : EditorSession.workspace().upgrades) {
          if (upgrade.modifier.equals(doc.id)) upgrade.modifier = text;
        }
        doc.id = text;
        Page.changed();
      }
    });
    form.row(Names.tr("label.color"), new ColorSwatch(doc::colorInt, c -> { doc.color = Colors.hex(c); Page.changed(); }).withHex());
    form.row(Names.tr("label.level_display"), new Dropdown<>(EntryGenerator.LEVEL_DISPLAYS, d -> Names.tr("level_display." + d), () -> doc.levelDisplay, d -> {
      doc.levelDisplay = d;
      Page.changed();
    }, 110));

    form.header(Names.tr("section.effect_lines"));
    for (Line line : new ArrayList<>(doc.lines)) {
      buildLine(form, line);
    }
    Button addStat = form.place(new Button(Names.tr("button.add_line"), () -> {
      Line line = new Line();
      doc.lines.add(line);
      Page.rebuild();
    }), 0);
    Button addAttribute = form.place(new Button(Names.tr("button.add_attribute_line"), () -> {
      Line line = new Line();
      line.kind = "attribute";
      line.target = "minecraft:generic.movement_speed";
      line.op = "add_multiplied_total";
      line.perLevel = 0.05f;
      doc.lines.add(line);
      Page.rebuild();
    }).fit(), addStat.w + 4);
    Button addSlot = new Button(Names.tr("button.add_slot_line"), () -> {
      Line line = new Line();
      setKind(line, "slot");
      doc.lines.add(line);
      Page.rebuild();
    }).fit();
    addSlot.tooltip(Names.tr("hint.slot_line"));
    Button addEffect = new Button(Names.tr("button.add_effect_line"), () -> {
      Line line = new Line();
      setKind(line, "effect");
      doc.lines.add(line);
      Page.rebuild();
    }).fit();
    addEffect.tooltip(Names.tr("hint.effect_line"));
    // wrap the buttons on narrow windows
    int buttonX = addStat.w + addAttribute.w + 8;
    for (Button button : List.of(addSlot, addEffect)) {
      if (buttonX + button.w > form.width()) {
        form.next(18);
        buttonX = 0;
      }
      form.place(button, buttonX);
      buttonX += button.w + 4;
    }
    form.next(16);

    // usage
    form.header(Names.tr("section.usage"));
    List<String> users = new ArrayList<>();
    for (MaterialDoc material : EditorSession.workspace().materials) {
      boolean uses = material.defaultTraits.stream().anyMatch(t -> t.id.equals(doc.id))
        || material.statTraits.values().stream().anyMatch(list -> list.stream().anyMatch(t -> t.id.equals(doc.id)));
      if (uses) {
        users.add(material.displayName(EditorSession.uiLanguage()));
      }
    }
    form.hint(users.isEmpty() ? Names.tr("section.usage.none") : Names.tr("section.usage.list", String.join(", ", users)));
    long upgrades = EditorSession.workspace().upgrades.stream().filter(u -> u.modifier.equals(doc.id)).count();
    if (upgrades > 0) {
      form.hint(Names.tr("section.usage.upgrades", upgrades));
    }
    MaterialDoc current = EditorSession.material;
    if (current != null) {
      boolean already = current.defaultTraits.stream().anyMatch(t -> t.id.equals(doc.id));
      form.full(new Button(Names.tr("button.add_to_material", current.displayName(EditorSession.uiLanguage())), () -> {
        current.defaultTraits.add(new MaterialDoc.TraitRef(doc.id, 1));
        Page.rebuild();
      }).enabled(() -> !already).fit());
    }
    form.full(new Button(Names.tr("button.make_upgrade"), () -> com.tcmateditor.client.gui.EditorScreen.newUpgrade(doc.id)).fit())
      .tooltip(Names.tr("hint.make_upgrade"));
    form.finish();
  }

  private void buildLine(Form form, Line line) {
    int width = form.width();
    int top = form.y;
    UIElement card = form.view.add(new UIElement() {
      @Override
      public void render(GuiGraphics g, int mx, int my, float partialTick) {
        Theme.box(g, x, y, w, h, Theme.PANEL);
        g.fill(x, y, x + 2, y + h, line.isEffect() ? Names.effectColor(line.target) : doc.colorInt());
      }
    }.at(form.pad, top).size(width, 44));
    form.y += 3;
    if (line.isEffect()) {
      buildEffectLine(form, line, width);
      card.size(width, form.y - top + 3);
      form.y = top + card.h + 4;
      return;
    }
    int cardH = line.isAttribute() ? 60 : 44;
    card.size(width, cardH);
    // row 1: kind, target, operation
    form.place(new Dropdown<>(KINDS, k -> Names.tr("kind." + k), () -> line.kind, k -> {
      if (!k.equals(line.kind)) {
        setKind(line, k);
        Page.rebuild();
      }
    }, 66), 6).tooltip(Names.tr("hint.kind"));
    int targetW = Math.max(80, width - 6 - 66 - 4 - 100 - 24);
    if (line.isSlot()) {
      // slot lines pick a slot type and have no operation
      form.place(new Dropdown<>(EntryGenerator.MODIFIER_SLOT_TYPES, t -> Names.tr("slot_type." + t), () -> line.target, t -> {
        line.target = t;
        Page.changed();
      }, targetW), 76).tooltip(Names.tr("hint.slot_line"));
    } else {
      Button target = new Button(() -> line.isAttribute() ? Names.attribute(line.target) : Names.toolStat(line.target), () -> {
        if (line.isAttribute()) {
          Pickers.attribute(id -> { line.target = id; Page.rebuild(); });
        } else {
          Pickers.toolStat(id -> {
            // the tool stat picker also lists modifier slots
            if (id.startsWith(Pickers.SLOT_PREFIX)) {
              setKind(line, "slot");
              line.target = id.substring(Pickers.SLOT_PREFIX.length());
            } else {
              line.target = id;
            }
            Page.rebuild();
          });
        }
      });
      target.size(targetW, 16);
      target.tooltip(Component.literal(line.target));
      form.place(target, 76);
      List<String> ops = line.isAttribute() ? EntryGenerator.ATTRIBUTE_OPS : EntryGenerator.STAT_OPS;
      form.place(new Dropdown<>(ops, o -> Names.tr("op." + o), () -> line.op, o -> { line.op = o; Page.changed(); }, 100), 80 + targetW)
        .tooltip(() -> Names.tr("op." + line.op + ".hint"));
    }
    form.place(new Button(Component.literal("×"), () -> { doc.lines.remove(line); Page.rebuild(); }).style(Button.Style.GHOST).size(16, 16), width - 18)
      .tooltip(Names.tr("button.remove"));
    form.next(16);
    // row 2: values
    form.place(new Label(Names.tr("label.per_level"), Theme.TEXT_FAINT), 6).at(form.pad + 6, form.y + 4);
    int px = 8 + Theme.font().width(Names.tr("label.per_level"));
    boolean whole = line.isSlot();
    form.place(new NumberField(line.perLevel, whole, whole ? -100 : -10000, whole ? 100 : 10000, whole ? 1 : 0.05, 50).onValue(v -> { line.perLevel = (float) v; Page.changed(); }), px)
      .tooltip(Names.tr("hint.per_level"));
    int fx = px + 56;
    form.place(new Label(Names.tr("label.flat"), Theme.TEXT_FAINT), fx).at(form.pad + fx, form.y + 4);
    int fx2 = fx + 2 + Theme.font().width(Names.tr("label.flat"));
    form.place(new NumberField(line.flat, whole, whole ? -100 : -10000, whole ? 100 : 10000, whole ? 1 : 0.05, 50).onValue(v -> { line.flat = (float) v; Page.changed(); }), fx2)
      .tooltip(Names.tr("hint.flat"));
    form.place(new Label(() -> summary(line), Theme.TEXT_DIM), fx2 + 56).at(form.pad + fx2 + 56, form.y + 4).size(Math.max(10, width - fx2 - 62), 9);
    form.next(16);
    // row 3: slots for attributes
    if (line.isAttribute()) {
      form.place(new Label(Names.tr("label.slots"), Theme.TEXT_FAINT), 6).at(form.pad + 6, form.y + 3);
      int sx = 10 + Theme.font().width(Names.tr("label.slots"));
      for (String slot : EntryGenerator.SLOTS) {
        Button chip = new Button(Names.tr("slot." + slot), () -> {
          if (line.slots.contains(slot)) {
            line.slots.remove(slot);
          } else {
            line.slots.add(slot);
          }
          Page.changed();
        }).style(Button.Style.CHIP).selected(() -> line.slots.contains(slot));
        chip.h = 14;
        form.place(chip, sx);
        sx += chip.w + 3;
      }
      form.next(14);
    }
    form.y = top + cardH + 4;
  }

  /** Mob effect line: effect, trigger, then level, duration and chance that can grow with level */
  private void buildEffectLine(Form form, Line line, int width) {
    // row 1: kind, effect, trigger
    form.place(new Dropdown<>(KINDS, k -> Names.tr("kind." + k), () -> line.kind, k -> {
      if (!k.equals(line.kind)) {
        setKind(line, k);
        Page.rebuild();
      }
    }, 66), 6).tooltip(Names.tr("hint.kind"));
    int triggerW = 120;
    int effectW = Math.max(80, width - 6 - 66 - 4 - triggerW - 24);
    Button effect = new Button(() -> Names.effect(line.target), () -> Pickers.mobEffect(id -> {
      line.target = id;
      Page.rebuild();
    }));
    effect.size(effectW, 16);
    effect.tooltip(Component.literal(line.target));
    form.place(effect, 76);
    form.place(new Dropdown<>(EntryGenerator.EFFECT_TRIGGERS, t -> Names.tr("trigger." + t), () -> line.op, t -> { line.op = t; Page.changed(); }, triggerW), 80 + effectW)
      .tooltip(() -> Names.tr("trigger." + line.op + ".hint"));
    form.place(new Button(Component.literal("×"), () -> { doc.lines.remove(line); Page.rebuild(); }).style(Button.Style.GHOST).size(16, 16), width - 18)
      .tooltip(Names.tr("button.remove"));
    form.next(18);
    // rows 2+: values, each as "label [base] per level [x]"
    int[] x = { 6 };
    Component levelLabel = Names.tr("label.effect_level");
    Component secondsLabel = Names.tr("label.seconds");
    Component chanceLabel = Names.tr("label.chance");
    // same label width so the fields line up when the groups wrap
    int labelW = Math.max(Theme.font().width(levelLabel), Math.max(Theme.font().width(secondsLabel), Theme.font().width(chanceLabel)));
    valueGroup(form, x, width, levelLabel, labelW, true, 0, 255, 1, line.flat, line.perLevel, v -> line.flat = v, v -> line.perLevel = v);
    valueGroup(form, x, width, secondsLabel, labelW, false, 0, 3600, 1, line.seconds, line.secondsPerLevel, v -> line.seconds = v, v -> line.secondsPerLevel = v);
    valueGroup(form, x, width, chanceLabel, labelW, false, 0, 100, 5, line.chance, line.chancePerLevel, v -> line.chance = v, v -> line.chancePerLevel = v);
    form.next(16);
    form.place(new Label(() -> effectSummary(line), Theme.TEXT_DIM), 6).at(form.pad + 6, form.y + 1).size(Math.max(10, width - 12), 9);
    form.next(11);
  }

  /** "label base [x] per level [y]" that wraps to a new row when it does not fit */
  private static void valueGroup(Form form, int[] x, int width, Component label, int labelW, boolean whole, double min, double max, double step,
                                 float base, float perLevel, java.util.function.Consumer<Float> setBase, java.util.function.Consumer<Float> setPerLevel) {
    Component baseLabel = Names.tr("label.flat");
    Component perLabel = Names.tr("label.per_level");
    int groupW = labelW + 6 + Theme.font().width(baseLabel) + 2 + 44 + 4 + Theme.font().width(perLabel) + 2 + 44;
    if (x[0] > 6 && x[0] + groupW > width - 4) {
      form.next(18);
      x[0] = 6;
    }
    int cx = x[0];
    form.place(new Label(label, Theme.TEXT), cx).at(form.pad + cx, form.y + 4);
    cx += labelW + 6;
    form.place(new Label(baseLabel, Theme.TEXT_FAINT), cx).at(form.pad + cx, form.y + 4);
    cx += Theme.font().width(baseLabel) + 2;
    form.place(new NumberField(base, whole, min, max, step, 44).onValue(v -> { setBase.accept((float) v); Page.changed(); }), cx).tooltip(Names.tr("hint.flat"));
    cx += 48;
    form.place(new Label(perLabel, Theme.TEXT_FAINT), cx).at(form.pad + cx, form.y + 4);
    cx += Theme.font().width(perLabel) + 2;
    form.place(new NumberField(perLevel, whole, -max, max, step, 44).onValue(v -> { setPerLevel.accept((float) v); Page.changed(); }), cx).tooltip(Names.tr("hint.per_level"));
    x[0] = cx + 44 + 14;
  }

  /** e.g. "Lv1 Poison I · 5s · 100%   Lv3 Poison III · 5s · 100%" */
  private static Component effectSummary(Line line) {
    MutableComponent text = Component.empty();
    for (int level : new int[] { 1, 3 }) {
      int effectLevel = Math.round(line.flat + line.perLevel * level);
      float seconds = Math.max(0, line.seconds + line.secondsPerLevel * level);
      float chance = Math.max(0, Math.min(100, line.chance + line.chancePerLevel * level));
      if (level > 1) {
        text.append("    ");
      }
      text.append("Lv" + level + " ");
      if (effectLevel < 1 || seconds <= 0 || chance <= 0) {
        text.append(Names.tr("summary.effect_none"));
      } else {
        text.append(Names.effect(line.target)).append(" " + roman(effectLevel) + " · ")
          .append(Names.tr("summary.effect", trim(seconds), trim(chance) + "%"));
      }
    }
    return text;
  }

  private static String trim(float value) {
    return value == Math.round(value) ? String.valueOf(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
  }

  private static String roman(int level) {
    String[] numerals = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
    return level <= numerals.length ? numerals[level - 1] : String.valueOf(level);
  }

  /** Switches a line to another kind with sensible starting values */
  private static void setKind(Line line, String kind) {
    line.kind = kind;
    switch (kind) {
      case "attribute" -> { line.target = "minecraft:generic.attack_speed"; line.op = "add_multiplied_total"; }
      case "effect" -> {
        line.target = "minecraft:poison";
        line.op = "weapon";
        line.flat = 1;
        line.perLevel = 0;
        line.seconds = 5;
        line.secondsPerLevel = 0;
        line.chance = 100;
        line.chancePerLevel = 0;
      }
      case "slot" -> {
        line.target = "upgrades";
        line.op = "add";
        line.flat = 0;
        line.perLevel = 1;
      }
      default -> { line.target = "tconstruct:attack_damage"; line.op = "add"; }
    }
  }

  /** Values at level 1 and 3 */
  private static Component summary(Line line) {
    MutableComponent text = Component.literal("Lv1 " + format(line, line.flat + line.perLevel));
    text.append("  Lv3 " + format(line, line.flat + line.perLevel * 3));
    return text;
  }

  private static String format(Line line, float value) {
    if (line.isSlot()) {
      return String.format(Locale.ROOT, "%+d", Math.round(value));
    }
    return switch (line.op) {
      case "percent", "add_multiplied_base", "add_multiplied_total" -> String.format(Locale.ROOT, "%+.0f%%", value * 100);
      case "multiply_base", "multiply_conditional", "multiply_all" -> String.format(Locale.ROOT, "×%.2f", 1 + value);
      default -> String.format(Locale.ROOT, "%+.2f", value);
    };
  }
}
