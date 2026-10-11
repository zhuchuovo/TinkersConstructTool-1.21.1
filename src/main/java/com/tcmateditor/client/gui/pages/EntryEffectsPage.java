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
import com.tcmateditor.gen.DataGenerator;
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

/** Effects of a custom entry: each line raises or lowers one stat or attribute */
public class EntryEffectsPage implements Page {
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
    form.row(Names.tr("label.level_display"), new Dropdown<>(DataGenerator.LEVEL_DISPLAYS, d -> Names.tr("level_display." + d), () -> doc.levelDisplay, d -> {
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
    form.place(new Button(Names.tr("button.add_attribute_line"), () -> {
      Line line = new Line();
      line.kind = "attribute";
      line.target = "minecraft:generic.movement_speed";
      line.op = "add_multiplied_total";
      line.perLevel = 0.05f;
      doc.lines.add(line);
      Page.rebuild();
    }).fit(), addStat.w + 4);
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
    int cardH = line.isAttribute() ? 60 : 44;
    form.view.add(new UIElement() {
      @Override
      public void render(GuiGraphics g, int mx, int my, float partialTick) {
        Theme.box(g, x, y, w, h, Theme.PANEL);
        g.fill(x, y, x + 2, y + h, doc.colorInt());
      }
    }.at(form.pad, top).size(width, cardH));
    form.y += 3;
    // row 1: kind, target, operation
    form.place(new Dropdown<>(List.of("stat", "attribute"), k -> Names.tr("kind." + k), () -> line.kind, k -> {
      if (!k.equals(line.kind)) {
        line.kind = k;
        line.target = line.isAttribute() ? "minecraft:generic.attack_speed" : "tconstruct:attack_damage";
        line.op = line.isAttribute() ? "add_multiplied_total" : "add";
        Page.rebuild();
      }
    }, 66), 6).tooltip(Names.tr("hint.kind"));
    int targetW = Math.max(80, width - 6 - 66 - 4 - 100 - 24);
    Button target = new Button(() -> line.isAttribute() ? Names.attribute(line.target) : Names.toolStat(line.target), () -> {
      if (line.isAttribute()) {
        Pickers.attribute(id -> { line.target = id; Page.rebuild(); });
      } else {
        Pickers.toolStat(id -> { line.target = id; Page.rebuild(); });
      }
    });
    target.size(targetW, 16);
    target.tooltip(Component.literal(line.target));
    form.place(target, 76);
    List<String> ops = line.isAttribute() ? DataGenerator.ATTRIBUTE_OPS : DataGenerator.STAT_OPS;
    form.place(new Dropdown<>(ops, o -> Names.tr("op." + o), () -> line.op, o -> { line.op = o; Page.changed(); }, 100), 80 + targetW)
      .tooltip(() -> Names.tr("op." + line.op + ".hint"));
    form.place(new Button(Component.literal("×"), () -> { doc.lines.remove(line); Page.rebuild(); }).style(Button.Style.GHOST).size(16, 16), width - 18)
      .tooltip(Names.tr("button.remove"));
    form.next(16);
    // row 2: values
    form.place(new Label(Names.tr("label.per_level"), Theme.TEXT_FAINT), 6).at(form.pad + 6, form.y + 4);
    int px = 8 + Theme.font().width(Names.tr("label.per_level"));
    form.place(new NumberField(line.perLevel, false, -10000, 10000, 0.05, 50).onValue(v -> { line.perLevel = (float) v; Page.changed(); }), px)
      .tooltip(Names.tr("hint.per_level"));
    int fx = px + 56;
    form.place(new Label(Names.tr("label.flat"), Theme.TEXT_FAINT), fx).at(form.pad + fx, form.y + 4);
    int fx2 = fx + 2 + Theme.font().width(Names.tr("label.flat"));
    form.place(new NumberField(line.flat, false, -10000, 10000, 0.05, 50).onValue(v -> { line.flat = (float) v; Page.changed(); }), fx2)
      .tooltip(Names.tr("hint.flat"));
    form.place(new Label(() -> summary(line), Theme.TEXT_DIM), fx2 + 56).at(form.pad + fx2 + 56, form.y + 4).size(Math.max(10, width - fx2 - 62), 9);
    form.next(16);
    // row 3: slots for attributes
    if (line.isAttribute()) {
      form.place(new Label(Names.tr("label.slots"), Theme.TEXT_FAINT), 6).at(form.pad + 6, form.y + 3);
      int sx = 10 + Theme.font().width(Names.tr("label.slots"));
      for (String slot : DataGenerator.SLOTS) {
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

  /** Values at level 1 and 3 */
  private static Component summary(Line line) {
    MutableComponent text = Component.literal("Lv1 " + format(line, line.flat + line.perLevel));
    text.append("  Lv3 " + format(line, line.flat + line.perLevel * 3));
    return text;
  }

  private static String format(Line line, float value) {
    return switch (line.op) {
      case "percent", "add_multiplied_base", "add_multiplied_total" -> String.format(Locale.ROOT, "%+.0f%%", value * 100);
      case "multiply_base", "multiply_conditional", "multiply_all" -> String.format(Locale.ROOT, "×%.2f", 1 + value);
      default -> String.format(Locale.ROOT, "%+.2f", value);
    };
  }
}
