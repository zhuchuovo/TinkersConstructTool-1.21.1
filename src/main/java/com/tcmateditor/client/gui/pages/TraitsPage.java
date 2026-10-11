package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.MaterialImporter;
import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.Pickers;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.gui.ui.Button;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.NumberField;
import com.tcmateditor.client.gui.ui.Toggle;
import com.tcmateditor.client.gui.ui.UIElement;
import com.tcmateditor.gen.EntryGenerator;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.MaterialDoc.TraitRef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Traits: modifiers the material gives to tools, plus modifier slots. Custom entries can be used here. */
public class TraitsPage implements Page {
  private final MaterialDoc doc;

  public TraitsPage(MaterialDoc doc) {
    this.doc = doc;
  }

  @Override
  public void build(Form form) {
    form.hint(Names.tr("page.traits.hint"));

    // modifier slots, generated as a hidden trait of this material
    form.header(Names.tr("section.material_slots"));
    form.hint(Names.tr("section.material_slots.hint"));
    int x = 0;
    for (String type : EntryGenerator.MODIFIER_SLOT_TYPES) {
      Component label = Names.tr("slot_type." + type);
      int groupW = Theme.font().width(label) + 4 + 44;
      if (x > 0 && x + groupW > form.width()) {
        form.next(16);
        x = 0;
      }
      form.place(new Label(label, Theme.TEXT_DIM), x).at(form.pad + x, form.y + 4);
      int fieldX = x + Theme.font().width(label) + 4;
      form.place(new NumberField(doc.slots.getOrDefault(type, 0), true, -10, 10, 1, 40).onValue(v -> {
        if ((int) v == 0) {
          doc.slots.remove(type);
        } else {
          doc.slots.put(type, (int) v);
        }
        Page.changed();
      }), fieldX).tooltip(Names.tr("hint.material_slots"));
      x += groupW + 12;
    }
    form.next(16);

    form.header(Names.tr("section.default_traits"));
    traitList(form, doc.defaultTraits);

    // the four groups Tinkers' falls back to
    for (String group : MaterialImporter.TRAIT_GROUPS) {
      form.header(Names.traitGroup(group));
      boolean custom = doc.statTraits.containsKey(group);
      form.full(new Toggle(Names.tr("label.own_traits"), () -> doc.statTraits.containsKey(group), on -> {
        if (on) {
          doc.statTraits.put(group, new ArrayList<>());
        } else {
          doc.statTraits.remove(group);
        }
        Page.rebuild();
      }).size(form.width(), 12)).tooltip(Names.tr("hint.own_traits"));
      if (custom) {
        traitList(form, doc.statTraits.get(group));
      }
    }

    // overrides for single stat types
    form.header(Names.tr("section.stat_traits"));
    form.hint(Names.tr("section.stat_traits.hint"));
    for (String statId : new ArrayList<>(doc.statTraits.keySet())) {
      if (MaterialImporter.TRAIT_GROUPS.contains(statId)) {
        continue;
      }
      form.place(new Label(Names.statType(statId), Theme.TEXT), 0).size(form.width() - 20, 9).tooltip(Component.literal(statId));
      form.place(new Button(Component.literal("×"), () -> { doc.statTraits.remove(statId); Page.rebuild(); }).style(Button.Style.GHOST).size(16, 14), form.width() - 16)
        .tooltip(Names.tr("button.remove"));
      form.next(12);
      traitList(form, doc.statTraits.get(statId));
    }
    form.full(new Button(Names.tr("button.add_stat_override"), () -> Pickers.statType(id -> {
      if (!doc.statTraits.containsKey(id)) {
        doc.statTraits.put(id, new ArrayList<>());
        Page.rebuild();
      }
    })).fit());
    form.finish();
  }

  private void traitList(Form form, List<TraitRef> traits) {
    int width = form.width();
    for (TraitRef trait : new ArrayList<>(traits)) {
      int color = Names.modifierColor(trait.id);
      form.place(new UIElement() {
        @Override
        public void render(GuiGraphics g, int mx, int my, float partialTick) {
          Theme.box(g, x, y, w, h, isHovered(mx, my) ? Theme.CARD_HOVER : Theme.CARD);
          g.fill(x + 4, y + 5, x + 10, y + 11, color);
          Component name = Names.modifier(trait.id);
          Theme.ellipsis(g, name, x + 14, y + 4, w - 18, color);
        }
      }.size(width - 80, 16).tooltip(Component.literal(trait.id)), 0);
      form.place(new Label(Names.tr("label.level"), Theme.TEXT_FAINT), width - 76).at(form.pad + width - 76, form.y + 4);
      form.place(new NumberField(trait.level, true, 1, 100, 1, 34).onValue(v -> { trait.level = (int) v; Page.changed(); }), width - 54);
      form.place(new Button(Component.literal("×"), () -> { traits.remove(trait); Page.rebuild(); }).style(Button.Style.GHOST).size(16, 16), width - 16)
        .tooltip(Names.tr("button.remove"));
      form.next(16);
    }
    form.full(new Button(Names.tr("button.add_trait"), () -> Pickers.modifier(id -> {
      traits.add(new TraitRef(id, 1));
      Page.rebuild();
    })).fit());
  }
}
